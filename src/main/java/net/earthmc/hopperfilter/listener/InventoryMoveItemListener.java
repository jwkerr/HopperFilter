package net.earthmc.hopperfilter.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class InventoryMoveItemListener implements Listener {

    @EventHandler
    public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        final Inventory destination = event.getDestination();
        if (!destination.getType().equals(InventoryType.HOPPER)) return;

        final ItemStack item = event.getItem();
        final InventoryHolder holder = destination.getHolder(false);

        String hopperName;
        if (holder instanceof final Hopper hopper) {
            hopperName = serialiseComponent(hopper.customName());
        } else if (holder instanceof final HopperMinecart hopperMinecart) {
            hopperName = serialiseComponent(hopperMinecart.customName());
        } else {
            return;
        }

        if (!canItemPassHopper(hopperName, item)) {
            event.setCancelled(true);
            return;
        }

        // Checks below only matter for hopper blocks
        if (!(holder instanceof Hopper hopper)) return;

        // If there was a filter on this hopper we don't need to check for a more suitable hopper since this one is specifically filtering for this item
        if (hopperName != null) return;

        final Inventory source = event.getSource();

        // If the item can pass, but there is a more suitable hopper with a filter we do this
        if (shouldCancelDueToMoreSuitableHopper(source, hopper, item)) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryPickupItem(final InventoryPickupItemEvent event) {
        final Inventory inventory = event.getInventory();
        if (!inventory.getType().equals(InventoryType.HOPPER)) return;

        final InventoryHolder holder = inventory.getHolder(false);

        String hopperName;
        if (holder instanceof final Hopper hopper) {
            hopperName = serialiseComponent(hopper.customName());
        } else if (holder instanceof final HopperMinecart hopperMinecart) {
            hopperName = serialiseComponent(hopperMinecart.customName());
        } else {
            return;
        }

        if (hopperName == null) return;

        if (!canItemPassHopper(hopperName, event.getItem().getItemStack())) event.setCancelled(true);
    }

    private boolean shouldCancelDueToMoreSuitableHopper(final Inventory source, final Hopper destinationHopper, final ItemStack item) {
        // If the source is not a hopper we don't care about it
        if (!(source.getHolder(false) instanceof final Hopper sourceHopper)) return false;

        final org.bukkit.block.data.type.Hopper initiatorHopperData = (org.bukkit.block.data.type.Hopper) sourceHopper.getBlockData();

        // If the hopper is facing down that means it isn't possible for another hopper to also take items out of this hopper
        final BlockFace facing = initiatorHopperData.getFacing();
        if (facing.equals(BlockFace.DOWN)) return false;

        Block facingBlock = sourceHopper.getBlock().getRelative(facing);

        // If the relative block is not a hopper we don't care about it as that means our original destination is the only possible destination
        if (!facingBlock.getType().equals(Material.HOPPER)) return false;

        final Hopper facingHopper = (Hopper) facingBlock.getState();

        Hopper otherHopper;
        if (facingHopper.equals(destinationHopper)) { // We need to check the hopper below
            facingBlock = sourceHopper.getBlock().getRelative(BlockFace.DOWN);
            if (!facingBlock.getType().equals(Material.HOPPER)) return false; // We can safely say the original is the only hopper

            otherHopper = (Hopper) facingBlock.getState();
        } else { // We need to check this hopper
            otherHopper = facingHopper;
        }

        final String hopperName = serialiseComponent(otherHopper.customName());
        if (hopperName == null) return false;

        // Before this method is called we are certain the destinationHopper does not have a name
        // If the other hopper we found here can also pass this item through but does have a name we cancel this movement to prevent
        // items moving to unnamed hoppers when there is a hopper specifically filtering for this item
        return canItemPassHopper(hopperName, item);
    }

    private boolean canItemPassHopper(final String hopperName, final ItemStack item) {
        if (hopperName == null) return true;

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

        if (pattern.equals(itemName)) return true;

        final char prefix = pattern.charAt(0); // The character at the start of the pattern
        final String string = pattern.substring(1); // Anything after the prefix
        return switch (prefix) {
            case '*' -> itemName.contains(string); // Contains specified pattern
            case '^' -> itemName.startsWith(string); // Starts with specified pattern
            case '$' -> itemName.endsWith(string); // Ends with specified pattern
            case '#' -> { // Item has specified tag
                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(string), Material.class);
                if (tag == null) tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(string), Material.class);

                yield tag != null && tag.isTagged(item.getType());
            }
            case '+' -> { // Item has specified enchantment
                Map<Enchantment, Integer> enchantments;
                if (item.getType().equals(Material.ENCHANTED_BOOK)) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    enchantments = meta.getStoredEnchants();
                } else {
                    enchantments = item.getEnchantments();
                }

                final NamespacedKey key = NamespacedKey.minecraft(string);
                final Enchantment enchantment = Registry.ENCHANTMENT.get(key);
                if (enchantment == null) yield false;

                yield (enchantments.getOrDefault(enchantment, null) != null);
            }
            case '!' -> !canItemPassPattern(string, item); // NOT operator
            default -> false;
        };
    }

    private @Nullable String serialiseComponent(final Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }
}
