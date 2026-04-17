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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class CSharpCollection {
    private final List<Object> backing = new ArrayList<>();

    // Intentionally use the non-java capitalization to conform to C# naming.
    public int Capacity;

    public void add(Object item) {
        backing.add(item);
    }

    public int size() { return backing.size(); }

    public Optional<Object> indexAt(int i) {
        if (i >= 0 && i < backing.size()) {
            return Optional.of(backing.get(i));
        }
        return Optional.empty();
    }

    public Iterator<Object> iterator() {
        return backing.iterator();
    }
}
