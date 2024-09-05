package net.earthmc.hopperfilter.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.earthmc.hopperfilter.HopperFilter;
import net.earthmc.hopperfilter.object.HopperRenameInteraction;
import net.earthmc.hopperfilter.util.PatternUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HopperRenameListener implements Listener {

    private static final Map<Player, Hopper> HOPPER_INTERACTIONS_TYPING = new ConcurrentHashMap<>();
    private static final Map<Player, HopperRenameInteraction> HOPPER_INTERACTIONS_ITEM = new ConcurrentHashMap<>();
    private static final Map<Player, Hopper> PREVIOUS_HOPPERS = new ConcurrentHashMap<>();
    private static final Map<Player, Integer> NUM_CONSECUTIVE_HOPPER_INTERACTIONS = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (!HopperFilter.getConfigManager().getConfig().getBoolean("enable_simple_hopper_renaming")) return;

        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (!(clickedBlock.getState() instanceof final Hopper hopper)) return;

        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;

        final Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        if (player.getGameMode().equals(GameMode.CREATIVE)) event.setCancelled(true);

        final BlockBreakEvent bbe = new BlockBreakEvent(hopper.getBlock(), player);
        if (!bbe.callEvent()) return;

        playSoundAtLocation(hopper.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1F, 0.55F, 1.25F);

        final ItemStack item = event.getItem();

        if (item == null) {
            initiateHopperRename(player, hopper);

            handleConsecutiveHopperInteractions(player, hopper);
            return;
        }

        final String key = item.getType().getKey().getKey();
        final HopperRenameInteraction hri = HOPPER_INTERACTIONS_ITEM.get(player);

        if (hri == null) {
            final List<String> items = new ArrayList<>(List.of(key));
            HOPPER_INTERACTIONS_ITEM.put(player, new HopperRenameInteraction(hopper, items));
            return;
        }

        if (!hri.getHopper().equals(hopper)) {
            hri.setHopper(hopper);
            hri.setItems(new ArrayList<>(List.of(key)));
            return;
        }

        final List<String> items = hri.getItems();
        if (!items.contains(key)) items.add(key);
    }

    private void handleConsecutiveHopperInteractions(final Player player, final Hopper hopper) {
        int numConsecutive = NUM_CONSECUTIVE_HOPPER_INTERACTIONS.computeIfAbsent(player, n -> 0);

        Hopper previousHopper = PREVIOUS_HOPPERS.putIfAbsent(player, hopper);

        if (previousHopper == null || !previousHopper.equals(hopper)) {
            PREVIOUS_HOPPERS.put(player, hopper);

            numConsecutive = 1;
            NUM_CONSECUTIVE_HOPPER_INTERACTIONS.put(player, numConsecutive);
            return;
        }

        numConsecutive += 1;
        if (numConsecutive < 3) {
            NUM_CONSECUTIVE_HOPPER_INTERACTIONS.put(player, numConsecutive);
            return;
        }

        sendCopyableHopperName(hopper, player);
        HOPPER_INTERACTIONS_TYPING.remove(player);
        NUM_CONSECUTIVE_HOPPER_INTERACTIONS.put(player, 0);
    }

    @EventHandler
    public void cancelTypingInteractionOnMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final Hopper hopper = HOPPER_INTERACTIONS_TYPING.get(player);
        if (hopper == null) return;

        final Location hopperLocation = hopper.getLocation();
        final double distanceSquared = event.getTo().distanceSquared(hopperLocation);

        if (distanceSquared < 5 * 5) return;

        HOPPER_INTERACTIONS_TYPING.remove(player);

        playSoundAtLocation(hopperLocation, Sound.BLOCK_ANVIL_LAND, 0.3F, 1.25F, 1.5F);
    }

    @EventHandler
    public void cancelItemInteractionOnMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final HopperRenameInteraction hri = HOPPER_INTERACTIONS_ITEM.get(player);
        if (hri == null) return;

        final Location hopperLocation = hri.getHopper().getLocation();
        final double distanceSquared = event.getTo().distanceSquared(hopperLocation);

        if (distanceSquared < 5 * 5) return;

        HOPPER_INTERACTIONS_ITEM.remove(player);

        playSoundAtLocation(hopperLocation, Sound.BLOCK_ANVIL_LAND, 0.3F, 1.25F, 1.5F);
    }

    @EventHandler
    public void onPlayerToggleSneak(final PlayerToggleSneakEvent event) {
        if (event.isSneaking()) return;
        final Player player = event.getPlayer();

        final HopperRenameInteraction hri = HOPPER_INTERACTIONS_ITEM.remove(player);
        if (hri != null) {
            final Hopper hopper = hri.getHopper();
            renameHopper(hopper, String.join(",", hri.getItems()));
        }
    }

    @EventHandler
    public void onAsyncChat(final AsyncChatEvent event) {
        final Player player = event.getPlayer();
        final Hopper hopper = HOPPER_INTERACTIONS_TYPING.get(player);
        if (hopper == null) return;

        event.setCancelled(true);

        final String originalMessage = PatternUtil.serialiseComponent(event.originalMessage());
        renameHopper(hopper, originalMessage);

        HOPPER_INTERACTIONS_TYPING.remove(player);
    }

    private void initiateHopperRename(final Player player, final Hopper hopper) {
        final BlockBreakEvent bbe = new BlockBreakEvent(hopper.getBlock(), player);
        if (!bbe.callEvent()) return;

        HOPPER_INTERACTIONS_TYPING.put(player, hopper);
    }

    private void sendCopyableHopperName(final Hopper hopper, final Player player) {
        TextComponent.Builder builder = Component.text();

        builder.append(Component.text("[", NamedTextColor.DARK_GRAY));

        Component customName = hopper.customName();
        String stringName = customName == null ? "Item Hopper" : PlainTextComponentSerializer.plainText().serialize(customName);
        Component name = Component.text(stringName, NamedTextColor.GRAY);
        builder.append(name);

        builder.append(Component.text("]", NamedTextColor.DARK_GRAY));

        builder.hoverEvent(Component.text("Click to copy!", NamedTextColor.GRAY));
        builder.clickEvent(ClickEvent.copyToClipboard(stringName));

        player.sendMessage(builder.build());
    }

    private void renameHopper(final Hopper hopper, final String name) {
        final Component component = name.equals("null") || name.equals("remove") ? null : Component.text(name);
        hopper.customName(component);
        hopper.setTransferCooldown(20); // Simple fix to prevent dupes

        final HopperFilter instance = HopperFilter.getInstance();
        instance.getServer().getRegionScheduler().run(instance, hopper.getLocation(), task -> hopper.update());

        playSoundAtLocation(hopper.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.75F, 1.25F, 1.5F);
    }

    private void playSoundAtLocation(Location location, Sound sound, float volume, float origin, float bound) {
        final HopperFilter instance = HopperFilter.getInstance();
        instance.getServer().getRegionScheduler().run(instance, location, task -> {
            Random random = new Random();
            location.getWorld().playSound(location, sound, volume, random.nextFloat(origin, bound));
        });
    }
}
