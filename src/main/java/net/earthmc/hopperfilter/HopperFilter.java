package net.earthmc.hopperfilter;

import net.earthmc.hopperfilter.listener.HopperRenameListener;
import net.earthmc.hopperfilter.listener.InventoryActionListener;
import net.earthmc.hopperfilter.manager.ConfigManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class HopperFilter extends JavaPlugin {

    private static HopperFilter instance;
    private static ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);

        registerListeners(
                new HopperRenameListener(),
                new InventoryActionListener()
        );
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public static HopperFilter getInstance() {
        return instance;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }
}
