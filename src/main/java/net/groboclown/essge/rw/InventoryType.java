package net.groboclown.essge.rw;

import net.groboclown.essge.filetypes.MobileObjects;
import net.groboclown.essge.game.CurrencyValue;
import net.groboclown.essge.game.InventoryItem;
import net.groboclown.essge.sharp.serializer.properties.CollectionProperty;
import net.groboclown.essge.sharp.serializer.properties.ComplexProperty;
import net.groboclown.essge.sharp.serializer.properties.Property;
import net.groboclown.essge.sharp.serializer.properties.SimpleProperty;

import java.util.*;

/**
 * A property that the game uses as an Inventory.
 * <p>
 * In general, the inventory contains:
 * <ul>
 *   <li>ItemList (CSharpCollection) containing InventoryItem</li>
 *   <li>SerializedItemList (CSharpCollection) containing UUID for each InventoryItem.</li>
 * </ul>
 * <p>
 * The "PlayerInventory" also has:
 *     currencyTotalValue (type CurrencyValue)
 *     campingSupplies (type Integer)
 * The "QuestInventory" also has:
 *     HasNew
 */
public class InventoryType {
    private final Component packet;
    private final MobileObjects mobile;

    public InventoryType(MobileObjects mobile, Component packet) {
        this.packet = packet;
        this.mobile = mobile;
    }

    public Integer getMaxItems() {
        return this.packet.getObj("MaxItems", Integer.class).orElse(null);
    }

    public <T> Optional<T> getObj(final String name, Class<T> type) {
        return this.packet.getObj(name, type);
    }

    /**
     * Only applies to the PlayerInventory object.
     * @return total currency for the party, or -1.0 if not found.
     */
    public Float getTotalCurrency() {
        // FIXME this isn't the right place that contains the actual currency held.
        Optional<CurrencyValue> currency = this.packet.getObj("currencyTotalValue", CurrencyValue.class);
        return currency.map(c -> c.v).orElse(null);
    }

    public boolean setTotalCurrency(float value) {
        Optional<ComplexProperty> currencyProp = this.packet.getProp("currencyTotalValue", ComplexProperty.class);
        if (currencyProp.isEmpty()) {
            return false;
        }
        ComplexProperty owner = currencyProp.get();
        if (owner.properties.size() != 1) {
            return false;
        }
        if (!(owner.obj instanceof CurrencyValue cv)) {
            return false;
        }
        Property valueProp = owner.properties.getFirst();
        if (!(valueProp instanceof SimpleProperty sp)) {
            return false;
        }
        if (!"v".equals(sp.name) || !(sp.value instanceof Float)) {
            return false;
        }
        sp.value = value;
        sp.obj = value;
        cv.v = value;
        return true;
    }

    public Collection<Item> list() {
        List<Item> ret = new ArrayList<>();
        CollectionProperty items = getItemList();
        CollectionProperty serials = getSerializedItemList();
        int size = Math.min(items.items.size(), serials.items.size());
        for (int i = 0; i < size; i++) {
            Property item = items.items.get(i);
            Property serial = serials.items.get(i);
            if (!(item instanceof ComplexProperty ci) || !(serial instanceof SimpleProperty si)) {
                continue;
            }
            ret.add(new Item(si, ci, i));
        }
        return ret;
    }

    private CollectionProperty getItemList() {
        return this.packet.getProp("ItemList", CollectionProperty.class).orElseThrow();
    }

    private CollectionProperty getSerializedItemList() {
        return this.packet.getProp("SerializedItemList", CollectionProperty.class).orElseThrow();
    }


    public static class Item {
        private final SimpleProperty serial;
        private final ComplexProperty item;
        private final int index;

        public Item(SimpleProperty serial, ComplexProperty item, int index) {
            this.serial = serial;
            this.item = item;
            this.index = index;
        }

        public UUID getGuid() {
            return (UUID) serial.obj;
        }

        public ComplexProperty getProperty() {
            return this.item;
        }

        public InventoryItem get() {
            return (InventoryItem) this.item.obj;
        }

        public int getIndex() {
            return index;
        }
    }
}
