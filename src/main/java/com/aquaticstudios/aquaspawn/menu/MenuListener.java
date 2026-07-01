package com.aquaticstudios.aquaspawn.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MenuListener implements Listener {

    private final MenuManager menu;

    public MenuListener(MenuManager menu) {
        this.menu = menu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        menu.handleClick(event);
    }
}
