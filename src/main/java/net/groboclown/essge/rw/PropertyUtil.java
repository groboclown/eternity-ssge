package net.groboclown.essge.rw;

import net.groboclown.essge.game.ComponentPersistencePacket;
import net.groboclown.essge.sharp.serializer.properties.Property;
import net.groboclown.essge.sharp.serializer.properties.SimpleProperty;

import java.util.Optional;

/**
 * General helpers for working with the list of properties in the save game.
 */
public class PropertyUtil {

/*
    public static List<Property> extractCharactersObjects(List<Property> mobileObjects) {
        List<Property> objs = new ArrayList<>();
        Optional<String> objectName = getObjectNameForGUID(mobileObjects);

        if (!objectName.isPresent()) {
            return objs;
        }

        for (Property property : mobileObjects) {
            ObjectPersistencePacket packet =
                    (ObjectPersistencePacket) property.obj;

            if (packet.ObjectID.equals(guid)
                    || packet.Parent.equals(objectName.get())) {

                objs.add(property);
            }
        }

        return objs;
    }
 */

    public static <T> boolean setSimpleValue(Property prop, T newVal) {
        if (!(prop instanceof SimpleProperty simp)) {
            return false;
        }
        if (newVal == null) {
            // Not 100% right.  The Float/Integer/etc values shouldn't allow this.
            simp.value = null;
            return true;
        }
        if (simp.value.getClass().equals(newVal.getClass())) {
            // not instanceof!

            // Note, set it in both places.  'value' is the serialized version, which will be put back in the end.
            simp.value = newVal;
            simp.obj = newVal;
            return true;
        }
        return false;
    }

    public static Optional<Integer> getIntegerValue(ComponentPersistencePacket packet, String name) {
        Object val = packet.Variables.get(name);
        if (val == null) {
            return Optional.empty();
        }
        if (val instanceof Integer) {
            return Optional.of((Integer) val);
        }
        return Optional.empty();
    }

    public static Optional<Float> getFloatValue(ComponentPersistencePacket packet, String name) {
        Object val = packet.Variables.get(name);
        if (val == null) {
            return Optional.empty();
        }
        if (val instanceof Float) {
            return Optional.of((Float) val);
        }
        return Optional.empty();
    }
}
