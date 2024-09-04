package net.earthmc.hopperfilter.manager;

import net.earthmc.hopperfilter.HopperFilter;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final HopperFilter instance;
    private FileConfiguration config;

    public ConfigManager(HopperFilter instance) {
        this.instance = instance;

        loadConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void loadConfig() {
        config = instance.getConfig();

        addValues();

        instance.saveConfig();
        instance.reloadConfig();
    }

    private void addValues() {
        config.addDefault("enable_simple_hopper_renaming", true);

        config.options().copyDefaults(true);
    }
}
