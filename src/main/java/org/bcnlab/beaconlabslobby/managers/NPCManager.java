package org.bcnlab.beaconlabslobby.managers;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import org.bcnlab.beaconlabslobby.BeaconLabsLobby;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import java.io.File;

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
    }

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        Npc npc = event.getNpc();
        Player player = event.getPlayer();
        String npcName = npc.getData().getName();

        if (npcConfig.contains("npcs." + npcName)) {
            String action = npcConfig.getString("npcs." + npcName + ".action");
            String value = npcConfig.getString("npcs." + npcName + ".value");

            if ("COMMAND".equalsIgnoreCase(action) && value != null) {
                // Execute command for the player
                player.chat("/" + value);
            }
        }
    }

    public void createNpc(Player creator, String name, String displayName) {
        Location loc = creator.getLocation();
        
        NpcData data = new NpcData(name, creator.getUniqueId(), loc);
        Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
        
        data.setDisplayName(displayName);
        
        FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
        npc.create();
        npc.spawnForAll();
    }
    
    public void deleteNpc(String name) {
        Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpc(name);
        if (npc != null) {
            npc.removeForAll();
            FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
        }
    }
}
