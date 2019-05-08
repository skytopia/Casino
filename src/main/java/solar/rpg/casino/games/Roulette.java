package solar.rpg.casino.games;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import solar.rpg.casino.Main;
import solar.rpg.casino.data.GameType;
import solar.rpg.skyblock.island.Island;
import solar.rpg.skyblock.util.Utility;

/**
 * Roulette is a slot-like game with a singular win condition and reward.
 * The roulette will spin and stop in a random direction. If this direction
 * is the winning direction, the player wins the roulette.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
public class Roulette extends Game {

    /* A head to spin, and a button to activate the game. */
    private final Block HEAD, BUTTON;

    /* The direction the skull must land on to win the roulette. */
    private final BlockFace WINDIR;

    public Roulette(Main PLUGIN, World WORLD, BlockFace winDir, Location... locs) {
        super(PLUGIN, WORLD);
        WINDIR = winDir;
        HEAD = locs[0].getBlock();
        BUTTON = locs[1].getBlock();
    }

    @Override
    public GameType getGameType() {
        return GameType.ROULETTE;
    }

    @Override
    public String getName() {
        return "Roulette";
    }

    /**
     * Spins the roulette.

     * @param player The player who spun the roulette.
     */
    private void spin(Player player) {
        // Get the island the player belongs to.
        Island island = PLUGIN.cloud().islands().getIsland(player.getUniqueId());

        // Players must have an island before using the roulette machine.
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You need an island to do this!");
            return;
        }

        // Players using the roulette machine must have at least $5000 available.
        if (!PLUGIN.cloud().getEconomy().has(player, 5000)) {
            player.sendMessage(ChatColor.RED + "You need at least 5,000ƒ to play this!");
            return;
        }

        // If it is already in use, don't allow it to spin again.
        if (inUse) {
            player.sendMessage(ChatColor.RED + "Hold on! It's already spinning!");
            return;
        }

        // Withdraw funds, spin, and notify all island members.
        island.main().getEconomy().withdrawPlayer(player, 5000);
        island.actions().messageAll(player.getDisplayName() + ChatColor.GOLD + " spun the roulette machine!");
        inUse = true;

        // Choose a random amount of spins.
        int noOfSpins = 30 + RNG.nextInt(76);
        new BukkitRunnable() {
            int delay = 1;
            int spins = noOfSpins;

            public void run() {
                // Decrement delay time.
                delay -= 1;

                // If delay time has reached zero, set a new delay.
                if (delay <= 0) {
                    // The delay increases as the number of spins remaining decreases.
                    if (spins <= 5)
                        delay = 4;
                    else if (spins <= 30)
                        delay = 2;
                    else
                        delay = 1;
                } else
                    // Return here if delay has not reached zero.
                    return;

                // Decrement spins and spin the skull.
                spins -= 1;
                spinSkull();

                // Play a clicking sound and spawn particles above the head.
                WORLD.playSound(HEAD.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_TRIPWIRE_CLICK_ON, 3.0F, 3.0F);
                WORLD.spawnParticle(Particle.SLIME, HEAD.getLocation().add(0.5, 0.5, 0.5), RNG.nextInt(10) == 7 ? 0 : 1);

                // Determine reward and reset on last spin.
                if (spins == 0) {
                    determineReward(player);
                    inUse = false;
                    cancel();
                }
            }
        }.runTaskTimer(PLUGIN, 0L, 1L);
    }

    /**
     * Determines the reward based on the direction the skull is facing.
     *
     * @param pl The player who spun the roulette.
     */
    private void determineReward(OfflinePlayer pl) {
        Skull skull = (Skull) HEAD.getState();
        boolean won = skull.getRotation().equals(WINDIR);

        if (won) {
            PLUGIN.cloud().getEconomy().depositPlayer(pl.getName(), 15000);
            if (pl.isOnline()) {
                Utility.spawnFirework(pl.getPlayer().getLocation());
                pl.getPlayer().sendMessage(ChatColor.GOLD + "You win! " + ChatColor.RED + "15,000ƒ awarded!");
            }
        } else if (pl.isOnline())
            pl.getPlayer().sendMessage(ChatColor.GRAY + "The skull didn't land on the winning position...");
    }

    @EventHandler
    public void onSpin(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getWorld().equals(WORLD)) return;
        Location click = event.getClickedBlock().getLocation();
        if (click.equals(BUTTON.getLocation()))
            spin(event.getPlayer());
    }

    /**
     * Spins the skull one rotation clockwise.
     */
    private void spinSkull() {
        Skull sk = (Skull) HEAD.getState();
        BlockFace face = sk.getRotation();
        switch (face) {
            case NORTH:
                sk.setRotation(BlockFace.NORTH_EAST);
                break;
            case NORTH_EAST:
                sk.setRotation(BlockFace.EAST);
                break;
            case EAST:
                sk.setRotation(BlockFace.SOUTH_EAST);
                break;
            case SOUTH_EAST:
                sk.setRotation(BlockFace.SOUTH);
                break;
            case SOUTH:
                sk.setRotation(BlockFace.SOUTH_WEST);
                break;
            case SOUTH_WEST:
                sk.setRotation(BlockFace.WEST);
                break;
            case WEST:
                sk.setRotation(BlockFace.NORTH_WEST);
                break;
            case NORTH_WEST:
                sk.setRotation(BlockFace.NORTH);
                break;
        }
        sk.update(true);
    }
}