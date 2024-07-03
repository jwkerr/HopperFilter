package net.earthmc.hopperfilter;

import net.earthmc.hopperfilter.listener.HopperRenameListener;
import net.earthmc.hopperfilter.listener.InventoryActionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class HopperFilter extends JavaPlugin {

    private static HopperFilter instance;

    @Override
    public void onEnable() {
        instance = this;

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

    public static @Nullable String serialiseComponent(final Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }
}
