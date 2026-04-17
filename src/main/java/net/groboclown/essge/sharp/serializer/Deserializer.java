/**
 * Eternity Keeper, a Pillars of Eternity save game editor.
 * Copyright (C) 2015 the authors.
 * <p>
 * Eternity Keeper is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * Eternity Keeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.groboclown.essge.sharp.serializer;

import com.google.common.primitives.Shorts;
import com.google.common.primitives.UnsignedInteger;
import net.groboclown.essge.sharp.errors.*;
import net.groboclown.essge.sharp.serializer.properties.*;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import static java.util.AbstractMap.Entry;
import static java.util.AbstractMap.SimpleImmutableEntry;
import static net.groboclown.essge.sharp.serializer.SharpSerializer.*;

public class Deserializer {
    private final SharpSerializer parent;
    private final DataInput stream;
    private final List<String> names = new ArrayList<>();
    private final List<TypePair> types = new ArrayList<>();

    private final Map<Integer, ReferenceTargetProperty> propertyCache =
            new HashMap<>();

    public Deserializer(DataInput stream, SharpSerializer parent) throws IOException, SharpException {
        this.stream = stream;
        this.parent = parent;

        readHeader(names, HeaderValue::validValue);
        readHeader(types, this::convertToType);
    }

    private HeaderValue<TypePair> convertToType(final String s) {
        if (s == null) {
            return new HeaderValue<>(null, null);
        }

        String key = s.replaceAll("`\\d", "");

        if (key.contains(",")) {
            key = key.split(",")[0];
        }

        if (key.contains("[[")) {
            key = key.split("\\[\\[")[0];
        }

        Class<?> value = typeMap.get(key);
        if (value == null) {
            // return new HeaderValue<>(null, new UnhandledPropertyException(key + " (" + s + ")"));
            return HeaderValue.validValue(new TypePair(TypePair.DebugInspect.class, key));
        }

        return HeaderValue.validValue(new TypePair(value, s));
    }

    record HeaderValue<T>(T value, UnhandledPropertyException err) {
        static <T> HeaderValue<T> validValue(T t) {
                return new HeaderValue<>(t, null);
            }
        }

    private <T> void readHeader(
            List<T> items
            , Function<String, HeaderValue<T>> readCallback) throws IOException, UnhandledPropertyException {

        int count = readNumber();

        for (int i = 0; i < count; i++) {
            String itemAsText = readString();
            HeaderValue<T> item = readCallback.apply(itemAsText);
            if (item.err == null) {
                items.add(item.value);
            } else {
                throw item.err;
            }
        }
    }

    private String readString() throws IOException {
        if (!stream.readBoolean()) {
            return null;
        }

        return readCSharpString();
    }

    private String readCSharpString() throws IOException {
        int length = read7BitEncodedInt();
        byte[] buffer = new byte[length];
        stream.readFully(buffer);

        return new String(buffer, StandardCharsets.UTF_8);
    }

    private int read7BitEncodedInt()
            throws IOException, NumberFormatException {

        int count = 0;
        int shift = 0;
        byte b;

        do {
            if (shift == 5 * 7) {
                throw new NumberFormatException("Bad 7 bit encoded int.");
            }

            b = stream.readByte();
            count |= (b & 0x7f) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);

        return count;
    }

    private int readNumber() throws IOException {
        byte size = stream.readByte();

        return switch (size) {
            case 0 -> 0;
            case 1 -> stream.readByte() & 0xff;
            case 2 -> stream.readShort();
            default -> stream.readInt();
        };
    }

    public Property deserialize() throws IOException, SharpException {
        byte elementID = stream.readByte();
        return deserialize(elementID, null);
    }

    private Property deserialize(byte elementID, TypePair expectedType)
            throws IOException, SharpException {

        String propertyName = readName();
        return deserialize(elementID, propertyName, expectedType);
    }

    private Property deserialize(
            byte elementID
            , String propertyName
            , TypePair expectedType) throws IOException, SharpException {

        TypePair propertyType = readType();
        if (propertyType == null) {
            propertyType = new TypePair(expectedType.type, null);
        }

        int referenceID = 0;
        if (elementID == Elements.Reference
                || Elements.isElementWithID(elementID)) {

            referenceID = readNumber();
            if (elementID == Elements.Reference) {
                return createProperty(referenceID, propertyName, propertyType);
            }
        }

        Property property =
                createProperty(elementID, propertyName, propertyType);

        switch (property) {
            case NullProperty nullProperty -> {
                return nullProperty;
            }
            case SimpleProperty simpleProperty -> {
                parseSimpleProperty(simpleProperty);
                return property;
            }
            case ReferenceTargetProperty referenceTargetProperty -> {
                if (referenceID > 0) {
                    referenceTargetProperty.reference =
                            new Reference(referenceID);

                    referenceTargetProperty.reference.isProcessed =
                            true;

                    propertyCache.put(
                            referenceID
                            , referenceTargetProperty);
                }
            }
            default -> {
                // fall through
            }
        }

        // MultiDimensionalArray property.

        switch (property) {
            case SingleDimensionalArrayProperty singleDimensionalArrayProperty -> {
                parseSingleDimensionalArrayProperty(
                        singleDimensionalArrayProperty);
                return property;
            }
            case DictionaryProperty dictionaryProperty -> {
                parseDictionaryProperty(dictionaryProperty);
                return property;
            }
            case CollectionProperty collectionProperty -> {
                parseCollectionProperty(collectionProperty);
                return property;
            }
            case ComplexProperty complexProperty -> {
                parseComplexProperty(complexProperty);
                return property;
            }
            default -> {
            }
        }

        return property;
    }

    private void parseCollectionProperty(CollectionProperty property)
            throws IOException, SharpException {

        property.elementType = readType();
        readProperties(property.properties, property.type.type);
        readItems(property.items, property.elementType);
    }

    private void parseDictionaryProperty(DictionaryProperty property)
            throws IOException, SharpException {

        property.keyType = readType();
        property.valueType = readType();
        readProperties(property.properties, property.type.type);
        readDictionaryItems(
                property.items
                , property.keyType
                , property.valueType);
    }

    private void readDictionaryItems(
            List<Entry<Property, Property>> items
            , TypePair keyType
            , TypePair valueType)
            throws IOException, SharpException {

        int count = readNumber();
        for (int i = 0; i < count; i++) {
            readDictionaryItem(items, keyType, valueType);
        }
    }

    private void readDictionaryItem(
            List<Entry<Property, Property>> items
            , TypePair keyType
            , TypePair valueType)
            throws IOException, SharpException {

        // Key
        byte elementID = stream.readByte();
        Property keyProperty = deserialize(elementID, keyType);

        // Value
        elementID = stream.readByte();
        Property valueProperty = deserialize(elementID, valueType);

        Entry<Property, Property> item =
                new SimpleImmutableEntry<>(keyProperty, valueProperty);
        items.add(item);
    }

    private void parseSingleDimensionalArrayProperty(
            SingleDimensionalArrayProperty property)
            throws IOException, SharpException {

        property.elementType = readType();
        property.lowerBound = readNumber();
        readItems(property.items, property.elementType);
    }

    private void readItems(List<Property> items, TypePair elementType) throws IOException, SharpException {
        int count = readNumber();
        for (int i = 0; i < count; i++) {
            byte elementID = stream.readByte();
            Property subProperty = deserialize(elementID, elementType);

            items.add(subProperty);
        }
    }

    private void parseComplexProperty(ComplexProperty property)
            throws IOException, SharpException {

        readProperties(property.properties, property.type.type);
    }

    private void readProperties(List<Property> properties, Class<?> ownerType)
            throws IOException, SharpException {

        int count = readNumber();
        for (int i = 0; i < count; i++) {
            byte elementID = stream.readByte();
            String propertyName = readName();
            Field subPropertyInfo;

            try {
                subPropertyInfo = ownerType.getField(propertyName);
            } catch (NoSuchFieldException e) {
                throw new NoSuchPropertyFieldException(ownerType, propertyName);
            }

            Class<?> propertyType = subPropertyInfo.getType();

            Property subProperty = deserialize(
                    elementID
                    , propertyName
                    , new TypePair(propertyType, null));

            properties.add(subProperty);
        }
    }

    private void parseSimpleProperty(SimpleProperty property)
            throws IOException, InvalidPropertyException {

        property.value = readValue(property.type.type);
    }

    private Object readValue(Class<?> expectedType) throws IOException, InvalidPropertyException {
        if (!stream.readBoolean()) {
            return null;
        }

        return readValueCore(expectedType);
    }

    private Object readValueCore(Class<?> expectedType) throws IOException, InvalidPropertyException {
        try {
            switch (expectedType.getSimpleName()) {
                case "int" -> {
                    return stream.readInt();
                }
                case "UnsignedInteger" -> {
                    return UnsignedInteger.valueOf((long) stream.readInt());
                }
                case "String" -> {
                    return readCSharpString();
                }
                case "boolean" -> {
                    return stream.readBoolean();
                }
                case "Byte[]" -> {
                    int length = readNumber();
                    if (length < 1) {
                        return null;
                    }

                    byte[] buffer = new byte[length];
                    stream.readFully(buffer);

                    return boxBytes(buffer);
                }
                case "UUID" -> {
                    ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
                    stream.readFully(buffer.array());

                    int a = Integer.reverseBytes(buffer.getInt(0));
                    int b = Shorts.fromBytes(
                            buffer.array()[5]
                            , buffer.array()[4])
                            & 0xffff;

                    int c = Shorts.fromBytes(
                            buffer.array()[7]
                            , buffer.array()[6])
                            & 0xffff;

                    int d = buffer.array()[8] & 0xff;
                    int e = buffer.array()[9] & 0xff;
                    int f = buffer.array()[10] & 0xff;
                    int g = buffer.array()[11] & 0xff;
                    int h = buffer.array()[12] & 0xff;
                    int i = buffer.array()[13] & 0xff;
                    int j = buffer.array()[14] & 0xff;
                    int k = buffer.array()[15] & 0xff;

                    long mostSig = ((long) a << 32) + ((long) b << 16) + c;
                    long leastSig = ((long) d << 56) + ((long) e << 48)
                            + ((long) f << 40) + ((long) g << 32) + ((long) h << 24)
                            + ((long) i << 16) + ((long) j << 8) + k;

                    return new UUID(mostSig, leastSig);
                }
                case "float" -> {
                    return stream.readFloat();
                }
            }

            if (expectedType.isEnum()) {
                return readEnumeration(expectedType);
            }
        } catch (Exception e) {
            throw new IOException("Unable to read property value: %s%n", e);
        }

        String typeName = readCSharpString();
        Class<?> javaType = typeMap.get(typeName);

        if (javaType == null) {
            throw new InvalidPropertyException("No mapping found for type '" + typeName +
                    "', expected type was '" + expectedType.getSimpleName() + "'.");
        }

        return javaType;

    }

    private Byte[] boxBytes(byte[] in) {
        Byte[] out = new Byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i];
        }

        return out;
    }

    private Object readEnumeration(Class<?> type) throws IOException, InvalidPropertyException {
        int value = stream.readInt();
        try {
            Object[] constants = type.getEnumConstants();
            try {
                for (Object constant : constants) {
                    Field n = constant.getClass().getField("n");
                    int enumVal = n.getInt(constant);
                    if (enumVal == value) {
                        return constant;
                    }
                }

                return constants[value];
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return constants[value];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidPropertyException("Tried to get enum value index " + value + " that "
                            + "didn't exist for enum " + type.getSimpleName());
        }
    }

    private Property createProperty(byte elementID, String propertyName, TypePair propertyType) throws UnhandledPropertyException {
        return switch (elementID) {
            case Elements.SimpleObject -> new SimpleProperty(propertyName, propertyType);
            case Elements.ComplexObject, Elements.ComplexObjectWithID ->
                    new ComplexProperty(propertyName, propertyType);
            case Elements.SingleArray, Elements.SingleArrayWithID ->
                    new SingleDimensionalArrayProperty(propertyName, propertyType);
            case Elements.Dictionary, Elements.DictionaryWithID -> new DictionaryProperty(propertyName, propertyType);
            case Elements.Collection -> new CollectionProperty(propertyName, propertyType);
            case Elements.Null -> new NullProperty(propertyName);
            default -> throw new UnhandledPropertyException("element ID " + elementID);
        };
    }

    private Property createProperty(
            int referenceID
            , String propertyName
            , TypePair propertyType) throws SharpException {

        ReferenceTargetProperty cachedProperty = propertyCache.get(referenceID);

        if (cachedProperty == null) {
            throw new UnknownReferenceException(referenceID);
        }

        ReferenceTargetProperty property =
                (ReferenceTargetProperty)
                        Property.createInstance(
                                cachedProperty.getPropertyArt()
                                , propertyName
                                , propertyType);

        cachedProperty.reference.count++;

        //noinspection ConstantConditions
        property.makeFlatCopyFrom(cachedProperty);
        property.reference = new Reference(referenceID);

        return property;
    }

    private TypePair readType() throws IOException {
        int index = readNumber();
        return types.get(index);
    }

    private String readName() throws IOException {
        int index = readNumber();
        return names.get(index);
    }
}
