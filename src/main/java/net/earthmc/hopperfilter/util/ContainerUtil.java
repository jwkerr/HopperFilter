package net.earthmc.hopperfilter.util;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ContainerUtil {

    public static boolean canHopperInventoryFitItemStack(Inventory inventory, ItemStack item) {
        Inventory clone = Bukkit.createInventory(null, InventoryType.HOPPER);
        clone.setContents(inventory.getContents());

        HashMap<Integer, ItemStack> remaining = clone.addItem(item);
        return remaining.isEmpty();
    }
}
