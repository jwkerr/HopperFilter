package net.earthmc.hopperfilter.object;

import org.bukkit.block.Hopper;

import java.util.List;

public class HopperRenameInteraction {

    private final Hopper hopper;
    private final List<String> items;

    public HopperRenameInteraction(Hopper hopper, List<String> items) {
        this.hopper = hopper;
        this.items = items;
    }

    public Hopper getHopper() {
        return hopper;
    }

    public List<String> getItems() {
        return items;
    }
}
