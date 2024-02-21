package net.justugh.japi.util.cooldown;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Cooldown {

    private final UUID identifier;
    private final String cooldownId;
    private final long timeOfCreation;
    private final long duration;

    public Cooldown(UUID identifier, String cooldownId, long duration) {
        this.identifier = identifier;
        this.cooldownId = cooldownId;
        this.timeOfCreation = System.currentTimeMillis();
        this.duration = duration;
    }

    public boolean isActive() {
        return System.currentTimeMillis() < timeOfCreation + duration;
    }

    public long getTimeUntilExpired() {
        return isActive() ? (timeOfCreation + duration) - System.currentTimeMillis() : -1;
    }

}
