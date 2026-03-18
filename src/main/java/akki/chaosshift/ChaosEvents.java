package akki.chaosshift;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Random;

public class ChaosEvents {

    private final Plugin plugin;
    private final Random random = new Random();
    private int taskId;

    public ChaosEvents(Plugin plugin) {
        this.plugin = plugin;
    }

    public void startChaos() {

        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            int event = random.nextInt(4);

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

    public void stopChaos() {
        Bukkit.getScheduler().cancelTask(taskId);
    }
}