/**
 *  Eternity Keeper, a Pillars of Eternity save game editor.
 *  Copyright (C) 2015 the authors.
 *
 *  Eternity Keeper is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  Eternity Keeper is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.groboclown.essge.sharp.serializer.properties;

import net.groboclown.essge.sharp.errors.ReferenceMismatchException;
import net.groboclown.essge.sharp.errors.SharpException;
import net.groboclown.essge.sharp.serializer.TypePair;

import java.util.ArrayList;
import java.util.List;

public class SingleDimensionalArrayProperty extends ReferenceTargetProperty {
	public TypePair elementType;
	public int lowerBound;
	public List<Property> items = new ArrayList<>();

	public SingleDimensionalArrayProperty (String name, TypePair type) {
		super(name, type);
	}

	@Override
	public PropertyArt getPropertyArt () {
		return PropertyArt.SingleDimensionalArray;
	}

	@Override
	public void makeFlatCopyFrom (ReferenceTargetProperty source) throws SharpException {
		if (source instanceof SingleDimensionalArrayProperty) {
			super.makeFlatCopyFrom(source);
			lowerBound = ((SingleDimensionalArrayProperty) source).lowerBound;
			elementType = ((SingleDimensionalArrayProperty) source).elementType;
			items = ((SingleDimensionalArrayProperty) source).items;
		} else {
			throw new ReferenceMismatchException(source, "SingleDimensionalArrayProperty");
		}
	}
}
