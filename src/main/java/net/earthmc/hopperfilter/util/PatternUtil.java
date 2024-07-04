package net.earthmc.hopperfilter.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.Nullable;

public class PatternUtil {

    public static @Nullable String serialiseComponent(final Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static <T extends Keyed> @Nullable Keyed getKeyedFromString(String string, Registry<T> registry) {
        final NamespacedKey key = NamespacedKey.minecraft(string);

        return registry.get(key);
    }

    public static @Nullable Integer getIntegerFromString(String string) {
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
