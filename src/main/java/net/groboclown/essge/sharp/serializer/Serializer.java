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

import net.groboclown.essge.rw.EternityTypeMap;
import net.groboclown.essge.sharp.errors.SharpException;
import net.groboclown.essge.sharp.serializer.properties.*;
import net.groboclown.essge.sharp.serializer.write.*;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import static net.groboclown.essge.sharp.serializer.SharpSerializer.Elements;
import static net.groboclown.essge.sharp.serializer.properties.MultiDimensionalArrayProperty.ArrayDimension;
import static net.groboclown.essge.sharp.serializer.properties.MultiDimensionalArrayProperty.MultiDimensionalArrayItem;

public class Serializer {
    private static final String rootName = "Root";
    private final List<WriteCommand> commandCache = new ArrayList<>();
    private final IndexGenerator<String> types = new IndexGenerator<>();
    private final IndexGenerator<String> names = new IndexGenerator<>();

    private final BinaryWriter stream;
    private SerializerFormat format = SerializerFormat.PRESERVE;

    public Serializer(DataOutput stream) {
        this.stream = new BinaryWriter(stream);
    }

    public Serializer toFormat(SerializerFormat format) {
        this.format = format;

        return this;
    }

    public void serialize(Property property) throws IOException, SharpException {
        if (property == null) {
            throw new IllegalArgumentException(
                    "Tried to serialize null object.");
        }

        if (!property.name.equals(rootName)) {
            throw new IllegalArgumentException(
                    "Attempted to serialize non-root property!");
        }

        serializeCore(property);
        writeNamesHeader();
        writeTypesHeader();
        writeCache();
    }

    private void writeCache() throws IOException, SharpException {
        for (WriteCommand command : commandCache) {
            command.write(stream);
        }
    }

    private void writeTypesHeader() throws IOException {
        stream.writeNumber(types.items.size());
        for (String type : types.items) {
            String typeName = convertToTypeName(type);
            stream.writeStringGuarded(typeName);
        }
    }

    private String convertToTypeName(String type) {
        if (type == null) return null;

        switch (format) {
            case UNITY_2017:
                type = EternityTypeMap.getBackwardsCompatibleType(type);
                break;

            case PRESERVE:
            default:
                break;
        }

        return type;
    }

    private void writeNamesHeader() throws IOException {
        stream.writeNumber(names.items.size());
        for (String name : names.items) {
            stream.writeStringGuarded(name);
        }
    }

    private void serializeCore(Property property) {
        switch (property) {
            case null -> throw new IllegalArgumentException(
                    "Cannot serialize null property!");
            case NullProperty nullProperty -> {
                serializeNullProperty(nullProperty);
                return;
            }
            case SimpleProperty simpleProperty -> {
                serializeSimpleProperty(simpleProperty);
                return;
            }
            default -> {
            }
        }

        if (serializeReference(((ReferenceTargetProperty) property))) {
            return;
        }

        serializeReferenceTarget((ReferenceTargetProperty) property);
    }

    private void serializeSimpleProperty(Property property) {
        writePropertyHeader(Elements.SimpleObject, property.name, property);
        writeValue(property.obj);
    }

    private void writeValue(Object value) {
        commandCache.add(new ValueWriteCommand(value));
    }

    private void writePropertyHeader(
            byte elementID
            , String name
            , Property property) {

        writeElementID(elementID);
        writeName(name);
        writeType(property != null ? property.type : null);
    }

    private void writeType(TypePair type) {
        int index = types.getIndexOfItem(type != null ? type.cSharpType : null);
        commandCache.add(new NumberWriteCommand(index));
    }

    private void writeName(String name) {
        int index = names.getIndexOfItem(name);
        commandCache.add(new NumberWriteCommand(index));
    }

    private void writeElementID(byte elementID) {
        commandCache.add(new ByteWriteCommand(elementID));
    }

    private void serializeNullProperty(Property property) {
        writePropertyHeader(Elements.Null, property.name, property);
    }

    private void serializeReferenceTarget(ReferenceTargetProperty property) {
        if (property.reference != null) {
            property.reference.isProcessed = true;
        }

        switch (property) {
            case MultiDimensionalArrayProperty multiDimensionalArrayProperty -> {
                serializeMultiDimensionalArrayProperty(multiDimensionalArrayProperty);
                return;
            }
            case SingleDimensionalArrayProperty singleDimensionalArrayProperty -> {
                serializeSingleDimensionalArrayProperty(singleDimensionalArrayProperty);
                return;
            }
            case DictionaryProperty dictionaryProperty -> {
                serializeDictionaryProperty(dictionaryProperty);
                return;
            }
            case CollectionProperty collectionProperty -> {
                serializeCollectionProperty(collectionProperty);
                return;
            }
            default -> {
            }
        }

        serializeComplexProperty(property);
    }

