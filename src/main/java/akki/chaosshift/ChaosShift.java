package akki.chaosshift;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ChaosShift extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("ChaosShift enabled!");

        gameManager = new GameManager(this);

        // auto start after 10 seconds (testing)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            gameManager.startGame();
        }, 200L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
