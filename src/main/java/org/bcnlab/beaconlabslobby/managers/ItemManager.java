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
import java.util.List;

public class ItemManager {

    private final BeaconLabsLobby plugin;
    private final NamespacedKey itemKey;
    private final MiniMessage miniMessage;

    public ItemManager(BeaconLabsLobby plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "lobby_item");
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void giveJoinItems(Player player) {
        player.getInventory().clear();
        giveItem(player, "server-selector", "selector");
        giveItem(player, "private-server-selector", "privateselector");
        giveItem(player, "player-hider", "hider");
        giveItem(player, "settings", "settings");
        giveItem(player, "friends", "friends");
    }

    private void giveItem(Player player, String configPath, String actionId) {
        FileConfiguration config = plugin.getConfig();
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

        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(parseText(name).decoration(TextDecoration.ITALIC, false));
            
            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(parseText(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(componentLore);
            
            // Handle Player Head
            if (type == Material.PLAYER_HEAD && meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(player);
            }

            // Mark item with PDC
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, actionId);
            item.setItemMeta(meta);
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
