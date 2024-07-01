package net.earthmc.hopperfilter.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
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

    private boolean canItemPassPattern(String pattern, final ItemStack item) {
        final String itemName = item.getType().getKey().getKey();

        if (pattern.equals(itemName)) return true;

        final char prefix = pattern.charAt(0); // The character at the start of the pattern
        final String string = pattern.substring(1); // Anything after the prefix
        return switch (prefix) {
            case '*' -> itemName.contains(string); // Contains specified pattern
            case '^' -> itemName.startsWith(string); // Starts with specified pattern
            case '$' -> itemName.endsWith(string); // Ends with specified pattern
            case '#' -> { // Item has specified tag
                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(string), Material.class);
                yield tag != null && tag.isTagged(item.getType());
            }
            default -> false;
        };
    }

    private @Nullable String serialiseComponent(final Component component) {
        if (component == null) return null;
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
