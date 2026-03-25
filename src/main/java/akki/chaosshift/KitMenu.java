package akki.chaosshift;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class KitMenu {

    public static Inventory createMenu(Map<KitType, Integer> votes) {

        Inventory inv = Bukkit.createInventory(null, 9, "Select Kit");

        inv.setItem(2, createItem(Material.IRON_SWORD, "§cWarrior", votes.getOrDefault(KitType.WARRIOR, 0)));
        inv.setItem(4, createItem(Material.BOW, "§aArcher", votes.getOrDefault(KitType.ARCHER, 0)));
        inv.setItem(6, createItem(Material.SHIELD, "§bTank", votes.getOrDefault(KitType.TANK, 0)));

        return inv;
    }

    private static ItemStack createItem(Material mat, String name, int votes) {

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(List.of("§7Votes: §f" + votes));

        item.setItemMeta(meta);
        return item;
    }
}
