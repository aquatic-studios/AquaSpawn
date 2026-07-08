package com.aquaticstudios.aquaspawn.menu;

import com.aquaticstudios.aquaspawn.config.ConfigFile;
import com.aquaticstudios.aquaspawn.utils.ColorUtils;
import com.aquaticstudios.aquaspawn.utils.Items;
import com.aquaticstudios.aquaspawn.utils.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class MenuManager {

    private final Plugin plugin;
    private final ConfigFile menuFile;
    private final ActionHandler actions;

    private String title = "Menu";
    private int size = 54;
    private final List<MenuItem> items = new ArrayList<>();

    public MenuManager(Plugin plugin, ConfigFile menuFile) {
        this.plugin = plugin;
        this.menuFile = menuFile;
        this.actions = new ActionHandler(plugin);
        load();
    }

    public void load() {
        items.clear();
        title = ColorUtils.color(menuFile.get().getString("menu_title", "&#54ADF4&lAquaSpawn"));
        size = clampSize(menuFile.get().getInt("size", 45));

        ConfigurationSection itemsSection = menuFile.get().getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection item = itemsSection.getConfigurationSection(key);
            if (item == null) {
                continue;
            }
            int slot = item.getInt("slot", -1);
            if (slot < 0 || slot >= size) {
                continue;
            }
            items.add(new MenuItem(
                    slot,
                    item.getString("material", "STONE"),
                    item.getString("display_name", " "),
                    item.getStringList("lore"),
                    item.getStringList("left_click_commands"),
                    item.getStringList("right_click_commands")
            ));
        }
    }

    public void open(Player player) {
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        for (MenuItem item : items) {
            inventory.setItem(item.slot(), build(item, player));
            holder.slots().put(item.slot(), item);
        }
        player.openInventory(inventory);
    }

    private ItemStack build(MenuItem item, Player player) {
        ItemStack stack = new ItemStack(Items.material(item.material()));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.color(Placeholders.apply(player, item.displayName())));
            List<String> lore = new ArrayList<>(item.lore().size());
            for (String line : item.lore()) {
                lore.add(ColorUtils.color(Placeholders.apply(player, line)));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player) || event.getClickedInventory() == null) {
            return;
        }
        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        MenuItem item = holder.slots().get(event.getRawSlot());
        if (item == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        actions.run(player, event.isRightClick() ? item.rightClick() : item.leftClick());
    }

    private static int clampSize(int configured) {
        if (configured < 9) {
            return 9;
        }
        if (configured > 54) {
            return 54;
        }
        return (configured / 9) * 9 == configured ? configured : ((configured / 9) + 1) * 9;
    }
}
