package net.earthmc.hopperfilter;

import net.earthmc.hopperfilter.listener.InventoryMoveItemListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HopperFilter extends JavaPlugin {

    @Override
    public void onEnable() {
        registerListeners();
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new InventoryMoveItemListener(), this);
    }
}
