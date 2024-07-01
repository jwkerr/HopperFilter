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

public class InventoryMoveItemListener implements Listener {

    @EventHandler
    public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        final Inventory destination = event.getDestination();
        if (!destination.getType().equals(InventoryType.HOPPER)) return;

        if (!(destination.getHolder(false) instanceof final Hopper hopper)) return;

        final String hopperName = serialiseComponent(hopper.customName());
        if (hopperName == null) return;

        if (!canItemPassHopper(hopperName, event.getItem())) event.setCancelled(true);
    }

    private boolean canItemPassHopper(final String hopperName, final ItemStack item) {
        nextPatternGroup: for (final String patternGroup : hopperName.split(",")) {
            for (final String patternGroupString : patternGroup.split("&")) {
                final String pattern = patternGroupString.toLowerCase().strip();
                if (!canItemPassPattern(pattern, item)) continue nextPatternGroup;
            }
            return true;
        }

        return false;
    }

    private boolean canItemPassPattern(final String pattern, final ItemStack item) {
        final String itemName = item.getType().getKey().getKey();
        final int endIndex = pattern.length() - 1;

        if (pattern.equals(itemName)) return true;

        if (pattern.startsWith("*")) { // Contains specified pattern
            return itemName.contains(pattern.substring(1, endIndex));
        } else if (pattern.startsWith("^")) { // Starts with specified pattern
            return itemName.startsWith(pattern.substring(1));
        } else if (pattern.startsWith("$")) { // Ends with specified pattern
            return itemName.endsWith(pattern.substring(0, endIndex));
        }

        return false;
    }

    private @Nullable String serialiseComponent(final Component component) {
        if (component == null) return null;
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
