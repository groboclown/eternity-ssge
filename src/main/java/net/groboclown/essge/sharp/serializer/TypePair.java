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

import java.util.HashMap;
import java.util.Map;

public class TypePair {
    public Class<?> type;
    public String cSharpType;

    public TypePair(Class<?> type, String cSharpType) {
        this.type = type;
        this.cSharpType = cSharpType;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TypePair other)) {
            return false;
        }

        return other.type == type && other.cSharpType.equals(cSharpType);
    }

    // Type used for when a mapping doesn't exist.
    public static class DebugInspect {
        public Map<String, String> fieldMapping = new HashMap<>();
    }
}
