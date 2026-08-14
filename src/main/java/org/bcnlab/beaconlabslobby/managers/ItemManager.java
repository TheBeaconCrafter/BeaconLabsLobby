package org.bcnlab.beaconlabslobby.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;
import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {

    private final BeaconLabsLobby plugin;
    private final NamespacedKey itemKey;
    private final MiniMessage miniMessage;
    private final Map<String, ItemStack> itemTemplates = new HashMap<>();

    public ItemManager(BeaconLabsLobby plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "lobby_item");
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void giveJoinItems(Player player) {
        player.getInventory().clear();
        FileConfiguration config = plugin.getConfig();
        giveItem(player, config, "server-selector", "selector");
        giveItem(player, config, "private-server-selector", "privateselector");
        giveItem(player, config, "player-hider", "hider");
        giveItem(player, config, "settings", "settings");
        giveItem(player, config, "friends", "friends");
    }

    public void reload() {
        itemTemplates.clear();
    }

    private void giveItem(Player player, FileConfiguration config, String configPath, String actionId) {
        if (configPath.equals("private-server-selector")) {
            if (!config.getBoolean("private-server-selector.enabled", false)) return;
            String permission = config.getString("private-server-selector.permission", "beaconlabslobby.privateselector");
            if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) return;
        }

        ConfigurationSection section = config.getConfigurationSection(configPath + ".settings");
        if (section == null) return;

        String name = section.getString("name", "Item");
        String typeName = section.getString("type", "STONE");
        Material type = Material.matchMaterial(typeName);
        if (type == null) type = Material.STONE;
        List<String> lore = section.getStringList("lore");
        int slot = section.getInt("slot", 0);

        ItemStack item = itemTemplates.get(configPath);
        if (item == null) {
            item = new ItemStack(type);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(parseText(name).decoration(TextDecoration.ITALIC, false));

                List<Component> componentLore = new ArrayList<>(lore.size());
                for (String line : lore) {
                    componentLore.add(parseText(line).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(componentLore);
                meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, actionId);
                item.setItemMeta(meta);
            }
            itemTemplates.put(configPath, item);
        }

        // Each player needs their own head owner, so clone the cached template before giving it.
        item = item.clone();
        if (type == Material.PLAYER_HEAD && item.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            item.setItemMeta(skullMeta);
        }
        player.getInventory().setItem(slot, item);
    }

    public String getActionId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    private Component parseText(String text) {
        if (text.contains("&") && !text.contains("<") && !text.contains(">")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
        return miniMessage.deserialize(text);
    }
}
