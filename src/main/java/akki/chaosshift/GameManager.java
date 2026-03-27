package akki.chaosshift;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import java.util.Objects;

public class GameManager {

    private final Plugin plugin;
    private GameState state = GameState.WAITING;
    private ChaosEvents events;
    private int gameTime = 0;
    private int difficultyLevel = 1;

    private boolean gameRunning = false;

    private final org.bukkit.World world = org.bukkit.Bukkit.getWorld("world");

    private final java.util.Map<org.bukkit.Location, org.bukkit.Material> platformBlocks = new java.util.HashMap<>();

    private final java.util.Set<java.util.UUID> alivePlayers = new java.util.HashSet<>();

    private final java.util.Map<java.util.UUID, KitType> playerKits = new java.util.HashMap<>();

    private final Map<UUID, KitType> playerVotes = new HashMap<>();

    private int votingTime = 10;

    private final java.util.Set<java.util.UUID> landedPlayers = new java.util.HashSet<>();
    private boolean eventsStarted = false;

    public void setKit(java.util.UUID uuid, KitType kit) {
        playerKits.put(uuid, kit);
    }

    public java.util.Map<KitType, Integer> getKitVotes() {
        return kitVotes;
    }

    public KitType getKit(java.util.UUID uuid) {
        return playerKits.getOrDefault(uuid, KitType.WARRIOR);
    }

    private final java.util.Map<KitType, Integer> kitVotes = new java.util.HashMap<>();
    private KitType selectedKit = KitType.WARRIOR;

    public GameManager(Plugin plugin){
        this.plugin = plugin;
    }

    public int getDifficultyLevel(){
        return difficultyLevel;
    }

    private void savePlatform() {

        platformBlocks.clear();

        int minX = -14;
        int maxX = 14;
        for (int x = minX; x <= maxX; x++) {
            int minY = 118;
            int maxY = 130;
            for (int y = minY; y <= maxY; y++) {
                int minZ = -14;
                int maxZ = 14;
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

        kitVotes.clear();
        for (KitType kit : KitType.values()) {
            kitVotes.put(kit, 0);
        }

        startVotingCountdown();

        for (var player : Bukkit.getOnlinePlayers()) {

            player.getInventory().clear();

            // Kit selector (COMPASS)
            var compass = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS);
            var meta = compass.getItemMeta();
            meta.setDisplayName("§aKit Selection");
            compass.setItemMeta(meta);

            player.getInventory().addItem(compass);

            // Game start (EMERALD for OP)
            if (player.isOp()) {

                var emerald =  new ItemStack(Material.EMERALD);
                var meta_e = emerald.getItemMeta();
                meta_e.setDisplayName("§aStart Game");
                emerald.setItemMeta(meta_e);

                player.getInventory().addItem(emerald);
            }
        }

        playerVotes.clear();

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

    }

    private void resetDifficulty() {
        difficultyLevel = 1;
        gameTime = 0;
    }



    private void startVotingCountdown() {

        votingTime = 10;

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {

            if (votingTime <= 0) {

                task.cancel();

                Bukkit.broadcast(
                        net.kyori.adventure.text.Component.text("Voting Ended!")
                );
                beginGame();
                return;
            }

            for (var player : Bukkit.getOnlinePlayers()) {
                player.showTitle(
                        net.kyori.adventure.title.Title.title(
                                net.kyori.adventure.text.Component.text("Vote for Kit"),
                                net.kyori.adventure.text.Component.text("Time: " + votingTime)
                        )
                );
            }

            votingTime--;
        }, 0L, 20L);

    }

    public void voteKit(UUID uuid, KitType kit) {

        // Remove previous vote
        if (playerVotes.containsKey(uuid)) {
            KitType old = playerVotes.get(uuid);
            kitVotes.put(old, kitVotes.get(old) - 1);
        }

        playerVotes.put(uuid, kit);
        kitVotes.put(kit, kitVotes.getOrDefault(kit, 0) + 1);

        Bukkit.broadcast(Component.text("Vote: " + kit.name()));
    }

