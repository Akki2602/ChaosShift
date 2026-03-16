package akki.chaosshift;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ChaosShift extends JavaPlugin {

    private ChaosEvents events;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Hello World");

        events = new ChaosEvents(this);
        events.startChaos();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
