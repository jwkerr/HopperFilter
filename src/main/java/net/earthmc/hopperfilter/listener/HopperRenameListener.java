package net.earthmc.hopperfilter.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.earthmc.hopperfilter.HopperFilter;
import net.kyori.adventure.text.Component;
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
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HopperRenameListener implements Listener {

    private static final Map<Player, Hopper> HOPPER_RENAME_INTERACTIONS = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (!(clickedBlock.getState() instanceof final Hopper hopper)) return;

        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;

        final Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        final ItemStack item = event.getItem();
        if (item == null) {
            initiateHopperRename(player, hopper);
        } else {
            final BlockBreakEvent bbe = new BlockBreakEvent(hopper.getBlock(), player);
            if (!bbe.callEvent()) return;

            renameHopper(player, hopper, item.getType().getKey().getKey());
        }
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final Hopper hopper = HOPPER_RENAME_INTERACTIONS.get(player);
        if (hopper == null) return;

        final Location hopperLocation = hopper.getLocation();
        final double distanceSquared = event.getTo().distanceSquared(hopperLocation);

        if (distanceSquared > 5 * 5) {
            HOPPER_RENAME_INTERACTIONS.remove(player);

            Random random = new Random();
            player.playSound(hopperLocation, Sound.BLOCK_ANVIL_LAND, 0.3F, random.nextFloat(1.25F, 1.5F));
        }
    }

    @EventHandler
    public void onAsyncChat(final AsyncChatEvent event) {
        final Player player = event.getPlayer();
        final Hopper hopper = HOPPER_RENAME_INTERACTIONS.get(player);
        if (hopper == null) return;

        event.setCancelled(true);

        final String originalMessage = HopperFilter.serialiseComponent(event.originalMessage());
        renameHopper(player, hopper, originalMessage);

        HOPPER_RENAME_INTERACTIONS.remove(player);
    }

    private void initiateHopperRename(final Player player, final Hopper hopper) {
        final BlockBreakEvent bbe = new BlockBreakEvent(hopper.getBlock(), player);
        if (!bbe.callEvent()) return;

        HOPPER_RENAME_INTERACTIONS.put(player, hopper);

        final Random random = new Random();
        player.playSound(hopper.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.1F, random.nextFloat(0.55F, 1.25F));
    }

    private void renameHopper(final Player player, final Hopper hopper, final String name) {
        final Component component = name.equals("null") ? null : Component.text(name);
        hopper.customName(component);

        final HopperFilter instance = HopperFilter.getInstance();
        instance.getServer().getRegionScheduler().run(instance, hopper.getLocation(), task -> {
            hopper.update();
        });

        final Random random = new Random();
        player.playSound(hopper.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.75F, random.nextFloat(1.25F, 1.5F));
    }
}
