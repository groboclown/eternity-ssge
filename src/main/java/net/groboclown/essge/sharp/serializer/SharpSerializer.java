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

// We host our own implementation of SharpSerializer which is used in
// Pillars of Eternity to serialize game objects into saves.

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import net.groboclown.essge.rw.EternityTypeMap;
import net.groboclown.essge.sharp.errors.InvalidPropertyException;
import net.groboclown.essge.sharp.errors.SharpException;
import net.groboclown.essge.sharp.errors.UnhandledPropertyException;
import net.groboclown.essge.sharp.serializer.properties.Property;
import net.groboclown.essge.sharp.serializer.properties.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Map.Entry;


public class SharpSerializer {
    public static final Map<String, Class<?>> typeMap = EternityTypeMap.map;
    private final Map<Integer, Property> propertyCache = new HashMap<>();

    public static class Elements {
        public static final byte Collection = 1;
        public static final byte ComplexObject = 2;
        public static final byte Dictionary = 3;
        public static final byte MultiArray = 4;
        public static final byte Null = 5;
        public static final byte SimpleObject = 6;
        public static final byte SingleArray = 7;
        public static final byte ComplexObjectWithID = 8;
        public static final byte Reference = 9;
        public static final byte CollectionWithID = 10;
        public static final byte DictionaryWithID = 11;
        public static final byte SingleArrayWithID = 12;
        public static final byte MultiArrayWithID = 13;

        public static boolean isElementWithID(byte elementID) {
            return elementID == ComplexObjectWithID
                    || elementID == CollectionWithID
                    || elementID == DictionaryWithID
                    || elementID == SingleArrayWithID
                    || elementID == MultiArrayWithID;
        }
    }

    private final File targetFile;
    private long position = 0;

    private SerializerFormat format = SerializerFormat.PRESERVE;

    public SharpSerializer(String filePath) throws FileNotFoundException {
        targetFile = new File(filePath);
        if (!targetFile.exists()) {
            throw new FileNotFoundException();
        }
    }

    public SharpSerializer toFormat(SerializerFormat format) {
        this.format = format;

        return this;
    }

    public Optional<Property> deserialize() throws IOException, SharpException {
        try (FileInputStream baseStream = new FileInputStream(targetFile)) {
            try (LittleEndianDataInputStream stream =
                         new LittleEndianDataInputStream(baseStream)) {

                baseStream.getChannel().position(position);
                Deserializer deserializer = new Deserializer(stream, this);
                Property property = deserializer.deserialize();
                position = baseStream.getChannel().position();

                return Optional.ofNullable(createObject(property));
            }
        }
    }

    public void serialize(Property property) throws SharpException, IOException {
        FileOutputStream baseStream = new FileOutputStream(
                targetFile
                , true);

        try (LittleEndianDataOutputStream stream =
                     new LittleEndianDataOutputStream(baseStream)) {

            baseStream.getChannel()
                    .position(baseStream.getChannel().size());

            Serializer serializer = new Serializer(stream).toFormat(format);
            serializer.serialize(property);
        }
    }

