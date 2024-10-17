package net.bitbylogic.apibylogic.util.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class ItemSerializer {

    public static String itemStackToBase64(ItemStack item) {
        if (item == null) {
            return null;
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeObject(item.serialize());
            dataOutput.close();

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ItemStack itemStackFromBase64(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack itemStack = ItemStack.deserialize((Map<String, Object>) dataInput.readObject());

            dataInput.close();
            return itemStack;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        if (items == null || items.length == 0) {
            return "";
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);

            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return new ItemStack[]{};
        }

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

}
