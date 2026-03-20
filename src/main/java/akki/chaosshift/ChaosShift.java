package akki.chaosshift;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ChaosShift extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("ChaosShift enabled!");

        preloadWorld("world");
        preloadWorld("world_nether");
        preloadWorld("world_the_end");

        loadArenaChunks(Bukkit.getWorld("world"));

        gameManager = new GameManager(this);

        gameManager.startAutoGameLoop();
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
