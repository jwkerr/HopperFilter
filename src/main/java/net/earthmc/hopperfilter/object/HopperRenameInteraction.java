package net.earthmc.hopperfilter.object;

import org.bukkit.block.Hopper;

import java.util.List;

public class HopperRenameInteraction {

    private Hopper hopper;
    private List<String> items;

    public HopperRenameInteraction(final Hopper hopper, final List<String> items) {
        this.hopper = hopper;
        this.items = items;
    }

    public void setHopper(Hopper hopper) {
        this.hopper = hopper;
    }

    public Hopper getHopper() {
        return hopper;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public List<String> getItems() {
        return items;
    }
}
