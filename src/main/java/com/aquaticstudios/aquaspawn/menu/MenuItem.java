package com.aquaticstudios.aquaspawn.menu;

import java.util.List;

public final class MenuItem {

    private final int slot;
    private final String material;
    private final String displayName;
    private final List<String> lore;
    private final List<String> leftClick;
    private final List<String> rightClick;

    public MenuItem(int slot, String material, String displayName,
                    List<String> lore, List<String> leftClick, List<String> rightClick) {
        this.slot = slot;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
    }

    public int slot() {
        return slot;
    }

    public String material() {
        return material;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> lore() {
        return lore;
    }

    public List<String> leftClick() {
        return leftClick;
    }

    public List<String> rightClick() {
        return rightClick;
    }
}
