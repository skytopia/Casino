package solar.rpg.casino.games;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import solar.rpg.casino.Main;
import solar.rpg.casino.data.GameType;
import solar.rpg.skyblock.island.Island;
import solar.rpg.skyblock.util.Utility;

/**
 * Simple slots game. 1 in 6 chance to win more than you spent.
 * The rarer the ore, the more money is given as a reward.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
public class Slots extends Game {

    /* Three slot blocks, one activation button. */
    private final Block TOP, MIDDLE, BOTTOM, BUTTON;

    public Slots(Main PLUGIN, World WORLD, Location... locs) {
        super(PLUGIN, WORLD);
        TOP = locs[0].getBlock();
        MIDDLE = locs[1].getBlock();
        BOTTOM = locs[2].getBlock();
        BUTTON = locs[3].getBlock();
    }

    @Override
    public GameType getGameType() {
        return GameType.SLOTS;
    }

    @Override
    public String getName() {
        return "Single Slots";
    }

    /**
     * Undertakes the slot spinning procedure.
     * At the end, a prize is determined.
     *
     * @param player The player who spun the slots machine.
     */
    private void spin(Player player) {
        // Get the island the player belongs to.
        Island island = PLUGIN.cloud().islands().getIsland(player.getUniqueId());

        // Players must have an island before using the slots machine.
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You need an island to do this!");
            return;
        }

        // Players using the slots machine must have at least $5000 available.
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
        island.actions().messageAll(player.getDisplayName() + ChatColor.GOLD + " spun the slots machine!");
        inUse = true;

        // Choose a random amount of spins.
        int noOfSpins = 100 + RNG.nextInt(76);

        new BukkitRunnable() {
            /* Delay in between clicks. Goes slower as the slots reaches the end of the spin. */
            int delay;

            /* Amount of spins remaining before the slot stops. */
            int spins = noOfSpins;

            public void run() {
                // Decrement delay time.
                delay -= 1;

                // If delay time has reached zero, set a new delay.
                if (delay <= 0) {
                    // The delay increases as the number of spins remaining decreases.
                    if (spins <= 3)
                        delay = 4;
                    else if (spins <= 30)
                        delay = 2;
                    else
                        delay = 1;
                } else
                    // Return here if delay has not reached zero.
                    return;

                spins -= 1;

                // Rotate each slot block forward one, giving the top slot a random new block.
                BOTTOM.setType(MIDDLE.getType());
                MIDDLE.setType(TOP.getType());
                TOP.setType(genSlot());

                // Play a sound and spawn a particle.
                WORLD.playSound(TOP.getLocation().subtract(0.5, -0.5, 0.5), org.bukkit.Sound.BLOCK_TRIPWIRE_CLICK_ON, 2.0F, 2.0F);
                WORLD.spawnParticle(org.bukkit.Particle.CRIT_MAGIC, MIDDLE.getLocation().subtract(0.5, 0, 0), 1);

                // Once spins reaches zero, determine the player's reward and reset.
                if (spins == 0) {
                    determineReward(island, player, MIDDLE.getType());
                    inUse = false;
                    cancel();
                }
            }
        }.runTaskTimer(PLUGIN, 0L, 1L);
    }

    /**
     * Determines the reward to give the player after the slots have stopped spinning.
     *
     * @param is  The player's island.
     * @param pl  The player who spun the slots.
     * @param mat Material that the slots stopped on.
     */
    private void determineReward(Island is, Player pl, Material mat) {
        Utility.spawnFirework(pl.getLocation());
        String prizeMessage;
        int prizeAmount;
        switch (mat) {
            case POLISHED_DIORITE: {
                prizeMessage = ChatColor.GRAY + "DIORITE! " + ChatColor.GOLD + "You won 250ƒ!";
                prizeAmount = 250;
                break;
            }
            case COAL_ORE: {
                prizeMessage = ChatColor.BLACK + "COAL! " + ChatColor.GOLD + "You won 7,500ƒ!";
                prizeAmount = 7500;
                break;
            }
            case LAPIS_ORE: {
                prizeMessage = ChatColor.BLUE + "LAPIS! " + ChatColor.GOLD + "You won 10,000ƒ!";
                prizeAmount = 10000;
                break;
            }
            case REDSTONE_ORE: {
                prizeMessage = ChatColor.RED + "REDSTONE! " + ChatColor.GOLD + "You won 12,500ƒ!";
                prizeAmount = 12500;
                break;
            }
            case IRON_ORE: {
                prizeMessage = ChatColor.DARK_GRAY + "IRON! " + ChatColor.GOLD + "You won 15,000ƒ!";
                prizeAmount = 15000;
                break;
            }
            case GOLD_ORE: {
                prizeMessage = ChatColor.GOLD + "GOLD! " + ChatColor.GOLD + "You won 20,000ƒ!";
                prizeAmount = 20000;
                break;
            }
            case DIAMOND_ORE: {
                Bukkit.broadcastMessage(pl.getDisplayName() + ChatColor.GOLD + " just won the Mini Jackpot on the slots machine! " + ChatColor.RED + "(/warp casino)");
                prizeMessage = ChatColor.AQUA + "DIAMOND! " + ChatColor.GOLD + "You won 50,000ƒ!";
                prizeAmount = 50000;
                break;
            }
            case EMERALD_ORE: {
                Bukkit.broadcastMessage(pl.getDisplayName() + ChatColor.GOLD + " just won the JACKPOT on the slots machine! " + ChatColor.RED + "(/warp casino)");
                prizeMessage = ChatColor.GREEN + "EMERALD!!! " + ChatColor.GOLD + "You won 100,000ƒ!";
                prizeAmount = 100000;
                break;
            }
            default:
                throw new IllegalStateException("Unsupported block type?");
        }
        // Use Skyblock's economy hook to credit money to player's account.
        is.actions().messageAll(prizeMessage);
        is.main().getEconomy().depositPlayer(pl, prizeAmount);
    }

    /**
     * Randomly generates a slot material for the slots machine.
     * <p>
     * 1 out of 6 times, a random ore will be generated using the Skyblocks'
     * the same probability function as regular in-game ore generation.
     *
     * @return Randomly generated slot material.
     */
    private Material genSlot() {
        if (RNG.nextInt(6) != 1)
            return Material.POLISHED_DIORITE;
        return PLUGIN.cloud().ore().genOre(RNG.nextInt(100) + 1);
    }

    @EventHandler
    public void onSpin(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getWorld().equals(WORLD)) return;
        Location click = event.getClickedBlock().getLocation();
        if (click.equals(BUTTON.getLocation()))
            spin(event.getPlayer());
    }
}