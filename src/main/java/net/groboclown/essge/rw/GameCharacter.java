package net.groboclown.essge.rw;

import net.groboclown.essge.filetypes.MobileObjects;
import net.groboclown.essge.game.ComponentPersistencePacket;
import net.groboclown.essge.game.InventoryItem;
import net.groboclown.essge.sharp.RootProperty;
import net.groboclown.essge.sharp.serializer.CSharpCollection;

import java.util.*;

public class GameCharacter {
    private final MobileObjects mobile;
    private final RootProperty root;
    private final Iterable<Component> components;

    public GameCharacter(MobileObjects mobile, RootProperty root) {
        this.mobile = mobile;
        this.root = root;
        this.components = root.getComponents();
    }

    public String getObjectName() {
        return root.getPacket().ObjectName;
    }

    public UUID getGuid() {
        return root.getPacket().GUID;
    }

    public void printPacket() {
        for (ComponentPersistencePacket packet: this.root.getPacket().ComponentPackets) {
            System.out.println(" - " + packet.TypeString);
            for (Map.Entry<String, Object> e: packet.Variables.entrySet()) {
                System.out.println("   " + e.getKey() + ": " + (e.getValue() == null ? "null" : e.getValue().getClass().getSimpleName()) + "(" + e.getValue() + ")");
            }
        }
    }

    public Optional<String> getCharacterName() {
        Optional<Object> oName = getStats()
                .map(s -> s.getValue("OverrideName"));
        if (oName.isEmpty()) {
            return Optional.empty();
        }
        Object val = oName.get();
        if (val instanceof String sVal) {
            return Optional.of(sVal);
        }
        return Optional.empty();
    }

    private Optional<Component> findNamed(final String name) {
        for (Component packet: this.components) {
            if (name.equals(packet.getType())) {
                return Optional.of(packet);
            }
        }
        return Optional.empty();
    }

    public Optional<InventoryType> getStash() {
        return findNamed("StashInventory").map(e -> new InventoryType(this.mobile, e));
    }

    public Optional<InventoryType> getPlayerInventory() {
        return findNamed("PlayerInventory").map(e -> new InventoryType(this.mobile, e));
    }

    public Optional<InventoryType> getQuestInventory() {
        return findNamed("QuestInventory").map(e -> new InventoryType(this.mobile, e));
    }

    public Optional<InventoryType> getQuickbarInventory() {
        return findNamed("QuickbarInventory").map(e -> new InventoryType(this.mobile, e));
    }

    public Float getCurrencyTotalValue() {
        return getPlayerInventory().map(InventoryType::getTotalCurrency).orElse(null);
    }

    public boolean setCurrencyTotalValue(float value) {
        return getPlayerInventory().map(i -> i.setTotalCurrency(value)).orElse(false);
    }

    // Also:
    //    "Mover" entry, which contains "RunSpeed" (Float) + "WalkSpeed" (Float)

    // ----------------------------------------------------------------
    public static class Equipment {
        private final Component packet;
        private final MobileObjects mobile;

        public Equipment(MobileObjects mobile, Component packet) {
            this.mobile = mobile;
            this.packet = packet;
        }

        public int getEquippedItemCount() {
            return getListCount("EquipmentSetSerialized");
        }

        public Optional<RootProperty> getEquippedItem(int index) {
            // TODO cast this to the right type
            return getItemAt("EquipmentSetSerialized", index);
        }

        public Iterable<RootProperty> iterEquippedItems() {
            return copyListFor("EquipmentSetSerialized");
        }

        public int getWeaponSetsItemCount() {
            return getListCount("WeaponSetsSerialized");
        }

        public Optional<RootProperty> getWeaponSetsItem(int index) {
            // TODO cast this to the right type
            return getItemAt("WeaponSetsSerialized", index);
        }

        public Iterable<RootProperty> iterWeaponSets() {
            return copyListFor("WeaponSetsSerialized");
        }

        // TODO These are wrong - they return the non-serial version.

        private CSharpCollection getList(String name) {
            Optional<CSharpCollection> list = this.packet.getObj(name, CSharpCollection.class);
            return list.orElse(null);
        }

        private int getListCount(String name) {
            CSharpCollection list = getList(name);
            if (list == null) {
                return 0;
            }
            return list.size();
        }

