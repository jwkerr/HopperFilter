package net.earthmc.hopperfilter.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class InventoryMoveItemListener implements Listener {

    private static final Map<String, String[]> CONTAINER_SPLITS = new HashMap<>();

    @EventHandler
    public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        final Inventory initiator = event.getInitiator();
        if (!initiator.getType().equals(InventoryType.HOPPER)) return;

        if (!(initiator.getHolder() instanceof final Hopper hopper)) return;

        final String hopperName = serialiseComponent(hopper.customName());
        if (hopperName == null) return;

        if (!canItemPassFilter(hopperName, event.getItem())) event.setCancelled(true);
    }

    private boolean canItemPassFilter(final String containerName, final ItemStack item) {
        String[] split = CONTAINER_SPLITS.getOrDefault(containerName, null);
        if (split == null) { // Cache splits for containers of the same name (untested performance change)
            split = containerName.split(",");
            CONTAINER_SPLITS.put(containerName, split);
        }

        final String itemName = item.getType().toString().toLowerCase();

        for (final String string : split) {
            final String pattern = string.toLowerCase();
            final int length = pattern.length();

            if (pattern.startsWith("*") && pattern.endsWith("*")) { // Contains specified pattern
                if (itemName.contains(pattern.substring(1, length - 1))) return true;
            } else if (pattern.startsWith("*")) { // Starts with specified pattern
                if (itemName.startsWith(pattern.substring(1))) return true;
            } else if (pattern.endsWith("*")) { // Ends with specified pattern
                if (itemName.endsWith(pattern.substring(0, length - 1))) return true;
            }

            if (pattern.equals(itemName)) return true;
        }

        return false;
    }

    private @Nullable String serialiseComponent(final Component component) {
        if (component == null) return null;
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
