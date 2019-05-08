package solar.rpg.casino.games;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import solar.rpg.casino.Main;
import solar.rpg.casino.data.GameType;

import java.util.*;

public abstract class Game implements Listener {

    /* Reference to JavaPlugin. */
    final Main PLUGIN;

    /* Random number generator. */
    final Random RNG;

    /* Denotes whether this game is in use or not. */
    boolean inUse = false;
    UUID using;

    /* What world the Game is in. */
    final World WORLD;

    /* Keeps track of cooldowns for users. */
    private final Set<UUID> cooldown;

    Game(Main PLUGIN, World WORLD) {
        this.PLUGIN = PLUGIN;
        this.RNG = PLUGIN.getRNG();
        this.WORLD = WORLD;
        cooldown = new HashSet<>();
        PLUGIN.getServer().getPluginManager().registerEvents(this, PLUGIN);
    }

    public abstract GameType getGameType();

    public abstract String getName();

    protected void setCooldown(UUID user) {
        if(!cooldown.contains(user)) {
            cooldown.add(user);
            new BukkitRunnable() {
                public void run() {

                }
            }.runTaskLater(PLUGIN, 30 * 20);
        }
    }

    protected boolean checkBlock(Location loc, Material type) {
        return loc.getBlock().getType().equals(type);
    }

    public boolean isInUse(){
        return inUse;
    }
}
