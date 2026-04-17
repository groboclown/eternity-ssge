package net.groboclown.essge.filetypes;

import net.groboclown.essge.rw.GameCharacter;
import net.groboclown.essge.game.ObjectPersistencePacket;
import net.groboclown.essge.sharp.RootProperty;
import net.groboclown.essge.sharp.serializer.DeserializedPackets;
import net.groboclown.essge.sharp.serializer.properties.Property;
import net.groboclown.essge.sharp.serializer.properties.SimpleProperty;

import java.util.*;

/**
 * Easy access to the mobile objects.
 * <p>
 * Note that adding packets isn't currently supported.
 */
public class MobileObjects {
    private final List<Property> packets;
    private final List<RootProperty> roots;
    private final SimpleProperty count;


    public MobileObjects(List<Property> packets, SimpleProperty count) {
        this.packets = packets;
        this.count = count;

        final List<RootProperty> roots = new ArrayList<>();
        for (Property prop: packets) {
            roots.add(new RootProperty(prop));
        }
        this.roots = roots;
    }

    public Iterable<RootProperty> getRoots() {
        return Collections.unmodifiableCollection(this.roots);
    }

    public DeserializedPackets asDeserialized() {
        return new DeserializedPackets(this.packets, this.count);
    }

    public List<GameCharacter> findCompanionObjects() {
        List<GameCharacter> ret = new ArrayList<>();
        for (RootProperty prop: this.roots) {
            if (prop.getPacket().ObjectName.startsWith("Companion_")) {
                ret.add(new GameCharacter(this, prop));
            }
        }
        return ret;
    }

    public List<GameCharacter> findPlayerObjects() {
        List<GameCharacter> ret = new ArrayList<>();
        for (RootProperty prop: this.roots) {
            // Generally, "Player_New_Game*"
            if (prop.getPacket().ObjectName.startsWith("Player_")) {
                ret.add(new GameCharacter(this, prop));
            }
        }
        return ret;
    }

    public Optional<RootProperty> findByObjectId(final String guid) {
        if (guid == null || guid.isEmpty() || guid.equals(RootProperty.NULL_GUID.toString())) {
            // Null object
            return Optional.empty();
        }
        for (RootProperty prop: this.roots) {
            // Generally, "Player_New_Game*"
            if (prop.getPacket().ObjectID.equals(guid)) {
                return Optional.of(prop);
            }
        }
        return Optional.empty();
    }

    public Optional<RootProperty> findByUUID(final UUID guid) {
        if (guid == null || RootProperty.NULL_GUID.equals(guid)) {
            // Null object
            return Optional.empty();
        }
        for (RootProperty prop: this.roots) {
            // Generally, "Player_New_Game*"
            if (prop.getPacket().GUID.equals(guid)) {
                return Optional.of(prop);
            }
        }
        return Optional.empty();
    }

    public List<RootProperty> extractRelatedObjects(final UUID guid) {
        final List<RootProperty> objs = new ArrayList<>();
        final Optional<RootProperty> obj = findByUUID(guid);
        if (obj.isEmpty()) {
            return objs;
        }
        final String objectName = obj.get().getPacket().ObjectName;
        final String objectID = obj.get().getPacket().ObjectID;

        for (RootProperty root: this.roots) {
            ObjectPersistencePacket packet = root.getPacket();
            if (packet.ObjectID.equals(objectID) || packet.Parent.equals(objectName)) {
                objs.add(root);
            }
        }

        return objs;
    }

}
