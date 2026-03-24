package akki.chaosshift;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerListener implements Listener{

    private final GameManager gameManager;

    public PlayerListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        var victim = event.getEntity();
        var killer = victim.getKiller();

        // Handle elimination
        gameManager.playerDied(victim.getUniqueId());

        // ✅ Kill reward system
        if (killer != null) {

            // Heal player (max health)
            double maxHealth = killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            killer.setHealth(Math.min(maxHealth, killer.getHealth() + 6));

            // Optional: small bonus effects
            killer.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.REGENERATION,
                    60,
                    1
            ));

            // Sound
            killer.playSound(
                    killer.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                    1f,
                    1.2f
            );

            // Title
            killer.showTitle(
                    net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text(
                                    "Eliminated!",
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#ff0000")
                            ),
                            net.kyori.adventure.text.Component.text(
                                    "+ Health Restored",
                                    net.kyori.adventure.text.format.TextColor.fromHexString("#00ff00")
                            )
                    )
            );

            // Chat message
            killer.sendMessage(
                    net.kyori.adventure.text.Component.text(
                            "You gained health for eliminating " + victim.getName()
                    )
            );
        }
    }

    @org.bukkit.event.EventHandler
    public void onFallDamage(org.bukkit.event.entity.EntityDamageEvent event) {

        if (!(event.getEntity() instanceof  org.bukkit.entity.Player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {

        var player = event.getPlayer();

        if (!gameManager.isAlive(player.getUniqueId())) {
            player.setGameMode(GameMode.SPECTATOR);

            event.setRespawnLocation(new Location(
                    Bukkit.getWorld("world"), 0, 131, 0
            ));
        }
    }

    @EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {

        var player = event.getPlayer();

        if (event.getItem() == null) return;

        if (event.getItem().getType() != Material.COMPASS) return;

        if (!player.isOp()) return;

        event.setCancelled(true);

        gameManager.startGame();
    }

}
