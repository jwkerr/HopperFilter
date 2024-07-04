package net.earthmc.hopperfilter.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.earthmc.hopperfilter.HopperFilter;
import net.earthmc.hopperfilter.object.HopperRenameInteraction;
import net.earthmc.hopperfilter.util.PatternUtil;
import net.kyori.adventure.text.Component;
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

public class HopperRenameListener implements Listener {

    private static final Map<Player, Hopper> HOPPER_INTERACTIONS_TYPING = new HashMap<>();
    private static final Map<Player, HopperRenameInteraction> HOPPER_INTERACTIONS_ITEM = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (!(clickedBlock.getState() instanceof final Hopper hopper)) return;

        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;

        final Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        if (player.getGameMode().equals(GameMode.CREATIVE)) event.setCancelled(true);

        final ItemStack item = event.getItem();
        if (item == null) {
            initiateHopperRename(player, hopper);
        } else {
            final BlockBreakEvent bbe = new BlockBreakEvent(hopper.getBlock(), player);
            if (!bbe.callEvent()) return;

            String key = item.getType().getKey().getKey();

            HopperRenameInteraction hri = HOPPER_INTERACTIONS_ITEM.get(player);
            if (hri == null) {
                List<String> items = new ArrayList<>(List.of(key));
                HOPPER_INTERACTIONS_ITEM.put(player, new HopperRenameInteraction(hopper, items));
            } else {
                hri.getItems().add(key);
            }
        }
    }

    @EventHandler
    public void cancelTypingInteractionOnMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final Hopper hopper = HOPPER_INTERACTIONS_TYPING.get(player);
        if (hopper == null) return;

        final Location hopperLocation = hopper.getLocation();
        final double distanceSquared = event.getTo().distanceSquared(hopperLocation);

        if (distanceSquared > 5 * 5) {
            HOPPER_INTERACTIONS_TYPING.remove(player);

            final Random random = new Random();
            player.playSound(hopperLocation, Sound.BLOCK_ANVIL_LAND, 0.3F, random.nextFloat(1.25F, 1.5F));
        }
    }

    @EventHandler
    public void cancelItemInteractionOnMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final HopperRenameInteraction hri = HOPPER_INTERACTIONS_ITEM.get(player);
        if (hri == null) return;

        final Location hopperLocation = hri.getHopper().getLocation();
        final double distanceSquared = event.getTo().distanceSquared(hopperLocation);

        if (distanceSquared > 5 * 5) {
            HOPPER_INTERACTIONS_ITEM.remove(player);

            final Random random = new Random();
            player.playSound(hopperLocation, Sound.BLOCK_ANVIL_LAND, 0.3F, random.nextFloat(1.25F, 1.5F));
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(final PlayerToggleSneakEvent event) {
        if (event.isSneaking()) return;
        final Player player = event.getPlayer();

        final HopperRenameInteraction hri = HOPPER_INTERACTIONS_ITEM.remove(player);
        if (hri != null) {
            final Hopper hopper = hri.getHopper();
            renameHopper(player, hopper, String.join(",", hri.getItems()));
        }
    }

    @EventHandler
    public void onAsyncChat(final AsyncChatEvent event) {
        final Player player = event.getPlayer();
        final Hopper hopper = HOPPER_INTERACTIONS_TYPING.get(player);
        if (hopper == null) return;

        event.setCancelled(true);

        final String originalMessage = PatternUtil.serialiseComponent(event.originalMessage());
        renameHopper(player, hopper, originalMessage);

        HOPPER_INTERACTIONS_TYPING.remove(player);
    }

    private void initiateHopperRename(final Player player, final Hopper hopper) {
        final BlockBreakEvent bbe = new BlockBreakEvent(hopper.getBlock(), player);
        if (!bbe.callEvent()) return;

        HOPPER_INTERACTIONS_TYPING.put(player, hopper);

        final Random random = new Random();
        player.playSound(hopper.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1F, random.nextFloat(0.55F, 1.25F));
    }

    private void renameHopper(final Player player, final Hopper hopper, final String name) {
        final Component component = name.equals("null") ? null : Component.text(name);
        hopper.customName(component);

        final HopperFilter instance = HopperFilter.getInstance();
        instance.getServer().getRegionScheduler().run(instance, hopper.getLocation(), task -> hopper.update());

        final Random random = new Random();
        player.playSound(hopper.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.75F, random.nextFloat(1.25F, 1.5F));
    }
}