    private void serializeMultiDimensionalArrayProperty(MultiDimensionalArrayProperty property) {

        if (!writePropertyHeaderWithReferenceID(
                Elements.MultiArrayWithID
                , property.reference
                , property.name
                , property)) {

            writePropertyHeader(Elements.MultiArray, property.name, property);
        }

        writeType(property.elementType);
        writeDimensions(property.dimensions);
        writeMultiDimensionalArrayItems(property.items);
    }

    private void writeMultiDimensionalArrayItems(
            List<MultiDimensionalArrayItem> items) {

        writeNumber(items.size());
        for (MultiDimensionalArrayItem item : items) {
            writeMultiDimensionalArrayItem(item);
        }
    }

    private void writeMultiDimensionalArrayItem(
            MultiDimensionalArrayItem item) {

        writeNumbers(item.indexes);
        serializeCore(item.value);
    }

    private void writeNumbers(int[] n) {
        commandCache.add(new NumbersWriteCommand(n));
    }

    private void writeDimensions(List<ArrayDimension> dimensions) {
        writeNumber(dimensions.size());
        for (ArrayDimension dim : dimensions) {
            writeDimension(dim);
        }
    }

    private void writeDimension(ArrayDimension dim) {
        writeNumber(dim.length);
        writeNumber(dim.lowerBound);
    }

    private void serializeComplexProperty(Property property) {
        if (!writePropertyHeaderWithReferenceID(
                Elements.ComplexObjectWithID
                , ((ReferenceTargetProperty) property).reference
                , property.name
                , property)) {

            writePropertyHeader(
                    Elements.ComplexObject
                    , property.name
                    , property);
        }

        writeProperties(((ComplexProperty) property).properties);
    }


    private void writeProperties(List<Property> properties) {
        writeNumber((short) properties.size());
        for (Property property : properties) {
            serializeCore(property);
        }
    }

    private boolean writePropertyHeaderWithReferenceID(
            byte elementID
            , Reference reference
            , String name
            , Property property) {

        if (reference == null || reference.count < 2) {
            return false;
        }

        writePropertyHeader(elementID, name, property);
        writeNumber(reference.id);
        return true;
    }

    private void writeNumber(int n) {
        commandCache.add(new NumberWriteCommand(n));
    }

    private void serializeCollectionProperty(CollectionProperty property) {
        if (!writePropertyHeaderWithReferenceID(
                Elements.CollectionWithID
                , property.reference
                , property.name
                , property)) {

            writePropertyHeader(Elements.Collection, property.name, property);
        }

        writeType(property.elementType);
        writeProperties(property.properties);
        writeItems(property.items);
    }

    private void writeItems(List<Property> items) {
        writeNumber(items.size());
        for (Property item : items) {
            serializeCore(item);
        }
    }

    private void serializeDictionaryProperty(DictionaryProperty property) {
        if (!writePropertyHeaderWithReferenceID(
                Elements.DictionaryWithID
                , property.reference
                , property.name
                , property)) {

            writePropertyHeader(Elements.Dictionary, property.name, property);
        }

        writeType(property.keyType);
        writeType(property.valueType);
        writeProperties(property.properties);
        writeDictionaryItems(property.items);
    }

    private void writeDictionaryItems(List<Entry<Property, Property>> items) {
        writeNumber(items.size());
        for (Entry<Property, Property> item : items) {
            writeDictionaryItem(item);
        }
    }

    private void writeDictionaryItem(Entry<Property, Property> item) {
        serializeCore(item.getKey());
        serializeCore(item.getValue());
    }

    private void serializeSingleDimensionalArrayProperty(SingleDimensionalArrayProperty property) {

        if (!writePropertyHeaderWithReferenceID(
                Elements.SingleArrayWithID
                , property.reference
                , property.name
                , property)) {

            writePropertyHeader(Elements.SingleArray, property.name, property);
        }

        writeType(property.elementType);
        writeNumber(property.lowerBound);
        writeItems(property.items);
    }

    private boolean serializeReference(ReferenceTargetProperty property) {
        if (property.reference == null) {
            return false;
        }

        if (property.reference.count < 2 && !property.reference.isProcessed) {
            writePropertyHeader(Elements.Reference, property.name, null);
            writeNumber(property.reference.id);
            return true;
        }

        return false;
    }
}
