package net.earthmc.hopperfilter.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
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

        if (!canItemPassFilter(hopperName, event.getItem())) event.setCancelled(true);
    }

    private boolean canItemPassFilter(final String containerName, final ItemStack item) {
        final String[] split = containerName.split(",");
        final String itemName = item.getType().getKey().getKey();

        for (final String string : split) {
            final String pattern = string.toLowerCase().strip();
            final int length = pattern.length();

            if(pattern.startsWith("#")) { // Filter by item tags, such as `villager_plantable_seeds`
                String key = pattern.substring(1);
                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(key), Material.class);
                if(tag != null) {
                    if(tag.isTagged(item.getType())) return true;
                }
            } else if (pattern.startsWith("*") && pattern.endsWith("*")) { // Contains specified pattern
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
