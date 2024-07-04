package net.earthmc.hopperfilter.listener;

import net.earthmc.hopperfilter.util.PatternUtil;
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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class InventoryActionListener implements Listener {

    @EventHandler
    public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        final Inventory destination = event.getDestination();
        if (!destination.getType().equals(InventoryType.HOPPER)) return;

        final ItemStack item = event.getItem();
        final InventoryHolder holder = destination.getHolder(false);

        String hopperName;
        if (holder instanceof final Hopper hopper) {
            hopperName = PatternUtil.serialiseComponent(hopper.customName());
        } else if (holder instanceof final HopperMinecart hopperMinecart) {
            hopperName = PatternUtil.serialiseComponent(hopperMinecart.customName());
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
            hopperName = PatternUtil.serialiseComponent(hopper.customName());
        } else if (holder instanceof final HopperMinecart hopperMinecart) {
            hopperName = PatternUtil.serialiseComponent(hopperMinecart.customName());
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

        final Hopper facingHopper = (Hopper) facingBlock.getState(false);

        Hopper otherHopper;
        if (facingHopper.equals(destinationHopper)) { // We need to check the hopper below
            facingBlock = sourceHopper.getBlock().getRelative(BlockFace.DOWN);
            if (!facingBlock.getType().equals(Material.HOPPER)) return false; // We can safely say the original is the only hopper

            otherHopper = (Hopper) facingBlock.getState(false);
        } else { // We need to check this hopper
            otherHopper = facingHopper;
        }

        final String hopperName = PatternUtil.serialiseComponent(otherHopper.customName());
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
            case '~' -> { // Item has specified potion effect
                final Material material = item.getType();
                if (!(material.equals(Material.POTION) || material.equals(Material.SPLASH_POTION) || material.equals(Material.LINGERING_POTION))) yield false;

                final String[] split = string.split("_");

                PotionEffectType type = (PotionEffectType) PatternUtil.getKeyedFromString(split[0], Registry.POTION_EFFECT_TYPE);

                Integer userLevel = PatternUtil.getIntegerFromString(split[split.length - 1]);

                final PotionMeta meta = (PotionMeta) item.getItemMeta();
                final List<PotionEffect> effects = meta.getBasePotionType().getPotionEffects();
                if (userLevel == null) {
                    for (PotionEffect effect : effects) {
                        if (effect.getType().equals(type)) yield true;
                    }
                } else {
                    for (PotionEffect effect : effects) {
                        if (effect.getType().equals(type) && effect.getAmplifier() + 1 == userLevel) yield true;
                    }
                }
                yield false;
            }
            case '+' -> { // Item has specified enchantment
                Map<Enchantment, Integer> enchantments;
                if (item.getType().equals(Material.ENCHANTED_BOOK)) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    enchantments = meta.getStoredEnchants();
                } else {
                    enchantments = item.getEnchantments();
                }

                final String[] split = string.split("_");

                final Enchantment enchantment = (Enchantment) PatternUtil.getKeyedFromString(split[0], Registry.ENCHANTMENT);
                if (enchantment == null) yield false;

                Integer userLevel = PatternUtil.getIntegerFromString(split[split.length - 1]);

                final Integer enchantmentLevel = enchantments.get(enchantment);
                if (userLevel == null) {
                    yield enchantmentLevel != null;
                } else {
                    yield enchantmentLevel != null && (enchantmentLevel).equals(userLevel);
                }
            }
            case '!' -> !canItemPassPattern(string, item); // NOT operator
            default -> false;
        };
    }
}
