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


package net.groboclown.essge.sharp.serializer.properties;

import net.groboclown.essge.sharp.errors.InvalidPropertyException;
import net.groboclown.essge.sharp.errors.SharpException;
import net.groboclown.essge.sharp.errors.SubPropertyNotFoundException;
import net.groboclown.essge.sharp.errors.UnhandledPropertyException;
import net.groboclown.essge.sharp.serializer.TypePair;

import java.util.Optional;

public abstract class Property {
    public String name;
    public TypePair type;
    public Property parent;
    public Object obj;

    Property(String name, TypePair type) {
        this.name = name;
        this.type = type;
    }

    public abstract PropertyArt getPropertyArt();

    public static Property createInstance(
            PropertyArt art
            , String propertyName
            , TypePair propertyType) throws SharpException {

        return switch (art) {
            case Collection -> new CollectionProperty(propertyName, propertyType);
            case Complex -> new ComplexProperty(propertyName, propertyType);
            case Dictionary -> new DictionaryProperty(propertyName, propertyType);
            case Null -> new NullProperty(propertyName);
            case Reference -> null;
            case Simple -> new SimpleProperty(propertyName, propertyType);
            case SingleDimensionalArray -> new SingleDimensionalArrayProperty(propertyName, propertyType);
            default -> throw new UnhandledPropertyException(art.name());
        };
    }

    public static boolean update(
            final Property property
            , final String propertyName
            , final Object value) throws SharpException {
        final Optional<Property> subProperty = find(property, propertyName);
        if (subProperty.isEmpty()) {
            throw new SubPropertyNotFoundException(propertyName);
        }

        return update(subProperty.get(), value);
    }

    public static boolean update(final Property property, final Object value) throws SharpException {
        if (!(property instanceof SimpleProperty simpleProperty)) {
            throw new InvalidPropertyException("Property was not a SimpleProperty.%n");
        }

        simpleProperty.value = value;
        simpleProperty.obj = value;

        return true;
    }

    public static Optional<Property> find(final Property haystack, final String needle) {
        if (!(haystack instanceof ComplexProperty complexHaystack)) {
            return Optional.empty();
        }

        for (final Property subProperty : complexHaystack.properties) {
            if (subProperty.name.equals(needle)) {
                return Optional.of(subProperty);
            }
        }

        return Optional.empty();
    }

    public enum PropertyArt {
        Unknown, Simple, Complex, Collection, Dictionary, SingleDimensionalArray, MultiDimensionalArray, Null, Reference
    }
}
