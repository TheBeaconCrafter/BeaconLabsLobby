package org.bcnlab.beaconlabslobby.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BuildManager {

    private final Set<UUID> allowedBuilders = new HashSet<>();

    public void allowBuilding(UUID playerUUID) {
        allowedBuilders.add(playerUUID);
    }

    public void disallowBuilding(UUID playerUUID) {
        allowedBuilders.remove(playerUUID);
    }

    public boolean isAllowedToBuild(UUID playerUUID) {
        return allowedBuilders.contains(playerUUID);
    }

    public Set<UUID> getAllowedBuilders() {
        return new HashSet<>(allowedBuilders); // return a copy to prevent external modification
    }
}
