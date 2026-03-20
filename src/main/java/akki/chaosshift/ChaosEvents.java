package akki.chaosshift;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
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

    public ChaosEvents(Plugin plugin) {
        this.plugin = plugin;
    }

    public void startChaos() {

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            int event = random.nextInt(5);

            switch (event) {

                case 0:
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));
                        p.sendMessage("§aEverything changed: Speed Boost!");
                    }
                    break;

                case 1:
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, 3));
                        p.sendMessage("§bEverything changed: Low gravity!");
                    }
                    break;

                case 2:
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        World world = p.getWorld();
                        world.setTime(random.nextBoolean() ? 1000 : 18000);
                    }
                    Bukkit.broadcast(
                            net.kyori.adventure.text.Component.text("Everything changed: Time flipped!")
                    );
                    break;

                case 3:
                    spawnChasingMobs();
                    break;

                case 4:
                    teleportSwap();
                    break;
            }

        }, 0L, 600L).getTaskId();
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