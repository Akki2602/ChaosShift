package akki.chaosshift;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;



public class PlayerListener implements Listener{

    private final GameManager gameManager;
    private final org.bukkit.plugin.Plugin plugin;


    public PlayerListener(GameManager gameManager, org.bukkit.plugin.Plugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof org.bukkit.entity.Player victim)) return;

        event.setDamage(event.getDamage() * 0.85);

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) return;

        if (gameManager.getState() != GameState.RUNNING) return;

        if (!gameManager.isAlive(victim.getUniqueId())) return;

        if (victim.getGameMode() == GameMode.SPECTATOR) return;

        double finalHealth = victim.getHealth() - event.getFinalDamage();

        if (finalHealth <= 0) {

            event.setCancelled(true);

            var killer = victim.getKiller();

            if (killer != null && killer != victim) {

                double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();

                killer.setHealth(Math.min(maxHealth, killer.getHealth() + 4));

                killer.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.REGENERATION,
                        60,
                        1
                ));

                killer.playSound(
                        killer.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP,
                        1f,
                        1.2f
                );

                killer.showTitle(
                        net.kyori.adventure.title.Title.title(
                                net.kyori.adventure.text.Component.text("Eliminated!"),
                                net.kyori.adventure.text.Component.text("+ Health Restored")
                        )
                );

                killer.sendMessage(
                        net.kyori.adventure.text.Component.text(
                                "You gained health for eliminating " + victim.getName()
                        )
                );
            }

            gameManager.playerDied(victim.getUniqueId());

            victim.setHealth(victim.getAttribute(Attribute.MAX_HEALTH).getValue());
            victim.setFoodLevel(20);
            victim.setFireTicks(0);

            victim.teleport(new Location(
                    Bukkit.getWorld("world"), 0, 131, 0
            ));

            victim.playSound(
                    victim.getLocation(),
                    Sound.ENTITY_WITHER_DEATH,
                    1f,
                    1f
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



    @org.bukkit.event.EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {

        var player = event.getPlayer();

        if (event.getItem() == null) return;

        var item = event.getItem();

        if (item.getType() == org.bukkit.Material.COMPASS) {

            if (item.getItemMeta() != null &&
                    "§aKit Selection".equals(item.getItemMeta().getDisplayName())) {

                event.setCancelled(true);


                player.openInventory(KitMenu.createMenu(gameManager));
                return;
            }
        }

        if (item.getType() == org.bukkit.Material.EMERALD &&
                item.getItemMeta() != null &&
                "§aStart Game".equals(item.getItemMeta().getDisplayName())) {

            if (!player.isOp()) {
                player.sendMessage("Not OP");
                return;
            }

            event.setCancelled(true);

            player.sendMessage("Starting game...");

            gameManager.startGame();
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onDifficultyChange(org.bukkit.event.server.ServerLoadEvent event) {

        for (var world : org.bukkit.Bukkit.getWorlds()) {
            world.setDifficulty(org.bukkit.Difficulty.HARD);
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {

        if (event.getEntity() instanceof org.bukkit.entity.Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {

        if (event.getView().getTitle().equals("Select Kit")) {

            event.setCancelled(true);

            var player = (org.bukkit.entity.Player) event.getWhoClicked();
            var item = event.getCurrentItem();

            if (item == null) return;

            switch (item.getType()) {

                case IRON_SWORD -> {
                    gameManager.voteKit(player.getUniqueId(), KitType.WARRIOR);
                    player.sendMessage("Voted: Warrior");

                    player.openInventory(KitMenu.createMenu(gameManager));
                }

                case BOW -> {
                    gameManager.voteKit(player.getUniqueId(), KitType.ARCHER);
                    player.sendMessage("Voted: Archer");

                    player.openInventory(KitMenu.createMenu(gameManager));
                }

                case SHIELD -> {
                    gameManager.voteKit(player.getUniqueId(), KitType.TANK);
                    player.sendMessage("Voted: Tank");

                    player.openInventory(KitMenu.createMenu(gameManager));
                }
            }

        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        var player = event.getPlayer();

        player.getInventory().clear();

        var compass = new org.bukkit.inventory.ItemStack(Material.COMPASS);
        var meta = compass.getItemMeta();
        meta.setDisplayName("§aKit Selection");
        compass.setItemMeta(meta);

        player.getInventory().addItem(compass);

        if (player.isOp()) {
            var emerald = new org.bukkit.inventory.ItemStack(Material.EMERALD);
            var meta_e = emerald.getItemMeta();
            meta_e.setDisplayName("§aStart Game");
            emerald.setItemMeta(meta_e);

            player.getInventory().addItem(emerald);
        }
    }

    @org.bukkit.event.EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {

        var player = event.getPlayer();

        if (gameManager.getState() != GameState.RUNNING) return;

        if (gameManager.hasLanded(player.getUniqueId())) return;

        if (player.isOnGround()) {

            gameManager.playerLanded(player.getUniqueId());
        }
    }

}
