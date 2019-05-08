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

public class TripleSlots extends Game {

    /* 3 columns of single slots, one button. */
    private final Block TOP1;
    private final Block TOP2;
    private final Block TOP3;
    private final Block MIDDLE1;
    private final Block MIDDLE2;
    private final Block MIDDLE3;
    private final Block BOTTOM1;
    private final Block BOTTOM2;
    private final Block BOTTOM3;
    private final Block BUTTON;

    public TripleSlots(Main PLUGIN, World WORLD, Location... locs) {
        super(PLUGIN, WORLD);
        TOP1 = locs[0].getBlock();
        TOP2 = locs[1].getBlock();
        TOP3 = locs[2].getBlock();
        MIDDLE1 = locs[3].getBlock();
        MIDDLE2 = locs[4].getBlock();
        MIDDLE3 = locs[5].getBlock();
        BOTTOM1 = locs[6].getBlock();
        BOTTOM2 = locs[7].getBlock();
        BOTTOM3 = locs[8].getBlock();
        BUTTON = locs[9].getBlock();
    }

    @Override
    public GameType getGameType() {
        return GameType.TRIPLESLOTS;
    }

    @Override
    public String getName() {
        return "Triple Slots";
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

        // Players must have an island before using the triple slots machine.
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You need an island to do this!");
            return;
        }

        // Players using the triple slots machine must have at least $5000 available.
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
        island.actions().messageAll(player.getDisplayName() + ChatColor.GOLD + " spun the triple slots machine!");
        inUse = true;

        // Choose a random amount of spins.
        int noOfSpins = 100 + RNG.nextInt(76);

