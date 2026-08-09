package org.bcnlab.beaconlabslobby.managers;
import org.bcnlab.beaconlabslobby.utils.SoundUtil;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import java.io.File;
import java.util.UUID;

public class NPCManager implements Listener {

    private final BeaconLabsLobby plugin;
    private File npcFile;
    private YamlConfiguration npcConfig;

    public NPCManager(BeaconLabsLobby plugin) {
        this.plugin = plugin;
        this.npcFile = new File(plugin.getDataFolder(), "npc.yml");
        if (!this.npcFile.exists()) {
            plugin.saveResource("npc.yml", false);
        }
        this.npcConfig = YamlConfiguration.loadConfiguration(this.npcFile);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reloadNpcConfig() {
        this.npcConfig = YamlConfiguration.loadConfiguration(this.npcFile);
        if (npcConfig.contains("npcs")) {
            for (String name : npcConfig.getConfigurationSection("npcs").getKeys(false)) {
                Location loc = npcConfig.getLocation("npcs." + name + ".location");
                if (loc == null) continue;
                
                String displayName = npcConfig.getString("npcs." + name + ".displayName", name);
                String skinName = npcConfig.getString("npcs." + name + ".skin");
                
                Npc existing = FancyNpcsPlugin.get().getNpcManager().getNpc(name);
                if (existing != null) {
                    existing.getData().setLocation(loc);
                    existing.getData().setDisplayName(displayName);
                    if (skinName != null) {
                        de.oliver.fancynpcs.api.skins.SkinData skinData = FancyNpcsPlugin.get().getSkinManager().getByUsername(skinName, null);
                        if (skinData != null) existing.getData().setSkinData(skinData);
                    }
                    existing.removeForAll();
                    existing.spawnForAll();
                } else {
                    NpcData data = new NpcData(name, UUID.randomUUID(), loc);
                    data.setDisplayName(displayName);
                    Npc newNpc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
                    if (skinName != null) {
                        de.oliver.fancynpcs.api.skins.SkinData skinData = FancyNpcsPlugin.get().getSkinManager().getByUsername(skinName, null);
                        if (skinData != null) data.setSkinData(skinData);
                    }
                    FancyNpcsPlugin.get().getNpcManager().registerNpc(newNpc);
                    newNpc.create();
                    newNpc.spawnForAll();
                }
            }
        }
    }

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        Npc npc = event.getNpc();
        Player player = event.getPlayer();
        String npcName = npc.getData().getName();

        SoundUtil.playSound(player, org.bukkit.Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
        npc.lookAt(player, player.getLocation());

        if (npcConfig.contains("npcs." + npcName)) {
            String action = npcConfig.getString("npcs." + npcName + ".action");
            String value = npcConfig.getString("npcs." + npcName + ".value");

            if ("COMMAND".equalsIgnoreCase(action) && value != null && !value.isBlank()) {
                player.chat("/" + value);
            }
        }
    }
    
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        for (Npc npc : FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
            if (npc.getData().getLocation().getWorld().equals(player.getWorld())) {
                if (npc.getData().getLocation().distanceSquared(player.getLocation()) < 25) {
                    try {
                        boolean found = false;
                        for (java.lang.reflect.Method m : npc.getData().getClass().getMethods()) {
                            if (m.getName().equalsIgnoreCase("setSneaking") && m.getParameterCount() == 1) {
                                m.invoke(npc.getData(), event.isSneaking());
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            for (java.lang.reflect.Method m : npc.getClass().getMethods()) {
                                if (m.getName().equalsIgnoreCase("setSneaking") && m.getParameterCount() == 1) {
                                    m.invoke(npc, event.isSneaking());
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {}
                    try {
                        npc.updateForAll();
                    } catch (Throwable t) {
                        // fallback if updateForAll doesn't exist
                        npc.removeForAll();
                        npc.spawnForAll();
                    }
                }
            }
        }
    }

    public void createNpc(Player creator, String name, String displayName) {
        Location loc = creator.getLocation();
        NpcData data = new NpcData(name, UUID.randomUUID(), loc);
        Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
        
        data.setDisplayName(displayName);
        FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
        npc.create();
        npc.spawnForAll();
        
        npcConfig.set("npcs." + name + ".action", "COMMAND");
        npcConfig.set("npcs." + name + ".value", "say Hello");
        npcConfig.set("npcs." + name + ".location", loc);
        npcConfig.set("npcs." + name + ".displayName", displayName);
        try {
            npcConfig.save(npcFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save npc config: " + e.getMessage());
        }
    }
    
    public void deleteNpc(String name) {
        Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpc(name);
        if (npc != null) {
            npc.removeForAll();
            FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
            
            npcConfig.set("npcs." + name, null);
            try {
                npcConfig.save(npcFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save npc config: " + e.getMessage());
            }
        }
    }
    
    public void listNpcs(Player player) {
        player.sendMessage(net.kyori.adventure.text.Component.text("                                                  ").decorate(net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        plugin.sendMessage(player, "<gold>NPCs:</gold>");
        for (Npc npc : FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
            plugin.sendMessage(player, "<gray>- <gold>" + npc.getData().getName() + "</gold></gray>");
        }
        player.sendMessage(net.kyori.adventure.text.Component.text("                                                  ").decorate(net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH).color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }
    
    public void setSkin(String name, String skinName) {
        Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpc(name);
        if (npc != null) {
            de.oliver.fancynpcs.api.skins.SkinData skinData = FancyNpcsPlugin.get().getSkinManager().getByUsername(skinName, null);
            if (skinData != null) {
                npc.getData().setSkinData(skinData);
                npc.removeForAll();
                npc.spawnForAll();
                
                npcConfig.set("npcs." + name + ".skin", skinName);
                try {
                    npcConfig.save(npcFile);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save npc config: " + e.getMessage());
                }
            }
        }
    }
}
