package akki.chaosshift;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

        var player = event.getEntity();

        org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ChaosShift"),
                () -> player.spigot().respawn(),
                1L
        );

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

    @org.bukkit.event.EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {

        var player = event.getPlayer();

        if (event.getItem() == null) return;

        var item = event.getItem();

        // DEBUG (optional)
        player.sendMessage("Clicked: " + item.getType());

        // 🧭 COMPASS → open menu
        if (item.getType() == org.bukkit.Material.COMPASS) {

            if (item.getItemMeta() != null &&
                    "§aKit Selection".equals(item.getItemMeta().getDisplayName())) {

                event.setCancelled(true);

                player.sendMessage("Opening kit menu..."); // debug

                player.openInventory(KitMenu.createMenu(gameManager));
                return;
            }
        }

        // 💎 EMERALD → start game
        if (item.getType() == org.bukkit.Material.EMERALD &&
                item.getItemMeta() != null &&
                "§aStart Game".equals(item.getItemMeta().getDisplayName())) {

            if (!player.isOp()) {
                player.sendMessage("Not OP");
                return;
            }

            event.setCancelled(true);

            player.sendMessage("Starting game..."); // debug

            gameManager.startGame();
        }
    }

    @EventHandler
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
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

        // Only care if game running
        if (gameManager.getState() != GameState.RUNNING) return;

        // Already landed
        if (gameManager.hasLanded(player.getUniqueId())) return;

        // Check if player is on ground
        if (player.isOnGround()) {

            gameManager.playerLanded(player.getUniqueId());
        }
    }

}
