/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Matsubara
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.bitbylogic.apibylogic.util.inventory;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftConnection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * A utility class for update the inventory of a player.
 * This is useful to change the title of an inventory.
 */
@SuppressWarnings("ConstantConditions")
public final class InventoryUpdate {

    // Classes.
    private final static MinecraftClassHandle CRAFT_PLAYER;
    private final static MinecraftClassHandle CHAT_MESSAGE;
    private final static MinecraftClassHandle PACKET_PLAY_OUT_OPEN_WINDOW;
    private final static MinecraftClassHandle I_CHAT_BASE_COMPONENT;
    private final static MinecraftClassHandle CONTAINER;
    private final static MinecraftClassHandle CONTAINERS;
    private final static MinecraftClassHandle ENTITY_PLAYER;
    private final static MinecraftClassHandle I_CHAT_MUTABLE_COMPONENT;

    // Methods.
    private final static MethodHandle getHandle;
    private final static MethodHandle getBukkitView;
    private final static MethodHandle literal;

    // Constructors.
    private final static MethodHandle chatMessage;
    private final static MethodHandle packetPlayOutOpenWindow;

    // Fields.
    private final static MethodHandle activeContainer;
    private final static MethodHandle windowId;

    // Methods factory.
    private final static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private final static Set<String> UNOPENABLES = Sets.newHashSet("CRAFTING", "CREATIVE", "PLAYER");

    @Getter
    private final static HashMap<UUID, String> lastSentTitle = new HashMap<>();

