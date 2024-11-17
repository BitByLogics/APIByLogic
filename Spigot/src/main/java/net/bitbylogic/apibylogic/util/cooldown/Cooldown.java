package net.bitbylogic.apibylogic.util.cooldown;

import lombok.Getter;

import java.util.Objects;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Cooldown cooldown = (Cooldown) object;
        return timeOfCreation == cooldown.timeOfCreation && duration == cooldown.duration && Objects.equals(identifier, cooldown.identifier) && Objects.equals(cooldownId, cooldown.cooldownId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, cooldownId, timeOfCreation, duration);
    }

}
