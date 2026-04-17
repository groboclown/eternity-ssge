package net.groboclown.essge.sharp;

import net.groboclown.essge.game.ObjectPersistencePacket;
import net.groboclown.essge.rw.Component;
import net.groboclown.essge.sharp.serializer.properties.ComplexProperty;
import net.groboclown.essge.sharp.serializer.properties.Property;
import net.groboclown.essge.sharp.serializer.properties.SingleDimensionalArrayProperty;

import java.util.*;

public class RootProperty {
    public static final UUID NULL_GUID = new UUID(0, 0);

    private final ComplexProperty property;
    private final ObjectPersistencePacket packet;

    public RootProperty(final Property property) {
        if (!(property instanceof ComplexProperty complex)
                || !(property.obj instanceof ObjectPersistencePacket pkt)) {
            throw new IllegalArgumentException();
        }
        this.property = complex;
        this.packet = pkt;
    }

    public ObjectPersistencePacket getPacket() {
        return this.packet;
    }

    public Iterable<Property> iterate() {
        return this.property.properties;
    }

    public Map<String, Property> map() {
        final Map<String, Property> ret = new HashMap<>();
        for (Property prop: this.property.properties) {
            if (ret.containsKey(prop.name)) {
                throw new IllegalStateException("multiple values in properties with same name");
            }
            ret.put(prop.name, prop);
        }
        return ret;
    }

    public Optional<Property> findFirst(final String name) {
        for (Property prop: this.property.properties) {
            if (name.equals(prop.name)) {
                return Optional.of(prop);
            }
        }
        return Optional.empty();
    }

    public Iterable<Component> getComponents() {
        Optional<Property> prop = findFirst("ComponentPackets");
        if (prop.isEmpty()) {
            return Collections.emptyList();
        }
        if (!(prop.get() instanceof SingleDimensionalArrayProperty items)) {
            return Collections.emptyList();
        }
        List<Component> ret = new ArrayList<>(items.items.size());
        for (Property p: items.items) {
            if (p instanceof ComplexProperty complex) {
                ret.add(new Component(complex));
            }
        }
        return ret;
    }

    public String getName() {
        return this.property.name;
    }

    public Property.PropertyArt getPropertyArt() {
        return this.property.getPropertyArt();
    }

    public static List<Property> forExport(final List<RootProperty> roots) {
        List<Property> ret = new ArrayList<>();
        for (RootProperty prop: roots) {
            ret.add(prop.property);
        }
        return ret;
    }
}
