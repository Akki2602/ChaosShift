package akki.chaosshift;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class ChaosEvents {

    private final Plugin plugin;
    private final Random random = new Random();
    private int taskId;

    private final int minX = -30;
    private final int maxX = 30;
    private final int minZ = -30;
    private final int maxZ = 30;
    private final int arenaY = 80;

    private final java.util.List<org.bukkit.World> dimensionCycle = new java.util.ArrayList<>();
    private int currentDimensionIndex = -1;
    private org.bukkit.World lastWorldUsed = null; // prevents cross-round repeat

    private final java.util.List<Runnable> eventPool = new java.util.ArrayList<>();
    private final java.util.Queue<Runnable> eventQueue = new java.util.LinkedList<>();

    private final GameManager gameManager;

    public ChaosEvents(Plugin plugin, GameManager gameManager) {

        var overworld = Bukkit.getWorld("world");
        var nether = Bukkit.getWorld("world_nether");
        var end = Bukkit.getWorld("world_the_end");

        if (overworld != null) dimensionCycle.add(overworld);
        if (nether != null) dimensionCycle.add(nether);
        if (end != null) dimensionCycle.add(end);

// Shuffle initial order
        java.util.Collections.shuffle(dimensionCycle);

        this.plugin = plugin;
        this.gameManager = gameManager;

        eventPool.add(this::potionEffects);
        eventPool.add(this::spawnChasingMobs);
        eventPool.add(this::teleportSwap);
        eventPool.add(this::changeDimension);
        eventPool.add(this::mutateBlocks);
        eventPool.add(this::lowGravity);

        reshuffleEvents();
    }

    public void resetDimensionCycle() {

        if (dimensionCycle.size() < 2) return;

        java.util.Collections.shuffle(dimensionCycle);

        // Prevent repeating last dimension across rounds
        if (lastWorldUsed != null && dimensionCycle.get(0).equals(lastWorldUsed)) {
            java.util.Collections.rotate(dimensionCycle, 1);
        }

        currentDimensionIndex = -1;
    }

    private void reshuffleEvents() {

        java.util.List<Runnable> shuffled = new java.util.ArrayList<>(eventPool);
        java.util.Collections.shuffle(shuffled);

        eventQueue.clear();
        eventQueue.addAll(shuffled);
    }

    private Runnable getNextEvent() {

        if (eventQueue.isEmpty()) {
            reshuffleEvents();
        }
        return eventQueue.poll();
    }

    public void startChaos() {

        long interval = Math.max(60L, 500L - (gameManager.getDifficultyLevel() * 40L));

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {

            @Override
            public void run() {

                Runnable event = getNextEvent();
                if (event != null) event.run();

                // Restart task with new interval
                Bukkit.getScheduler().cancelTask(taskId);

                long interval = Math.max(60L, 500L - (gameManager.getDifficultyLevel() * 40L));

                startChaos(); // restart with new speed
            }

        }, 0L, Math.max(60L, 500L - (gameManager.getDifficultyLevel() * 40L))).getTaskId();
    }

    private void potionEffects() {

        int difficulty = gameManager.getDifficultyLevel();


        int amp = Math.min(5, difficulty);

        for (var player : Bukkit.getOnlinePlayers()) {

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED,
                    600,
                    amp
            ));

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.JUMP_BOOST,
                    600,
                    amp
            ));

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.STRENGTH,
                    600,
                    Math.max(1, amp / 2)
            ));

            player.playSound(
                    player.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                    1f,
                    1.2f
            );

            player.showTitle(
                    net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text(
                                    "Power Surge!",
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ff0000")
                            ),
                            net.kyori.adventure.text.Component.text(
                                    "Level " + difficulty,
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ffff00")
                            )
                    )
            );
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Everything changed: Power Surge! (Level " + difficulty + ")"
                )
        );
    }

    private void lowGravity() {

        int difficulty = gameManager.getDifficultyLevel();

        // scale but limit max power
        int amp = Math.min(5, difficulty);

        for (var player : Bukkit.getOnlinePlayers()) {

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    PotionEffectType.LEVITATION,
                    200,
                    amp
            ));

            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_SHULKER_AMBIENT,
                    1f,
                    1.2f
            );

            player.showTitle(
                    net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text(
                                    "Low Gravity!",
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ff11ff")
                            ),
                            net.kyori.adventure.text.Component.text(
                                    "Level " + difficulty,
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ffff00")
                            )
                    )
            );
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Everything changed: Low Gravity! (Level " + difficulty + ")"
                )
        );

    }

    private void spawnChasingMobs() {

        var players = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return;

        java.util.Collections.shuffle(players);
        players.stream().limit(3).forEach(player -> {

            var world = player.getWorld();

            var mobTypes = getMobsForWorld(world);

            if (mobTypes.isEmpty()) return;

            var targetMob = mobTypes.get(random.nextInt(mobTypes.size()));

            var loc = player.getLocation().clone().add(
                    random.nextInt(5) - 2,
                    0,
                    random.nextInt(5) - 2
            );

            int difficulty = gameManager.getDifficultyLevel();

            int mobCount = Math.min(5, 1 + difficulty / 2);

            for (int i = 0; i < mobCount; i++) {

                var spawnLoc = player.getLocation().clone().add(
                        random.nextInt(5) - 2,
                        0,
                        random.nextInt(5) - 2
                );


                var entity = world.spawnEntity(spawnLoc, targetMob);

                if (entity instanceof org.bukkit.entity.Mob mob) {
                    mob.setTarget(player);

                    applyMobScaling(mob, difficulty);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!mob.isDead()) {
                            mob.remove();
                        }
                    }, 200L); // 10 seconds
                }

            }


            player.playSound(
                    player.getLocation(),
                    org.bukkit.Sound.ENTITY_ZOMBIE_AMBIENT,
                    1f,
                    1f
            );

            player.showTitle(
                    net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text(
                                    "Hunted!",
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ff0000")
                            ),
                            net.kyori.adventure.text.Component.text(
                                    targetMob.name(),
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ffaa00")
                            )
                    )
            );
        });


        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Everything changed: You are being hunted!"
                )
        );
    }

    private void applyMobScaling(org.bukkit.entity.Mob mob, int difficulty) {

        // Speed scaling
        mob.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED,
                200,
                Math.min(3, difficulty / 2)
        ));

        // Strength scaling
        mob.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH,
                200,
                Math.min(2, difficulty / 3)
        ));

        // Extra health scaling
        var attribute = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attribute != null) {
            double newHealth = attribute.getBaseValue() + (difficulty * 2);
            attribute.setBaseValue(newHealth);
            mob.setHealth(newHealth);
        }

        // Optional: make mobs slightly faster AI
        mob.setPersistent(false); // prevents buildup
    }

    private java.util.List<org.bukkit.entity.EntityType> getMobsForWorld(org.bukkit.World world) {

        var list = new java.util.ArrayList<org.bukkit.entity.EntityType>();

        switch (world.getEnvironment()) {

            case NORMAL -> {
                list.add(org.bukkit.entity.EntityType.ZOMBIE);
                list.add(org.bukkit.entity.EntityType.SKELETON);
                list.add(org.bukkit.entity.EntityType.SPIDER);
                list.add(org.bukkit.entity.EntityType.CREEPER);
            }

            case NETHER -> {
                list.add(org.bukkit.entity.EntityType.BLAZE);
                list.add(org.bukkit.entity.EntityType.WITHER_SKELETON);
                list.add(org.bukkit.entity.EntityType.PIGLIN);
                list.add(org.bukkit.entity.EntityType.MAGMA_CUBE);
            }

            case THE_END -> {
                list.add(org.bukkit.entity.EntityType.ENDERMAN);
                list.add(org.bukkit.entity.EntityType.SHULKER);
            }
        }

        return list;
    }

    private void teleportSwap() {

        var players = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());

        if (players.size() < 2) return;

        java.util.Collections.shuffle(players);

        var locations = new java.util.ArrayList<org.bukkit.Location>();
        for (Player p : players) {
            locations.add(p.getLocation());
        }

        for (Player p : players) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    PotionEffectType.BLINDNESS,
                    40, // 2 seconds
                    1
            ));
        }

        for (int i = 0; i < players.size(); i++) {

            Player current = players.get(i);
            int nextIndex = (i + 1) % players.size();

            var target = players.get(nextIndex);
            var targetLocation = locations.get(nextIndex);

            current.teleport(targetLocation);

            current.playSound(
                    current.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    1f,
                    1f
            );

            current.showTitle(
                    net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text("Switched Places!", TextColor.fromHexString("#0b4d42")),
                            net.kyori.adventure.text.Component.text("With " + target.getName()),
                            net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(300),
                                    java.time.Duration.ofMillis(2000),
                                    java.time.Duration.ofMillis(300)
                            )
                    )
            );

            current.sendMessage(
                    net.kyori.adventure.text.Component.text(
                            "Everything changed: Reality swapped players!"
                    )
            );
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Everything changed: Everyone swapped positions!"
                )
        );
    }

    private void changeDimension() {

        if (dimensionCycle.size() < 2) return;

        int difficulty = gameManager.getDifficultyLevel();

        // Number of shifts increases with difficulty
        int shifts = Math.min(3, 1 + difficulty / 4);

        for (int s = 0; s < shifts; s++) {

            currentDimensionIndex = (currentDimensionIndex + 1) % dimensionCycle.size();
            var targetWorld = dimensionCycle.get(currentDimensionIndex);

            var players = Bukkit.getOnlinePlayers();

            int blindnessDuration = 40 + (difficulty * 5);

            for (var p : players) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        PotionEffectType.BLINDNESS,
                        blindnessDuration,
                        1
                ));

                // Add confusion at higher levels
                if (difficulty >= 5) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            PotionEffectType.NAUSEA,
                            60,
                            0
                    ));
                }
            }

            for (var player : players) {

                var currentLoc = player.getLocation();

                var origin = new Location(currentLoc.getWorld(), 0, 80, 0);

                double offsetX = currentLoc.getX() - origin.getX();
                double offsetZ = currentLoc.getZ() - origin.getZ();

                // Add chaos offset at higher difficulty
                if (difficulty >= 6) {
                    offsetX += (random.nextDouble() - 0.5) * difficulty;
                    offsetZ += (random.nextDouble() - 0.5) * difficulty;
                }

                var targetOrigin = new Location(targetWorld, 0, 80, 0);

                var newLoc = targetOrigin.clone().add(offsetX, 0, offsetZ);
                newLoc.setY(80);

                player.teleport(newLoc);

                player.playSound(
                        player.getLocation(),
                        Sound.ITEM_CHORUS_FRUIT_TELEPORT,
                        1f,
                        1f + (difficulty * 0.05f)
                );

                player.showTitle(
                        net.kyori.adventure.title.Title.title(
                                net.kyori.adventure.text.Component.text(
                                        "Reality Fractured!",
                                        net.kyori.adventure.text.format.TextColor.fromHexString("#a64dff")
                                ),
                                net.kyori.adventure.text.Component.text(
                                        "Entered " + formatWorldName(targetWorld.getName()),
                                        net.kyori.adventure.text.format.TextColor.fromHexString("#00ffff")
                                ),
                                Title.Times.times(
                                        java.time.Duration.ofMillis(200),
                                        java.time.Duration.ofMillis(1500),
                                        java.time.Duration.ofMillis(200)
                                )
                        )
                );
            }

            lastWorldUsed = targetWorld;
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Everything changed: Reality is collapsing! (Level " + difficulty + ")"
                )
        );
    }
    private void mutateBlocks() {

        int blocksToChange = 4 + gameManager.getDifficultyLevel();

        var materials = new org.bukkit.Material[]{
                Material.SLIME_BLOCK,
                Material.HONEY_BLOCK,
                Material.MAGMA_BLOCK,
                Material.AIR
        };

        var players = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
        java.util.Collections.shuffle(players);

        players.stream().limit(3).forEach(player -> {

            var world = player.getWorld();
            var baseLoc = player.getLocation();

            for (int i = 0; i < blocksToChange; i++) {

                int xOffset = random.nextInt(7) - 3;
                int zOffset = random.nextInt(7) - 3;

                int x = baseLoc.getBlockX() + xOffset;
                int z = baseLoc.getBlockZ() + zOffset;

                if (x < minX || x > maxX || z < minZ || z > maxZ) continue;

                var loc = new org.bukkit.Location(world, x, arenaY, z);
                var block = loc.getBlock();

                if (block.getType() == Material.AIR) continue;

                var oldMaterial = block.getType();
                var newMaterial = materials[random.nextInt(materials.length)];

                block.setType(newMaterial);

                Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(oldMaterial), 80L);
            }
            player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_SLIME_BLOCK_PLACE,
                    1f,
                    1f
            );

            player.showTitle(
                    net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text(
                                    "Unstable Ground!",
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ff9900")
                            ),
                            net.kyori.adventure.text.Component.text(
                                    "Watch your step!",
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ffff00")
                            )
                    )
            );
        });

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Everything changed: The ground is unstable!"
                )
        );
    }






    private String formatWorldName(String name) {
        return switch (name) {
          case "world" -> "Overworld";
          case "world_nether" -> "Nether";
          case "world_the_end" -> "End";
            default -> name;
        };
    }


    public void stopChaos() {
        Bukkit.getScheduler().cancelTask(taskId);
    }
}