    public Optional<Property> followReference(final ReferenceTargetProperty property) {
        if (property.reference == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(propertyCache.get(property.reference.id));
    }

    private Property createObject(Property property) throws SharpException {
        if (property == null) {
            throw new NullPointerException("null property");
        }

        if (property instanceof NullProperty) {
            property.obj = null;
            return property;
        }

        if (property.type == null) {
            throw new NullPointerException("null property type");
        }

        if (property instanceof SimpleProperty) {
            return createObjectFromSimpleProperty((SimpleProperty) property);
        }

        if (!(property instanceof ReferenceTargetProperty referenceTarget)) {
            throw new IllegalArgumentException("Don't know what to do with this property!");
        }

        if (referenceTarget.reference != null
                && !referenceTarget.reference.isProcessed) {

            return propertyCache.get(referenceTarget.reference.id);
        }

        Property value = createObjectCore(property);
        if (value == null) {
            throw new UnhandledPropertyException(property.name);
        }

        return value;
    }

    private Property createObjectCore(Object property) throws SharpException {
        // MultiDimensionalArray

        if (property instanceof SingleDimensionalArrayProperty) {
            return createObjectFromSingleDimensionalArrayProperty(
                    (SingleDimensionalArrayProperty) property);
        }

        if (property instanceof DictionaryProperty) {
            return createObjectFromDictionaryProperty(
                    (DictionaryProperty) property);
        }

        if (property instanceof CollectionProperty) {
            return createObjectFromCollectionProperty(
                    (CollectionProperty) property);
        }

        if (property instanceof ComplexProperty) {
            return createObjectFromComplexProperty((ComplexProperty) property);
        }

        return null;
    }

    private Property createObjectFromCollectionProperty(
            CollectionProperty property) throws SharpException {

        Class<?> type = property.type.type;
        Object collection = createInstance(type);

        if (property.reference != null) {
            propertyCache.put(property.reference.id, property);
        }

        fillProperties(collection, property.properties);
        try {
            Method addMethod = collection.getClass().getMethod(
                    "add"
                    , Object.class);

            for (Property item : property.items) {
                Property value = createObject(item);
                addMethod.invoke(collection, value.obj);
            }
        } catch (NoSuchMethodException e) {
            throw new InvalidPropertyException("Supposed 'Collection' class '" + collection.getClass().getSimpleName() +
                    "' had no add method: " + e.getMessage());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new InvalidPropertyException("Unable to call add method on class '" +
                    collection.getClass().getSimpleName() + "': " + e.getMessage());
        }

        property.obj = collection;
        return property;
    }

    private Property createObjectFromDictionaryProperty(
            DictionaryProperty property) throws SharpException {

        Object dictionary = createInstance(property.type.type);

        if (property.reference != null) {
            propertyCache.put(property.reference.id, property);
        }

        fillProperties(dictionary, property.properties);
        try {
            Method putMethod = dictionary.getClass().getMethod(
                    "put"
                    , Object.class
                    , Object.class);

            for (Entry<Property, Property> item : property.items) {
                Property key = createObject(item.getKey());
                Property value = createObject(item.getValue());
                putMethod.invoke(dictionary, key.obj, value.obj);
            }
        } catch (NoSuchMethodException e) {
            throw new InvalidPropertyException("Supposed 'Dictionary' class '" + dictionary.getClass().getSimpleName() +
                    "' had no put method: " + e.getMessage());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new InvalidPropertyException("Unable to call put method on class '" +
                    dictionary.getClass().getSimpleName() + "' :" + e.getMessage());
        }

        property.obj = dictionary;
        return property;
    }

    private Property createObjectFromSingleDimensionalArrayProperty(
            SingleDimensionalArrayProperty property) throws SharpException {

        int itemsCount = property.items.size();
        Object[] array = new Object[itemsCount];

        if (property.reference != null) {
            propertyCache.put(property.reference.id, property);
        }

        for (int index = property.lowerBound; index < property.lowerBound + itemsCount; index++) {
            Property item = property.items.get(index);
            Property value = createObject(item);
            if (value != null) {
                array[index] = value.obj;
            }
        }

        //noinspection unchecked
        property.obj = Arrays.copyOf(array, array.length, (Class<Object[]>) property.type.type);

        return property;
    }

    private Property createObjectFromComplexProperty(ComplexProperty property) throws SharpException {
        Object obj = createInstance(property.type.type);
        if (obj == null) {
            return null;
        }

        if (property.reference != null) {
            propertyCache.put(property.reference.id, property);
        }

        fillProperties(obj, property.properties);
        property.obj = obj;
        return property;
    }

    private void fillProperties(Object obj, List<Property> properties) throws SharpException {
        for (Property property : properties) {
            Field field;

            try {
                field = obj.getClass().getField(property.name);
            } catch (NoSuchFieldException e) {
                // TODO throw an exception?
                //   Otherwise, this leads to data loss.
                System.err.println(
                        "Class '" + obj.getClass().getSimpleName() + "' has no field '" +
                                property.name + "': " + e.getMessage());

                continue;
            }

            Property value = createObject(property);
            if (value == null) {
                continue;
            }

            try {
                field.set(obj, value.obj);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new InvalidPropertyException("Unable to set field '" + property.name +
                        "' of class '" + obj.getClass().getSimpleName() + "': " + e.getMessage());
            }
        }
    }

    private Object createInstance(Class<?> type) throws InvalidPropertyException {
        if (type == null) {
            return null;
        }

        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InvalidPropertyException("Unable to instantiate object of type '" + type.getSimpleName() + "': " + e.getMessage());
        }
    }

    private Property createObjectFromSimpleProperty(SimpleProperty property) {
        property.obj = property.value;
        return property;
    }
}
