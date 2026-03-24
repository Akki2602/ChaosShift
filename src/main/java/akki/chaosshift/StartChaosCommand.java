package akki.chaosshift;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartChaosCommand implements CommandExecutor {

    private final GameManager gameManager;

    public StartChaosCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            gameManager.startGame();
            sender.sendMessage("Chaos game started from console.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("You don't have permission.");
            return true;
        }

        gameManager.startGame();
        player.sendMessage("Chaos game started.");
        return  true;
    }
}
