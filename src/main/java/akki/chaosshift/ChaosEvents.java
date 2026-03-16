package akki.chaosshift;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class ChaosEvents {

    private final Plugin plugin;
    private final Random random = new Random();

    public ChaosEvents(Plugin plugin) {
        this.plugin = plugin;
    }

    public void startChaos() {

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            int event = random.nextInt(3);

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
                    World world = Bukkit.getWorlds().get(0);
                    world.setTime(random.nextBoolean() ? 1000 : 18000);
                    Bukkit.broadcastMessage("§eEverything changed: Time flipped!");
                    break;
            }

        }, 0L, 600L); // 600 ticks = 30 seconds
    }
}