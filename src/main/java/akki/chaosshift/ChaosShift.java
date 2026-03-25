package akki.chaosshift;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

public final class ChaosShift extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {

        for (var world : org.bukkit.Bukkit.getWorlds()) {
            world.setDifficulty(org.bukkit.Difficulty.HARD);
        }

        for (var player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.COMPASS));
            }
        }

        Bukkit.getPluginManager().registerEvents(
                new PlayerListener(gameManager),
                this
        );

        // Plugin startup logic
        getLogger().info("ChaosShift enabled!");

        preloadWorld("world");
        preloadWorld("world_nether");
        preloadWorld("world_the_end");

        loadArenaChunks(Bukkit.getWorld("world"));

        gameManager = new GameManager(this);

        getCommand("startchaos").setExecutor(new StartChaosCommand(gameManager));

    }

        private void preloadWorld(String name) {
            var world = Bukkit.getWorld(name);

            if (world == null) {
                world = Bukkit.createWorld(new org.bukkit.WorldCreator(name));
            }
        }

        private void loadArenaChunks(World world) {

            if (world == null) return;

            int radius = 4;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {

                    world.getChunkAt(x, z).setForceLoaded(true);
                }
            }
        }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