    private void decideWinningKit() {

        int maxVotes = -1;
        java.util.List<KitType> winners = new java.util.ArrayList<>();

        for (var entry : kitVotes.entrySet()) {

            int votes = entry.getValue();

            if (votes > maxVotes) {
                maxVotes = votes;
                winners.clear();
                winners.add(entry.getKey());
            } else if (votes == maxVotes) {
                winners.add(entry.getKey());
            }
        }

        // Tie → random
        if (winners.size() > 1) {
            selectedKit = winners.get(new java.util.Random().nextInt(winners.size()));
        } else {
            selectedKit = winners.get(0);
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Selected Kit: " + selectedKit.name()
                )
        );
    }

    public int getVotes(KitType kit) {
        return kitVotes.getOrDefault(kit, 0);
    }

    private void beginGame(){

        preparePlayersForGame();

        decideWinningKit();

        for (var player : Bukkit.getOnlinePlayers()) {
            giveKit(player);
        }

        landedPlayers.clear();
        eventsStarted = false;

        removePlatform();

        for (var player : org.bukkit.Bukkit.getOnlinePlayers()) {
            player.setFallDistance(0);

            player.playSound(player.getLocation(),
                    Sound.BLOCK_GLASS_BREAK, 1f, 1f);
        }

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, this::restorePlatform, 60L);

        state = GameState.RUNNING;

        Bukkit.broadcast(
                Component.text("Game Started!", NamedTextColor.GREEN)
        );



        Bukkit.getScheduler().runTaskLater(plugin, this::endGame, 12000L);
    }

    public void forceStopGame() {

        // Stop chaos
        if (events != null) {
            events.stopChaos();
        }

        // Reset state
        gameRunning = false;
        state = GameState.WAITING;

        resetDifficulty();

        for (var player : Bukkit.getOnlinePlayers()) {

            player.teleport(new org.bukkit.Location(world, 0, 131, 0));

            player.setGameMode(GameMode.SURVIVAL);

            player.setHealth(
                    player.getAttribute(Attribute.MAX_HEALTH).getValue()
            );

            player.setFoodLevel(20);

            player.getInventory().clear();
        }

        alivePlayers.clear();

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text("Game force stopped!")
        );
    }



    private void giveKit(org.bukkit.entity.Player player) {

        var inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(null);

        switch (selectedKit) {

            case WARRIOR -> {
                inv.addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD));
                inv.setArmorContents(new org.bukkit.inventory.ItemStack[]{
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS),
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS),
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE),
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET)
                });
            }

            case ARCHER -> {
                inv.addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
                inv.addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 32));
            }

            case TANK -> {
                inv.addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE_SWORD));
                inv.setArmorContents(new org.bukkit.inventory.ItemStack[]{
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_BOOTS),
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_LEGGINGS),
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_CHESTPLATE),
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HELMET)
                });
            }
        }

        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
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
        } else if (alivePlayers.isEmpty()) {
            Bukkit.broadcast(Component.text("Draw! No one survived."));
            endGame();
        }
    }

    public void playerLanded(java.util.UUID uuid) {

        landedPlayers.add(uuid);

        // If all alive players landed → start events
        if (!eventsStarted && landedPlayers.containsAll(alivePlayers)) {

            eventsStarted = true;

            Bukkit.broadcast(
                    net.kyori.adventure.text.Component.text("Chaos Begins!")
            );

            events = new ChaosEvents(plugin, this);
            events.startChaos();
        }
    }

    public boolean hasLanded(java.util.UUID uuid) {
        return landedPlayers.contains(uuid);
    }

    public void preparePlayersForGame() {

        for (var player : Bukkit.getOnlinePlayers()) {

            player.setGameMode(GameMode.SURVIVAL);

            player.setHealth(
                    player.getAttribute(Attribute.MAX_HEALTH).getValue()
            );

            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        }
    }

        public void endGame() {

        for (var player : org.bukkit.Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
        }

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
        resetDifficulty();

        // small delay before resetting
        Bukkit.getScheduler().runTaskLater(plugin, () -> state = GameState.WAITING, 100L);
    }

    public GameState getState() {
        return state;
    }
}