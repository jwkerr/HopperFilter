package net.earthmc.hopperfilter.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    public static Pair<String, Integer> getNameLevelPairFromString(String string) {
        final String[] split = string.split("_");
        if (split.length == 1) return Pair.of(split[0], null);

        final Integer integer = getIntegerFromString(split[split.length - 1]);
        if (integer == null) {
            return Pair.of(String.join("_", split), null);
        } else {
            final List<String> list = new ArrayList<>(List.of(split));
            list.remove(list.size() - 1);
            return Pair.of(String.join("_", list), integer);
        }
    }
}
