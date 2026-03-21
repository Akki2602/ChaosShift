package akki.chaosshift;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class GameManager {

    private final Plugin plugin;
    private GameState state = GameState.WAITING;
    private ChaosEvents events;
    private int gameTime = 0;
    private int difficultyLevel = 1;

    private final java.util.Set<java.util.UUID> alivePlayers = new java.util.HashSet<>();

    public GameManager(Plugin plugin){
        this.plugin = plugin;
    }

    public int getDifficultyLevel(){
        return difficultyLevel;
    }

    public void startAutoGameLoop() {

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if(state == GameState.WAITING) {

                if (Bukkit.getOnlinePlayers().size() >= 2) {
                    startGame();
                }
            }
        }, 0L, 100L);
    }

    public void startGame(){

        for (var player : Bukkit.getOnlinePlayers()) {
            alivePlayers.add(player.getUniqueId());
        }

        if (state != GameState.WAITING) return;

        if (Bukkit.getOnlinePlayers().size() < 2){
            Bukkit.broadcast(
                    Component.text("Not enough players!", NamedTextColor.RED)
            );
            return;
        }

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            gameTime++;

            if(gameTime % 30 == 0){
                difficultyLevel++;

                Bukkit.broadcast(
                        net.kyori.adventure.text.Component.text(
                                "Difficulty Increase! Level " + difficultyLevel
                        )
                );
            }
        }, 20L, 20L);

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

        events = new ChaosEvents(plugin, this);
        events.startChaos();

        Bukkit.getScheduler().runTaskLater(plugin, this::endGame, 12000L);
    }

    public void playerDied(java.util.UUID uuid) {
        alivePlayers.remove(uuid);

        var player = Bukkit.getPlayer(uuid);

        if (player != null) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "A player has been eliminated! Remaining: " + alivePlayers.size()
                )
        );

        checkWinCondition();
    }

    private void checkWinCondition(){

        if (alivePlayers.size() == 1) {

            var winnerUUID = alivePlayers.iterator().next();
            var winner = Bukkit.getPlayer(winnerUUID);

            if (winner != null) {

                Bukkit.broadcast(
                        net.kyori.adventure.text.Component.text(
                                winner.getName() + " has won the game!"
                        )
                );

                winner.showTitle(
                        net.kyori.adventure.title.Title.title(
                                net.kyori.adventure.text.Component.text("VICTORY"),
                                net.kyori.adventure.text.Component.text("You Survived!")
                        )
                );
            }

            endGame();
        }
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
        alivePlayers.clear();

        // small delay before resetting
        Bukkit.getScheduler().runTaskLater(plugin, () -> state = GameState.WAITING, 100L);
    }

    public GameState getState() {
        return state;
    }
}