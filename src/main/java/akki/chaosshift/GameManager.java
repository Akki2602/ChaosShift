package akki.chaosshift;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

public class GameManager {

    private final Plugin plugin;
    private GameState state = GameState.WAITING;

    public GameManager(Plugin plugin){
        this.plugin = plugin;
    }

    public void startGame(){

        if (Bukkit.getOnlinePlayers().size() < 2){
            Bukkit.broadcastMessage("§cNot enough players!");
            return;
        }

        state = GameState.RUNNING;

        Bukkit.broadcastMessage("§aGame Started!");

        ChaosEvents events = new ChaosEvents(plugin);
        events.startChaos();

        Bukkit.getScheduler().runTaskLater(plugin, this::endGame, 12000L);
    }

    public void endGame() {
        state = GameState.ENDING;

        Bukkit.broadcastMessage("§cGame Over!");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("Game Over", "", 10, 60, 10);
        }

        state = GameState.WAITING;
    }

    public GameState getState() {
        return state;
    }
}
