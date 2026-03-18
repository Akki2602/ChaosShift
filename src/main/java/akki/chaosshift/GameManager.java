package akki.chaosshift;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            Bukkit.broadcast(
                    Component.text("Not enough players!", NamedTextColor.RED)
            );
            return;
        }

        state = GameState.RUNNING;

        Bukkit.broadcast(net.kyori.adventure.text.Component.text("Game Started!"));

        ChaosEvents events = new ChaosEvents(plugin);
        events.startChaos();

        Bukkit.getScheduler().runTaskLater(plugin, this::endGame, 12000L);
    }

    public void endGame() {
        state = GameState.ENDING;

        Bukkit.broadcast(net.kyori.adventure.text.Component.text("Game Over!"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(
                    net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text("Game Over"),
                            net.kyori.adventure.text.Component.empty(),
                            net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(3000),
                                    java.time.Duration.ofMillis(1000)
                            )
                    )
            );
        }

        state = GameState.WAITING;
    }

    public GameState getState() {
        return state;
    }
}
