package solar.rpg.casino.games;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import solar.rpg.casino.Main;
import solar.rpg.casino.data.GameType;
import solar.rpg.skyblock.island.Island;
import solar.rpg.skyblock.util.Utility;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple slot-like game with no rewards.
 * Spins for a random amount of time, then produces
 * a "Hotness" rating for the user in question at random.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
public class LoveTester extends Game {

    /* 2 slots, player head, sign, and button. */
    private final Block BOTTOM, TOP, HEAD, SIGN, BUTTON;

    /* A list of people who will always receive the "HOT" rating! */
    private static final Set<String> ALWAYS_HOT;

    static {
        ALWAYS_HOT = new HashSet<>();
        ALWAYS_HOT.add("ILavaYou");
        ALWAYS_HOT.add("JacquiRose");
    }

    public LoveTester(Main PLUGIN, World WORLD, Location... locs) {
        super(PLUGIN, WORLD);
        BOTTOM = locs[0].getBlock();
        TOP = locs[1].getBlock();
        HEAD = locs[2].getBlock();
        SIGN = locs[3].getBlock();
        BUTTON = locs[4].getBlock();
    }

    @Override
    public GameType getGameType() {
        return GameType.LOVETESTER;
    }

    @Override
    public String getName() {
        return "Hot or Not Tester";
    }

    /**
     * Spins the machine. Lands on a random result.
     *
     * @param player The player who spun the machine.
     */
    private void spin(Player player) {
        // Get the island the player belongs to.
        Island island = PLUGIN.cloud().islands().getIsland(player.getUniqueId());

        // Players must have an island before using the love tester
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You need an island to do this!");
            return;
        }

        // Players using the love tester must have at least $1500 available.
        if (!PLUGIN.cloud().getEconomy().has(player, 1500)) {
            player.sendMessage(ChatColor.RED + "You need at least 1,500ƒ to use this!");
            return;
        }

        // If it is already in use, don't allow a second test to take place.
        if (inUse) {
            player.sendMessage(ChatColor.RED + "Hold on! It's already testing someone!");
            return;
        }

        // Withdraw funds, spin, and notify all island members.
        island.main().getEconomy().withdrawPlayer(player, 1500);
        island.actions().messageAll(player.getDisplayName() + ChatColor.GOLD + " tried the Hot or Not test!");
        inUse = true;

        // Choose a random amount of spins.
        int noOfSpins = 100 + RNG.nextInt(76);

        // Update skull to the skin of the user.
        Skull skull = (Skull) HEAD.getState();
        skull.setOwningPlayer(player);
        skull.update(true);

        // Update sign to the name of the user.
        Sign sign = (Sign) SIGN.getState();
        sign.setLine(1, player.getName());
        sign.setLine(2, "Testing...");
        sign.update(true);

        new BukkitRunnable() {
            int spins = noOfSpins;

            public void run() {
                spins -= 1;

                // Set the block of the bottom row to the top row.
                TOP.setType(BOTTOM.getType());

                // Regenerate a new bottom row block.
                // Set the 2nd last spin to Magma if the player is in the 'Always Hot' list of players!
                ItemStack gen = spins == 1 && ALWAYS_HOT.contains(player.getName()) ? new ItemStack(Material.MAGMA_BLOCK, 1) : genSlot();
                BOTTOM.setType(gen.getType());

                // Play a clicking sound and spawn hearts above the slot rows.
                WORLD.playSound(TOP.getLocation().subtract(0.5, -0.5, 0.5), Sound.BLOCK_TRIPWIRE_CLICK_ON, 2.0F, 2.0F);
                WORLD.spawnParticle(Particle.HEART, BOTTOM.getLocation().add(0.5, 2, 0.5), RNG.nextBoolean() ? 0 : RNG.nextBoolean() ? 1 : 0);

                // Calculate results if this is the last spin!
                if (spins == 0) {
                    // Translate block data into result, and notify whoever necessary!
                    String result = translate(TOP.getType());
                    sign.setLine(2, result);
                    sign.update(true);
                    if (result.equals("HOT!!!"))
                        Bukkit.broadcastMessage(player.getDisplayName() + ChatColor.GOLD + " is HOT STUFF! " + ChatColor.RED + "(/warp casino)");
                    else
                        player.sendMessage(ChatColor.GOLD + "You look like a " + ChatColor.RED + "\"" + result + "\"" + ChatColor.GOLD + " to me!");
                    Utility.spawnFirework(player.getLocation());

                    // Reset the spinner for next usage.
                    inUse = false;
                    cancel();
                }
            }
        }.runTaskTimer(PLUGIN, 0L, 1L);
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
     * Translates block data into a "Hot or Not" result.
     *
     * @param type The material type.
     * @return The "Hot or Not" result.
     */
    private String translate(Material type) {
        switch (type) {
            case STONE:
                return "Stone Cold";
            case MAGMA_BLOCK:
                return "HOT!!!";
            case LIGHT_GRAY_WOOL:
                return "Not";
            case GRAY_WOOL:
                return "Average";
            case PINK_WOOL:
                return "Cute!";
            case RED_WOOL:
                return "Beautiful!";
            default:
                // This value should never be a possibility.
                return "Ugly!";
        }
    }

    /**
     * @return A new block value for the next slot.
     */
    private ItemStack genSlot() {
        switch (RNG.nextInt(6)) {
            case 0:
                return new ItemStack(Material.STONE);
            case 1:
                return new ItemStack(Material.LIGHT_GRAY_WOOL);
            case 2:
                return new ItemStack(Material.GRAY_WOOL);
            case 3:
                return new ItemStack(Material.PINK_WOOL);
            case 4:
                return new ItemStack(Material.RED_WOOL);
            default:
                return new ItemStack(Material.MAGMA_BLOCK);
        }
    }
}