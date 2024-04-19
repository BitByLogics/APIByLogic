package net.bitbylogic.logicutils.util;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PersistentDataUtil {

    public static List<Object> getValues(PersistentDataContainer container, NamespacedKey key) {
        List<Object> values = new ArrayList<>();

        if(container.has(key, PersistentDataType.STRING)) {
            values.add(container.get(key, PersistentDataType.STRING));
        }

        if(container.has(key, PersistentDataType.BYTE)) {
            values.add(container.get(key, PersistentDataType.BYTE));
        }

        if(container.has(key, PersistentDataType.SHORT)) {
            values.add(container.get(key, PersistentDataType.SHORT));
        }

        if(container.has(key, PersistentDataType.INTEGER)) {
            values.add(container.get(key, PersistentDataType.INTEGER));
        }

        if(container.has(key, PersistentDataType.LONG)) {
            values.add(container.get(key, PersistentDataType.LONG));
        }

        if(container.has(key, PersistentDataType.FLOAT)) {
            values.add(container.get(key, PersistentDataType.FLOAT));
        }

        if(container.has(key, PersistentDataType.DOUBLE)) {
            values.add(container.get(key, PersistentDataType.DOUBLE));
        }

        if(container.has(key, PersistentDataType.BYTE_ARRAY)) {
            values.add(container.get(key, PersistentDataType.BYTE_ARRAY));
        }

        if(container.has(key, PersistentDataType.INTEGER_ARRAY)) {
            values.add(container.get(key, PersistentDataType.INTEGER_ARRAY));
        }

        if(container.has(key, PersistentDataType.LONG_ARRAY)) {
            values.add(container.get(key, PersistentDataType.LONG_ARRAY));
        }

        if(container.has(key, PersistentDataType.TAG_CONTAINER_ARRAY)) {
            values.add("Tag Container Array");
        }

        if(container.has(key, PersistentDataType.TAG_CONTAINER)) {
            values.add("Tag Container");
        }

        return values;
    }

}
