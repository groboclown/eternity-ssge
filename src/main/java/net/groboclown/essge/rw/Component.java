package net.groboclown.essge.rw;

import net.groboclown.essge.game.ComponentPersistencePacket;
import net.groboclown.essge.sharp.serializer.properties.ComplexProperty;
import net.groboclown.essge.sharp.serializer.properties.DictionaryProperty;
import net.groboclown.essge.sharp.serializer.properties.Property;
import net.groboclown.essge.sharp.serializer.properties.SimpleProperty;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A hybrid data object to/from serial format handler of a ComponentPersistencePacket property.
 */
public class Component {
    private final ComponentPersistencePacket packet;
    private final Map<String, Property> serialized;
    private final ComplexProperty property;

    public Component(ComplexProperty property) {
        if (property == null || !(property.obj instanceof ComponentPersistencePacket pkt)) {
            throw new IllegalArgumentException();
        }
        this.property = property;
        if (property.properties.size() != 2) {
            throw new IllegalArgumentException();
        }
        final Property variablesProp = property.properties.get(1);
        if (!(variablesProp instanceof DictionaryProperty variables)) {
            throw new IllegalArgumentException();
        }
        if (!"Variables".equals(variables.name)) {
            throw new IllegalArgumentException();
        }

        this.packet = pkt;
        this.serialized = new HashMap<>();
        for (Map.Entry<Property, Property> entry: variables.items) {
            if (!(entry.getKey() instanceof SimpleProperty key) || !(key.value instanceof String keyName)) {
                throw new IllegalArgumentException();
            }
            this.serialized.put(keyName, entry.getValue());
        }
    }

    public Property getLowProperty() {
        return this.property;
    }

    public String getType() {
        return this.packet.TypeString;
    }

    public Map<String, Object> listObjs() {
        return this.serialized.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().obj))
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get the high-level version of the property.
     * @param name the key in the complex property.
     * @return the value or empty.
     * @param <T> the expected type of the value.
     */
    public <T> Optional<T> getObj(final String name, Class<T> expected) {
        Property val = this.serialized.get(name);
        if (val == null) {
            return Optional.empty();
        }
        return Optional.of(expected.cast(val.obj));
    }

    public <T extends Property> Optional<T> getProp(final String name, Class<T> expected) {
        Property val = this.serialized.get(name);
        if (val == null) {
            return Optional.empty();
        }
        return Optional.of(expected.cast(val));
    }

    public <T> boolean setSimple(final String name, T val) {
        final Property prop = this.serialized.get(name);
        if (prop == null) {
            return false;
        }
        final boolean ret = PropertyUtil.setSimpleValue(prop, val);
        if (!ret) {
            return false;
        }

        // but it needs to be set in the object representation (dictionary), too.
        this.packet.Variables.put(name, val);
        // and, it should also be set in the object representation of the "Variables" packet,
        // but that's going too far.
        return true;
    }
}
