package akki.chaosshift;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerListener implements Listener{

    private final GameManager gameManager;

    public PlayerListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        var player = event.getEntity();

        gameManager.playerDied(player.getUniqueId());

    }

}