        private Optional<RootProperty> getItemAt(String name, int index) {
            CSharpCollection list = getList(name);
            if (list == null) {
                return Optional.empty();
            }
            Optional<Object> raw = list.indexAt(index);
            if (raw.isEmpty()) {
                return Optional.empty();
            }
            Object val = raw.get();
            if (!(val instanceof UUID)) {
                return Optional.empty();
            }
            return this.mobile.findByUUID((UUID) val);
        }

        private List<RootProperty> copyListFor(String name) {
            CSharpCollection list = getList(name);
            if (list == null) {
                return Collections.emptyList();
            }
            List<RootProperty> ret = new ArrayList<>();
            for (Iterator<Object> it = list.iterator(); it.hasNext(); ) {
                Object val = it.next();
                if (val instanceof UUID) {
                    Optional<RootProperty> found = this.mobile.findByUUID((UUID) val);
                    found.ifPresent(ret::add);
                }
            }
            return ret;
        }
    }

    public Optional<Equipment> getEquipment() {
        return findNamed("Equipment").map(e -> new Equipment(this.mobile, e));
    }



    // ----------------------------------------------------------------
    public static class Inventory {
        private final Component packet;

        public Inventory(Component packet) {
            this.packet = packet;
        }

        // Because of ItemList vs SerializedItemList, only allow updating the retrieved value.
        public int getItemCount() {
            CSharpCollection list = getList();
            if (list == null) {
                return 0;
            }
            return list.size();
        }

        public Optional<InventoryItem> getItemAt(int index) {
            CSharpCollection list = getList();
            if (list == null) {
                return Optional.empty();
            }
            Optional<Object> raw = list.indexAt(index);
            if (raw.isEmpty()) {
                return Optional.empty();
            }
            Object val = raw.get();
            if (val instanceof InventoryItem) {
                return Optional.of((InventoryItem) val);
            }
            return Optional.empty();
        }

        public Iterable<InventoryItem> iterate() {
            CSharpCollection list = getList();
            if (list == null) {
                return Collections.emptyList();
            }
            List<InventoryItem> ret = new ArrayList<>();
            for (Iterator<Object> it = list.iterator(); it.hasNext(); ) {
                Object val = it.next();
                if (val instanceof InventoryItem) {
                    ret.add((InventoryItem) val);
                }
            }
            return ret;
        }

        private CSharpCollection getList() {
            Optional<CSharpCollection> list = this.packet.getObj("ItemList", CSharpCollection.class);
            return list.orElse(null);
        }
    }

    public Optional<Inventory> getInventory() {
        Optional<Component> ret = findNamed("Inventory");
        if (ret.isEmpty()) {
            ret = findNamed("PlayerInventory");
        }
        return ret.map(Inventory::new);
    }



    // ----------------------------------------------------------------
    public static class Stats {
        private final Component component;

        public Stats(Component component) {
            this.component = component;
        }

        public Component getComponent() {
            return this.component;
        }

        public Integer getSkill(String name) {
            Optional<Integer> ret = this.component.getObj(name + "Skill", Integer.class);
            return ret.orElse(null);
        }

        public boolean setSkill(String name, int newVal) {
            if (newVal < 0) {
                throw new IllegalArgumentException("bad skill value: " + newVal);
            }
            return this.component.setSimple(name + "Skill", newVal);
        }

        public int getExperience() {
            return this.component.getObj("Experience", Integer.class).orElse(-1);
        }

        public boolean setExperience(int newVal) {
            if (newVal < 0) {
                throw new IllegalArgumentException("bad experience value: " + newVal);
            }
            return this.component.setSimple("Experience", newVal);
        }

        public int getBaseStat(String name) {
            return this.component.getObj("Base" + name, Integer.class).orElse(-1);
        }

        public boolean setBaseStat(String name, int newVal) {
            if (newVal < 0) {
                throw new IllegalArgumentException("bad stat value: " + newVal);
            }
            return this.component.setSimple("Base" + name, newVal);
        }

        public Object getValue(String name) {
            return this.component.getObj(name, Object.class).orElse(null);
        }

        public boolean setValue(String name, Object newVal) {
            return this.component.setSimple(name, newVal);
        }

        public boolean setFloatValue(String name, float newVal) {
            return this.component.setSimple(name, newVal);
        }

        public Float getFloatValue(String name) {
            return this.component.getObj(name, Float.class).orElse(null);
        }
    }

    public Optional<Stats> getStats() {
        return findNamed("CharacterStats").map(Stats::new);
    }
}
