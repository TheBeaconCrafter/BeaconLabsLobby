package org.bcnlab.beaconlabslobby.utils;

import org.bukkit.entity.Player;
import org.bukkit.Sound;

public class SoundUtil {
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player.hasMetadata("protocol_version")) {
            if (player.getMetadata("protocol_version").get(0).asInt() <= 47) {
                return;
            }
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
