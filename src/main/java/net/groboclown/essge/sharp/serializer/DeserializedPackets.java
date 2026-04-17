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

import net.groboclown.essge.sharp.errors.SharpException;
import net.groboclown.essge.sharp.serializer.properties.Property;
import net.groboclown.essge.sharp.serializer.properties.SimpleProperty;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DeserializedPackets {
    private List<Property> packets;
    private final SimpleProperty count;

    public DeserializedPackets(final List<Property> packets, final SimpleProperty count) {
        this.packets = packets;
        this.count = count;
    }

    public List<Property> getPackets() {
        return packets;
    }

    public void setPackets(final List<Property> packets) {
        this.packets = packets;
    }

    public SimpleProperty getCount() {
        return count;
    }

    public void reserialize(final File destinationFile) throws IOException, SharpException {
        reserialize(destinationFile, SerializerFormat.PRESERVE);
    }

    public void reserialize(final File destinationFile, SerializerFormat outputFormat) throws IOException, SharpException {
        final SharpSerializer serializer = new SharpSerializer(destinationFile.getAbsolutePath()).toFormat(outputFormat);

        serializer.serialize(this.count);
        for (final Property property : packets) {
            serializer.serialize(property);
        }
    }
}
