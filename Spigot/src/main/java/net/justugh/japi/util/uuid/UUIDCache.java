package net.justugh.japi.util.uuid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UUIDCache {

    private HashMap<UUID, List<String>> cachedUUIDs = new HashMap<>();

    public Optional<UUID> getUUIDByName(String name) {
        Map.Entry<UUID, List<String>> uuidEntry = cachedUUIDs.entrySet().stream().filter(entry ->
                entry.getValue().stream().anyMatch(value -> value.equalsIgnoreCase(name))).findFirst().orElse(null);
        return uuidEntry == null ? Optional.empty() : Optional.of(uuidEntry.getKey());
    }

    public List<String> getNamesByUUID(UUID uuid) {
        return cachedUUIDs.getOrDefault(uuid, new ArrayList<>());
    }

}
