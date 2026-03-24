package akki.chaosshift;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class GameManager {

    private final Plugin plugin;
    private GameState state = GameState.WAITING;
    private ChaosEvents events;
    private int gameTime = 0;
    private int difficultyLevel = 1;

    private final int minX = -14;
    private final int maxX = 14;
    private final int minZ = -14;
    private final int maxZ = 14;
    private final int minY = 118;
    private final int maxY = 130;

    private boolean gameRunning = false;

    private final org.bukkit.World world = org.bukkit.Bukkit.getWorld("world");

    private final java.util.Map<org.bukkit.Location, org.bukkit.Material> platformBlocks = new java.util.HashMap<>();

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

    private void savePlatform() {

        platformBlocks.clear();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    var loc = new org.bukkit.Location(world, x, y, z);
                    var block = loc.getBlock();

                    if (block.getType() != org.bukkit.Material.AIR) {
                        platformBlocks.put(loc, block.getType());
                    }
                }
            }
        }
    }

    private void removePlatform() {

        for (var loc : platformBlocks.keySet()) {
            loc.getBlock().setType(org.bukkit.Material.AIR);
        }
    }

    private void restorePlatform() {

        for (var entry : platformBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
    }

    public void startGame(){

        if (gameRunning) return;

        gameRunning = true;

        savePlatform();

        for (var player : org.bukkit.Bukkit.getOnlinePlayers()) {
            player.teleport(new org.bukkit.Location(world, 0, 131, 0));
        }

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

            if (gameTime % 30 == 0) {
                difficultyLevel++;

                Bukkit.broadcast(
                        net.kyori.adventure.text.Component.text(
                                "Difficulty Increased! Level " + difficultyLevel
                        )
                );
            }

            updateScoreboard();

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


        removePlatform();

        for (var player : org.bukkit.Bukkit.getOnlinePlayers()) {
            player.setFallDistance(0);

            player.playSound(player.getLocation(),
                    Sound.BLOCK_GLASS_BREAK, 1f, 1f);
        }

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> restorePlatform(), 60L);

        state = GameState.RUNNING;

        Bukkit.broadcast(
                Component.text("Game Started!", NamedTextColor.GREEN)
        );

        events = new ChaosEvents(plugin, this);
        events.startChaos();

        Bukkit.getScheduler().runTaskLater(plugin, this::endGame, 12000L);
    }

    public void playerDied(java.util.UUID uuid) {

        if(!alivePlayers.contains(uuid)) return;

        alivePlayers.remove(uuid);

        var player = Bukkit.getPlayer(uuid);

        if (player != null) {

            player.setGameMode(GameMode.SPECTATOR);

            player.teleport(new org.bukkit.Location(
                    Bukkit.getWorld("world"), 0, 131, 0
            ));

        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "A player has been eliminated! Remaining: " + alivePlayers.size()
                )
        );

        checkWinCondition();
    }

    public boolean isAlive(java.util.UUID uuid) {
        return alivePlayers.contains(uuid);
    }

    private void updateScoreboard() {

        for (var player : Bukkit.getOnlinePlayers()) {

            var manager = Bukkit.getScoreboardManager();
            var board = manager.getNewScoreboard();

            var obj = board.registerNewObjective(
                    "chaos",
                    "dummy",
                    net.kyori.adventure.text.Component.text("§6§lChaosShift")
            );

            obj.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

            int score = 6;

            // Alive players
            obj.getScore("§aAlive: §f" + alivePlayers.size()).setScore(score--);

            // Blank line
            obj.getScore(" ").setScore(score--);

            // Difficulty
            obj.getScore("§cDifficulty: §f" + difficultyLevel).setScore(score--);

            // Blank line (must be different)
            obj.getScore("  ").setScore(score--);

            // Time
            obj.getScore("§bTime: §f" + gameTime + "s").setScore(score--);

            // Footer (optional)
            obj.getScore("§7§oakki").setScore(score);

            player.setScoreboard(board);
        }
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

        gameRunning = false;

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

        for (var player : org.bukkit.Bukkit.getOnlinePlayers()) {
            player.teleport(new org.bukkit.Location(world, 0, 131, 0));
        }
        alivePlayers.clear();

        // small delay before resetting
        Bukkit.getScheduler().runTaskLater(plugin, () -> state = GameState.WAITING, 100L);
    }

    public GameState getState() {
        return state;
    }
}