        new BukkitRunnable() {
            int delay = 1;
            int spins = noOfSpins;

            boolean stop1 = false;
            boolean stop2 = false;

            public void run() {
                // Decrement delay time.
                delay -= 1;

                // If delay time has reached zero, set a new delay.
                if (delay <= 0) {
                    // The delay increases as the number of spinning slots decreases.
                    if (stop2)
                        delay = 4;
                    else if (stop1)
                        delay = 2;
                    else
                        delay = 1;
                } else
                    // Return here if delay has not reached zero.
                    return;

                spins -= 1;

                // Ignore the first column of slots if it has been marked as no longer spinning.
                if (!stop1) {
                    WORLD.playSound(MIDDLE1.getLocation().subtract(0.5, 0.5, 0.5), Sound.BLOCK_TRIPWIRE_CLICK_ON, 2.0F, 2.0F);
                    BOTTOM1.setType(MIDDLE1.getType());
                    MIDDLE1.setType(TOP1.getType());
                    TOP1.setType(genSlot());
                }

                // Ignore the second column of slots if it has been marked as no longer spinning.
                if (!stop2) {
                    WORLD.playSound(MIDDLE2.getLocation().subtract(0.5, 0.5, 0.5), Sound.BLOCK_TRIPWIRE_CLICK_ON, 2.0F, 2.0F);
                    BOTTOM2.setType(MIDDLE2.getType());
                    MIDDLE2.setType(TOP2.getType());
                    TOP2.setType(genSlot());
                }

                // Always spin the third row until completion.
                WORLD.playSound(MIDDLE3.getLocation().subtract(0.5, 0.5, 0.5), Sound.BLOCK_TRIPWIRE_CLICK_ON, 2.0F, 2.0F);
                BOTTOM3.setType(MIDDLE3.getType());
                MIDDLE3.setType(TOP3.getType());
                TOP3.setType(genSlot());

                if (spins == 0 && !stop1) {
                    // Stop the first row, continue spinning.
                    stop1 = true;
                    spins = 5 + RNG.nextInt(15);
                    WORLD.playSound(MIDDLE1.getLocation().subtract(0.5, 0.5, 0.5), Sound.BLOCK_NOTE_BASS, 3.0F, 0.75F);
                } else if (spins == 0 && !stop2) {
                    stop2 = true;
                    spins = 5 + RNG.nextInt(5);
                    WORLD.playSound(MIDDLE2.getLocation().subtract(0.5, 0.5, 0.5), Sound.BLOCK_NOTE_BASS, 3.0F, 0.75F);
                } else if (spins == 0) {
                    WORLD.playSound(MIDDLE3.getLocation().subtract(0.5, 0.5, 0.5), Sound.BLOCK_NOTE_BASS, 3.0F, 0.75F);
                    determineReward(island, player);
                    inUse = false;
                    cancel();
                }
            }
        }.runTaskTimer(this.PLUGIN, 0L, 1L);
    }

    /**
     * Determines the reward to give the player after the slots have stopped spinning.
     *
     * @param is The player's island.
     * @param pl The player who spun the triple slots.
     */
    private void determineReward(Island is, Player pl) {
        // Determine how many of the same ore are in a row.
        int inARow = 1;
        Material trueMiddle = MIDDLE2.getType();
        if (trueMiddle.equals(MIDDLE1.getType())) inARow++;
        if (trueMiddle.equals(MIDDLE3.getType())) inARow++;

        String oreMessage;
        int rawPrizeAmount;

        // Determine prize based on what ore, first.
        switch (trueMiddle) {
            case COAL_ORE: {
                oreMessage = ChatColor.BLACK + "COAL!";
                rawPrizeAmount = 25000;
                break;
            }
            case LAPIS_ORE: {
                oreMessage = ChatColor.BLUE + "LAPIS!";
                rawPrizeAmount = 35000;
                break;
            }
            case REDSTONE_ORE: {
                oreMessage = ChatColor.RED + "REDSTONE!";
                rawPrizeAmount = 45000;
                break;
            }
            case IRON_ORE: {
                oreMessage = ChatColor.DARK_GRAY + "IRON!";
                rawPrizeAmount = 75000;
                break;
            }
            case GOLD_ORE: {
                oreMessage = ChatColor.GOLD + "GOLD!";
                rawPrizeAmount = 100000;
                break;
            }
            case DIAMOND_ORE: {
                oreMessage = ChatColor.AQUA + "DIAMOND!";
                rawPrizeAmount = 250000;
                break;
            }
            case EMERALD_ORE: {
                oreMessage = ChatColor.GREEN + "EMERALD!!!";
                rawPrizeAmount = 1500000;
                break;
            }
            default:
                throw new IllegalStateException("Unsupported block type?");
        }

        String finalMessage;
        int actualPrizeAmount;

        // Determine actual prize amount and message to send to the island.
        switch (inARow) {
            case 1:
                finalMessage = ChatColor.GOLD + "You did not get a two or three in a row. " + ChatColor.RED + "Try again!";
                actualPrizeAmount = 0;
                break;
            case 2:
            case 3:
                Utility.spawnFirework(pl.getLocation());
                actualPrizeAmount = (int) (rawPrizeAmount * (inARow == 3 ? 1 : 0.33));
                finalMessage = String.format(ChatColor.GOLD + "You got a " + ChatColor.RED + "%s-in-a-row" + ChatColor.GOLD + " of " + oreMessage + ChatColor.GOLD + " You won %sƒ!", inARow, actualPrizeAmount);
                switch (trueMiddle) {
                    case DIAMOND_ORE:
                        Bukkit.broadcastMessage(String.format(pl.getDisplayName() + ChatColor.GOLD + " got a %s in a row of Diamond on the triple slots! " + ChatColor.RED + "(/warp casino)", inARow));
                        break;
                    case EMERALD_ORE:
                        Bukkit.broadcastMessage(String.format(pl.getDisplayName() + ChatColor.GOLD + " got a %s in a row of EMERALD on the triple slots!!! " + ChatColor.RED + "(/warp casino)", inARow));
                        break;
                }
                break;
            default:
                throw new IllegalStateException("Invalid in-a-row value on triple slots");
        }

        // Use Skyblock's economy hook to credit money to player's account.
        is.actions().messageAll(finalMessage);
        is.main().getEconomy().depositPlayer(pl, actualPrizeAmount);
    }

    /**
     * Randomly generates a slot material for the slots machine.
     * <p>
     * A random ore will be generated using the Skyblocks' the same
     * probability function as regular in-game ore generation.
     *
     * @return Randomly generated slot material.
     */
    private Material genSlot() {
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