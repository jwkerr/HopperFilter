package net.earthmc.hopperfilter.listener;

import net.earthmc.hopperfilter.util.ContainerUtil;
import net.earthmc.hopperfilter.util.PatternUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.tuple.Pair;
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

        if (!ContainerUtil.canHopperInventoryFitItemStack(otherHopper.getInventory(), item)) return false; // This hopper cannot fit the item, return false to avoid clogging

        final String hopperName = PatternUtil.serialiseComponent(otherHopper.customName());
        if (hopperName == null) return false;

        // Before this method is called we are certain the destinationHopper does not have a name
        // If the other hopper we found here can also pass this item through but does have a name we cancel this movement to prevent
        // items moving to unnamed hoppers when there is a hopper specifically filtering for this item
        return canItemPassHopper(hopperName, item);
    }

    private boolean canItemPassHopper(final String hopperName, final ItemStack item) {
        if (hopperName == null) return true;

        nextCondition: for (final String condition : hopperName.split(",")) {
            nextAnd: for (final String andString : condition.split("&")) {
                for (final String orString : andString.split("\\|")) {
                    final String pattern = orString.toLowerCase().strip();
                    if (canItemPassPattern(pattern, item)) continue nextAnd;
                }
                continue nextCondition;
            }
            return true;
        }

        return false;
    }

    private boolean canItemPassPattern(final String pattern, final ItemStack item) {
        final String itemName = item.getType().getKey().getKey();

        if (pattern.isEmpty() || pattern.equals(itemName))
            return true;

        final char prefix = pattern.charAt(0); // The character at the start of the pattern
        final String string = pattern.substring(1); // Anything after the prefix
        return switch (prefix) {
            case '!' -> !canItemPassPattern(string, item); // NOT operator
            case '*' -> itemName.contains(string); // Contains specified pattern
            case '^' -> itemName.startsWith(string); // Starts with specified pattern
            case '$' -> itemName.endsWith(string); // Ends with specified pattern
            case '#' -> { // Item has specified tag
                final NamespacedKey key = NamespacedKey.fromString(string);
                if (key == null)
                    yield false;

                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
                if (tag == null) tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);

                yield tag != null && tag.isTagged(item.getType());
            }
            case '~' -> doesItemHaveSpecifiedPotionEffect(item, string); // Item has specified potion effect
            case '+' -> doesItemHaveSpecifiedEnchantment(item, string); // Item has specified enchantment
            case '=' -> { // Item has specified name (including renames)
                String displayName = PlainTextComponentSerializer.plainText().serialize(item.displayName())
                        .toLowerCase()
                        .replaceAll(" ", "_");

                displayName = displayName.substring(1, displayName.length() - 1);

                yield displayName.equals(string);
            }
            default -> false;
        };
    }

    private boolean doesItemHaveSpecifiedPotionEffect(ItemStack item, String string) {
        final Material material = item.getType();
        if (!(material.equals(Material.POTION) || material.equals(Material.SPLASH_POTION) || material.equals(Material.LINGERING_POTION))) return false;

        final Pair<String, Integer> pair = PatternUtil.getNameLevelPairFromString(string);

        final PotionEffectType type = (PotionEffectType) PatternUtil.getKeyedFromString(pair.getLeft(), Registry.POTION_EFFECT_TYPE);
        if (type == null) return false;

        final Integer userLevel = pair.getRight();

        final PotionMeta meta = (PotionMeta) item.getItemMeta();
        final List<PotionEffect> effects = meta.getBasePotionType().getPotionEffects();
        if (userLevel == null) {
            for (PotionEffect effect : effects) {
                if (effect.getType().equals(type)) return true;
            }
            return meta.hasCustomEffect(type);
        } else {
            for (PotionEffect effect : effects) {
                if (effect.getType().equals(type) && effect.getAmplifier() + 1 == userLevel) return true;
            }
            for (PotionEffect effect : meta.getCustomEffects()) {
                if (effect.getType().equals(type) && effect.getAmplifier() + 1 == userLevel) return true;
            }
        }

        return false;
    }

    private boolean doesItemHaveSpecifiedEnchantment(ItemStack item, String string) {
        Map<Enchantment, Integer> enchantments;
        if (item.getType().equals(Material.ENCHANTED_BOOK)) {
            final EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            enchantments = meta.getStoredEnchants();
        } else {
            enchantments = item.getEnchantments();
        }

        final Pair<String, Integer> pair = PatternUtil.getNameLevelPairFromString(string);

        final Enchantment enchantment = (Enchantment) PatternUtil.getKeyedFromString(pair.getLeft(), Registry.ENCHANTMENT);
        if (enchantment == null) return false;

        final Integer userLevel = pair.getRight();

        final Integer enchantmentLevel = enchantments.get(enchantment);
        if (userLevel == null) {
            return enchantmentLevel != null;
        } else {
            return enchantmentLevel != null && (enchantmentLevel).equals(userLevel);
        }
    }
}