    static {
        boolean supports19 = XReflection.supports(19);

        // Initialize classes.
        CRAFT_PLAYER = XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.CB, "entity")
                .named("CraftPlayer");
        CHAT_MESSAGE = XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "network.chat")
                .named("ChatMessage");
        PACKET_PLAY_OUT_OPEN_WINDOW = XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "network.protocol.game")
                .named("PacketPlayOutOpenWindow");
        I_CHAT_BASE_COMPONENT = XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "network.chat")
                .named("IChatBaseComponent");
        // Check if we use containers, otherwise, can throw errors on older versions.
        CONTAINERS = useContainers() ? XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "world.inventory")
                .named("Containers") : null;
        ENTITY_PLAYER = XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "server.level")
                .named("EntityPlayer");
        CONTAINER = XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "world.inventory")
                .named("Container");
        I_CHAT_MUTABLE_COMPONENT = supports19 ? XReflection.ofMinecraft()
                .inPackage(MinecraftPackage.NMS, "network.chat")
                .named("IChatMutableComponent") : null;

        // Initialize methods.
        getHandle = CRAFT_PLAYER.method().named("getHandle").returns(ENTITY_PLAYER).unreflect();
        getBukkitView = CONTAINER.method().named("getBukkitView").returns(InventoryView.class).unreflect();
        literal = supports19 ?
                I_CHAT_BASE_COMPONENT.method().named("getTitle", "b")
                        .returns(I_CHAT_MUTABLE_COMPONENT).unreflect() : null;

        // Initialize constructors.
        chatMessage = supports19 ? null : CHAT_MESSAGE.constructor(String.class).unreflect();
        packetPlayOutOpenWindow =
                (useContainers()) ?
                        PACKET_PLAY_OUT_OPEN_WINDOW
                                .constructor(int.class, CONTAINERS.unreflect(), I_CHAT_BASE_COMPONENT.unreflect())
                                .unreflect() :
                        // Older versions use String instead of Containers, and require an int for the inventory size.
                        PACKET_PLAY_OUT_OPEN_WINDOW
                                .constructor(int.class, String.class, I_CHAT_BASE_COMPONENT.unreflect(), int.class)
                                .unreflect();

        // Initialize fields.
        activeContainer = ENTITY_PLAYER.field()
                .named("containerMenu", "activeContainer", "bV", "bW", "bU", "bP", "cd")
                .returns(CONTAINER).getter().unreflect();
        windowId = CONTAINER.field().named("windowId", "j", "containerId").returns(int.class).getter().unreflect();
    }

    /**
     * Update the player inventory, so you can change the title.
     *
     * @param player   whose inventory will be updated.
     * @param newTitle the new title for the inventory.
     */

    public static void updateInventory(JavaPlugin plugin, Player player, String newTitle) {
        Preconditions.checkArgument(player != null, "Cannot update inventory to null player.");

        if (lastSentTitle.containsKey(player.getUniqueId()) && lastSentTitle.get(player.getUniqueId()).equalsIgnoreCase(newTitle)) {
            return;
        }

        try {
            if (newTitle.length() > 32) {
                newTitle = newTitle.substring(0, 32);
            }

            if (XReflection.supports(20)) {
                InventoryView open = player.getOpenInventory();
                if (UNOPENABLES.contains(open.getType().name())) {
                    return;
                }
                Method method = open.getClass().getMethod("setTitle", String.class);
                if (!method.canAccess(open)) {
                    method.setAccessible(true);
                }
                method.invoke(open, newTitle);
                return;
            }

            // Get EntityPlayer from CraftPlayer.
            Object craftPlayer = CRAFT_PLAYER.unreflect().cast(player);
            Object entityPlayer = getHandle.invoke(craftPlayer);

            if (newTitle != null && newTitle.length() > 32) {
                newTitle = newTitle.substring(0, 32);
            } else if (newTitle == null) newTitle = "";

            // Create new title.
            Object title;
            if (XReflection.supports(19)) {
                title = literal.invoke(newTitle);
            } else {
                title = chatMessage.invoke(newTitle);
            }

            // Get activeContainer from EntityPlayer.
            Object activeContainer = InventoryUpdate.activeContainer.invoke(entityPlayer);

            // Get windowId from activeContainer.
            Integer windowId = (Integer) InventoryUpdate.windowId.invoke(activeContainer);

            // Get InventoryView from activeContainer.
            Object bukkitView = getBukkitView.invoke(activeContainer);
            if (!(bukkitView instanceof InventoryView)) return;

            InventoryView view = (InventoryView) bukkitView;
            InventoryType type = view.getTopInventory().getType();

            // Workbenchs and anvils can change their title since 1.14.
            if ((type == InventoryType.WORKBENCH || type == InventoryType.ANVIL) && !useContainers()) return;

            // You can't reopen crafting, creative and player inventory.
            if (UNOPENABLES.contains(type.name())) return;

            int size = view.getTopInventory().getSize();

            // Get container, check is not null.
            Containers container = Containers.getType(type, size);
            if (container == null) return;

            // If the container was added in a newer version than the current, return.
            if (container.getContainerVersion() > XReflection.MINOR_NUMBER && useContainers()) {
                Bukkit.getLogger().warning(String.format(
                        "[%s] This container doesn't work on your current version.",
                        plugin.getDescription().getName()));
                return;
            }

            Object object;
            // Dispensers and droppers use the same container, but in previous versions, use a diferrent minecraft name.
            if (!useContainers() && container == Containers.GENERIC_3X3) {
                object = "minecraft:" + type.name().toLowerCase();
            } else {
                object = container.getObject();
            }

            // Create packet.
            Object packet =
                    (useContainers()) ?
                            packetPlayOutOpenWindow.invoke(windowId, object, title) :
                            packetPlayOutOpenWindow.invoke(windowId, object, title, size);

            // Send packet sync.
            MinecraftConnection.sendPacket(player, packet);

            // Update inventory.
            player.updateInventory();
            lastSentTitle.put(player.getUniqueId(), newTitle);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static MethodHandle getField(Class<?> refc, Class<?> instc, String name, String... extraNames) {
        MethodHandle handle = getFieldHandle(refc, instc, name);
        if (handle != null) return handle;

        if (extraNames != null && extraNames.length > 0) {
            if (extraNames.length == 1) return getField(refc, instc, extraNames[0]);
            return getField(refc, instc, extraNames[0], removeFirst(extraNames));
        }

        return null;
    }

    private static String[] removeFirst(String[] array) {
        int length = array.length;

        String[] result = new String[length - 1];
        System.arraycopy(array, 1, result, 0, length - 1);

        return result;
    }

    private static MethodHandle getFieldHandle(Class<?> refc, Class<?> inscofc, String name) {
        try {
            for (Field field : refc.getFields()) {
                field.setAccessible(true);

                if (!field.getName().equalsIgnoreCase(name)) continue;

                if (field.getType().isInstance(inscofc) || field.getType().isAssignableFrom(inscofc)) {
                    return LOOKUP.unreflectGetter(field);
                }
            }
            return null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static MethodHandle getConstructor(Class<?> refc, Class<?>... types) {
        try {
            Constructor<?> constructor = refc.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return LOOKUP.unreflectConstructor(constructor);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static MethodHandle getMethod(Class<?> refc, String name, MethodType type) {
        return getMethod(refc, name, type, false);
    }

    private static MethodHandle getMethod(Class<?> refc, String name, MethodType type, boolean isStatic) {
        try {
            if (isStatic) return LOOKUP.findStatic(refc, name, type);
            return LOOKUP.findVirtual(refc, name, type);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * Containers were added in 1.14, a String were used in previous versions.
     *
     * @return whether to use containers.
     */
    private static boolean useContainers() {
        return XReflection.MINOR_NUMBER > 13;
    }

    /**
     * An enum class for the necessaries containers.
     */
    private enum Containers {
        GENERIC_9X1(14, "minecraft:chest", "CHEST"),
        GENERIC_9X2(14, "minecraft:chest", "CHEST"),
        GENERIC_9X3(14, "minecraft:chest", "CHEST", "ENDER_CHEST", "BARREL"),
        GENERIC_9X4(14, "minecraft:chest", "CHEST"),
        GENERIC_9X5(14, "minecraft:chest", "CHEST"),
        GENERIC_9X6(14, "minecraft:chest", "CHEST"),
        GENERIC_3X3(14, null, "DISPENSER", "DROPPER"),
        ANVIL(14, "minecraft:anvil", "ANVIL"),
        BEACON(14, "minecraft:beacon", "BEACON"),
        BREWING_STAND(14, "minecraft:brewing_stand", "BREWING"),
        ENCHANTMENT(14, "minecraft:enchanting_table", "ENCHANTING"),
        FURNACE(14, "minecraft:furnace", "FURNACE"),
        HOPPER(14, "minecraft:hopper", "HOPPER"),
        MERCHANT(14, "minecraft:villager", "MERCHANT"),
        // For an unknown reason, when updating a shulker box, the size of the inventory get a little bigger.
        SHULKER_BOX(14, "minecraft:blue_shulker_box", "SHULKER_BOX"),

        // Added in 1.14, so only works with containers.
        BLAST_FURNACE(14, null, "BLAST_FURNACE"),
        CRAFTING(14, null, "WORKBENCH"),
        GRINDSTONE(14, null, "GRINDSTONE"),
        LECTERN(14, null, "LECTERN"),
        LOOM(14, null, "LOOM"),
        SMOKER(14, null, "SMOKER"),
        // CARTOGRAPHY in 1.14, CARTOGRAPHY_TABLE in 1.15 & 1.16 (container), handle in getObject().
        CARTOGRAPHY_TABLE(14, null, "CARTOGRAPHY"),
        STONECUTTER(14, null, "STONECUTTER"),

        // Added in 1.14, functional since 1.16.
        SMITHING(16, null, "SMITHING");

        private final static char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        private final int containerVersion;
        private final String minecraftName;
        private final String[] inventoryTypesNames;

        Containers(int containerVersion, String minecraftName, String... inventoryTypesNames) {
            this.containerVersion = containerVersion;
            this.minecraftName = minecraftName;
            this.inventoryTypesNames = inventoryTypesNames;
        }

        /**
         * Get the container based on the current open inventory of the player.
         *
         * @param type type of inventory.
         * @return the container.
         */
        public static Containers getType(InventoryType type, int size) {
            if (type == InventoryType.CHEST) {
                return Containers.valueOf("GENERIC_9X" + size / 9);
            }
            for (Containers container : Containers.values()) {
                for (String bukkitName : container.getInventoryTypesNames()) {
                    if (bukkitName.equalsIgnoreCase(type.toString())) {
                        return container;
                    }
                }
            }
            return null;
        }

        /**
         * Get the object from the container enum.
         *
         * @return a Containers object if 1.14+, otherwise, a String.
         */
        public Object getObject() {
            try {
                if (!useContainers()) return getMinecraftName();
                int version = XReflection.MINOR_NUMBER;
                String name = (version == 14 && this == CARTOGRAPHY_TABLE) ? "CARTOGRAPHY" : name();
                // Since 1.17, containers go from "a" to "x".
                if (version > 16) name = String.valueOf(alphabet[ordinal()]);
                Field field = CONTAINERS.unreflect().getField(name);
                return field.get(null);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
            return null;
        }

        /**
         * Get the version in which the inventory container was added.
         *
         * @return the version.
         */
        public int getContainerVersion() {
            return containerVersion;
        }

        /**
         * Get the name of the inventory from Minecraft for older versions.
         *
         * @return name of the inventory.
         */
        public String getMinecraftName() {
            return minecraftName;
        }

        /**
         * Get inventory types names of the inventory.
         *
         * @return bukkit names.
         */
        public String[] getInventoryTypesNames() {
            return inventoryTypesNames;
        }
    }
}