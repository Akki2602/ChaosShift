package akki.chaosshift;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class GameManager {

    private final Plugin plugin;
    private GameState state = GameState.WAITING;
    private ChaosEvents events;

    public GameManager(Plugin plugin){
        this.plugin = plugin;
    }

    public void startAutoGameLoop() {

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if(state == GameState.WAITING) {

                if (Bukkit.getOnlinePlayers().size() >= 2) {
                    startGame();
                }
            }
        }, 0L, 100L);;;;;;
    }

    public void startGame(){

        if (state != GameState.WAITING) return;

        if (Bukkit.getOnlinePlayers().size() < 2){
            Bukkit.broadcast(
                    Component.text("Not enough players!", NamedTextColor.RED)
            );
            return;
        }

        state = GameState.STARTING;

        startCountdown();
    }

    private void startCountdown(){

        final int[] timeLeft = {5};

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {

            if (timeLeft[0] <= 0) {

                Bukkit.broadcast(
                        Component.text("GO!", NamedTextColor.GREEN)
                );

                task.cancel();
                beginGame();
                return;
            }

            Bukkit.broadcast(
                    Component.text("Starting in " + timeLeft[0] + "...", NamedTextColor.YELLOW)
            );

            timeLeft[0]--;
        }, 0L, 20L);
    }

    private void beginGame(){

        state = GameState.RUNNING;

        Bukkit.broadcast(
                Component.text("Game Started!", NamedTextColor.GREEN)
        );

        events = new ChaosEvents(plugin);
        events.startChaos();

        Bukkit.getScheduler().runTaskLater(plugin, this::endGame, 12000L);
    }

    public void endGame() {
        state = GameState.ENDING;

        // stop chaos FIRST
        if (events != null) {
            events.stopChaos();
        }

        Bukkit.broadcast(
                Component.text("Game Over!", NamedTextColor.RED)
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(
                    net.kyori.adventure.title.Title.title(
                            Component.text("Game Over"),
                            Component.empty()
                    )
            );
        }

        // small delay before resetting
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            state = GameState.WAITING;
        }, 100L);
    }

    public GameState getState() {
        return state;
    }
}