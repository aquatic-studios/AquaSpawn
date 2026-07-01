package com.aquaticstudios.aquaspawn.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public final class MenuHolder implements InventoryHolder {

    private final Map<Integer, MenuItem> slots = new HashMap<>();
    private Inventory inventory;

    public Map<Integer, MenuItem> slots() {
        return slots;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
