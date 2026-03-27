package akki.chaosshift;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StopChaosCommand implements CommandExecutor {

    private final GameManager gameManager;

    public StopChaosCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player player && !player.isOp()) {
            player.sendMessage("No permission.");
            return true;
        }

        gameManager.forceStopGame();

        sender.sendMessage("Chaos game stopped.");
        return true;
    }
}