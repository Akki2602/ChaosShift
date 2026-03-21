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

    private final java.util.List<Runnable> eventPool = new java.util.ArrayList<>();
    private final java.util.Queue<Runnable> eventQueue = new java.util.LinkedList<>();

    private final GameManager gameManager;

    public ChaosEvents(Plugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        eventPool.add(this::potionEffects);
        eventPool.add(this::spawnChasingMobs);
        eventPool.add(this::teleportSwap);
        eventPool.add(this::changeDimension);
        eventPool.add(this::mutateBlocks);

        reshuffleEvents();
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

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            Runnable event = getNextEvent();
            if (event != null) event.run();

        }, 0L, Math.max(100L, 600L - (gameManager.getDifficultyLevel() * 50L))).getTaskId();
    }

    private void potionEffects() {

        int difficulty = gameManager.getDifficultyLevel();

        // scale but limit max power
        int amp = Math.min(5, difficulty);

        for (var player : Bukkit.getOnlinePlayers()) {

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED,
                    200,
                    amp
            ));

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.JUMP_BOOST,
                    200,
                    amp
            ));

            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.STRENGTH,
                    200,
                    Math.max(1, amp / 2)
            ));

            // Sound
            player.playSound(
                    player.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                    1f,
                    1.2f
            );

            // Title
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

    private final  EntityType[] chaosMobs = {
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER
    };

    private void spawnChasingMobs() {

        EntityType type = chaosMobs[random.nextInt(chaosMobs.length)];

        for (Player player : Bukkit.getOnlinePlayers()) {

            for (int i = 0; i < 3; i++) {

                Location loc = player.getLocation().clone().add(
                        random.nextInt(6) - 3,
                        0,
                        random.nextInt(6) -3
                );

                LivingEntity mob = (LivingEntity) player.getWorld().spawnEntity(loc, type);

                if(mob instanceof Monster monster){
                    monster.setTarget(player);
                }
            }
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text("Everything changed: " + type.name() + " are hunting you!")
        );

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Entity e : world.getEntities()) {
                    if (e instanceof Monster) {
                        e.remove();
                    }
                }
            }
        }, 600L);
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

        var overworld = Bukkit.getWorld("world");
        var nether = Bukkit.getWorld("world_nether");
        var end = Bukkit.getWorld("world_the_end");

        var worlds = new java.util.ArrayList<org.bukkit.World>();

        if (overworld != null) worlds.add(overworld);
        if (nether != null) worlds.add(nether);
        if (end != null) worlds.add(end);

        if (worlds.size() < 2) return;

        org.bukkit.World currentWorld = Bukkit.getOnlinePlayers().iterator().next().getWorld();

        org.bukkit.World targetWorld;
        do {
            targetWorld = worlds.get(random.nextInt(worlds.size()));
        } while (targetWorld.equals(currentWorld));

        var players = Bukkit.getOnlinePlayers();

        for (var p : players) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    PotionEffectType.BLINDNESS,
                    40,
                    1
            ));
        }

        for (var player : players) {

            var currentLoc = player.getLocation();

            var origin = new org.bukkit.Location(
                    currentLoc.getWorld(),
                    0, 80, 0
            );

            double offsetX = currentLoc.getX() - origin.getX();
            double offsetZ = currentLoc.getZ() - origin.getZ();

            var targetOrigin = new org.bukkit.Location(
                    targetWorld,
                    0, 80, 0
            );

            var newLoc = targetOrigin.clone().add(offsetX, 0, offsetZ);

            newLoc.setY(80);

            player.teleport(newLoc);

            player.playSound(
                    player.getLocation(),
                    Sound.ITEM_CHORUS_FRUIT_TELEPORT,
                    1f,
                    1f
            );

            player.showTitle(
                    net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text(
                                    "Reality Shifted!",
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#a64dff")
                            ),
                            net.kyori.adventure.text.Component.text(
                                    "Entered " + formatWorldName(targetWorld.getName()),
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#00ffff")
                            ),
                            Title.Times.times(
                                    java.time.Duration.ofMillis(300),
                                    java.time.Duration.ofMillis(2000),
                                    java.time.Duration.ofMillis(300)
                            )
                    )
            );

            player.sendMessage(
                    net.kyori.adventure.text.Component.text(
                            "You have been shifted into " + formatWorldName(targetWorld.getName()) + "!"
                    )
            );
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Everything changed: The dimension shifted!"
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