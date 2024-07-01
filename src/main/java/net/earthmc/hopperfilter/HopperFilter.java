package net.earthmc.hopperfilter;

import net.earthmc.hopperfilter.listener.InventoryMoveItemListener;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class HopperFilter extends JavaPlugin {

    @Override
    public void onEnable() {
        registerListeners(
                new InventoryMoveItemListener()
        );
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
