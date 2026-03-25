package akki.chaosshift;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class KitMenu {

    public static org.bukkit.inventory.Inventory createMenu(GameManager gm) {

        var inv = org.bukkit.Bukkit.createInventory(null, 9, "Select Kit");

        inv.setItem(2, createItem(
                org.bukkit.Material.IRON_SWORD,
                "§cWarrior",
                gm.getVotes(KitType.WARRIOR)
        ));

        inv.setItem(4, createItem(
                org.bukkit.Material.BOW,
                "§aArcher",
                gm.getVotes(KitType.ARCHER)
        ));

        inv.setItem(6, createItem(
                org.bukkit.Material.SHIELD,
                "§bTank",
                gm.getVotes(KitType.TANK)
        ));

        return inv;
    }



    private static org.bukkit.inventory.ItemStack createItem(
            org.bukkit.Material mat,
            String name,
            int votes
    ) {

        var item = new org.bukkit.inventory.ItemStack(mat);
        var meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(java.util.List.of("§7Votes: §f" + votes));

        item.setItemMeta(meta);

        return item;
    }
}
