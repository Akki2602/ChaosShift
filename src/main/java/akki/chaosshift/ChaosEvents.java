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

    private final java.util.List<Runnable> eventPool = new java.util.ArrayList<>();
    private final java.util.Queue<Runnable> eventQueue = new java.util.LinkedList<>();

    private final GameManager gameManager;

    private int dimensionIndex = 0;

    public ChaosEvents(Plugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        eventPool.add(this::potionEffects);
        eventPool.add(this::spawnChasingMobs);
        eventPool.add(this::teleportSwap);
        eventPool.add(this::changeDimension);
        eventPool.add(this::lowGravity);
        eventPool.add(this::changeDimension);
        eventPool.add(this::spawnChasingMobs);

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

        }, 0L, 300L).getTaskId();
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

                spawnLoc.setY(81);
                if (spawnLoc.getBlock().getType().isSolid()) {
                    spawnLoc.setY(spawnLoc.getY() + 1);
                }

                spawnLoc.getChunk().load();

                var entity = world.spawn(spawnLoc, (Class<? extends LivingEntity>) targetMob.getEntityClass(), spawn -> {
                    if (spawn instanceof Mob mob) {
                        mob.setTarget(player);
                    }
                });

                if (entity instanceof Mob mob) {
                    mob.setTarget(player);
                    applyMobScaling(mob, difficulty);
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

        mob.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED,
                200,
                Math.min(3, difficulty / 2)
        ));

        mob.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH,
                200,
                Math.min(2, difficulty / 3)
        ));

        var attribute = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attribute != null) {
            double newHealth = attribute.getBaseValue() + (difficulty * 2);
            attribute.setBaseValue(newHealth);
            mob.setHealth(newHealth);
        }

        mob.setPersistent(false);
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
                    40,
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

        if (overworld == null || nether == null || end == null) return;

        org.bukkit.World targetWorld;

        switch (dimensionIndex) {
            case 0 -> targetWorld = nether;
            case 1 -> targetWorld = end;
            default -> targetWorld = overworld;
        }

        dimensionIndex = (dimensionIndex + 1) % 3;

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

            var origin = new Location(
                    currentLoc.getWorld(),
                    0, 81, 0
            );

            double offsetX = currentLoc.getX() - origin.getX();
            double offsetZ = currentLoc.getZ() - origin.getZ();

            var targetOrigin = new Location(
                    targetWorld,
                    0, 81, 0
            );

            var newLoc = targetOrigin.clone().add(offsetX, 0, offsetZ);
            newLoc.setY(81);

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
                            )
                    )
            );
        }

        Bukkit.broadcast(
                net.kyori.adventure.text.Component.text(
                        "Dimension changed to " + formatWorldName(targetWorld.getName())
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