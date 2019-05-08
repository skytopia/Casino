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
import java.util.UUID;

/**
 * Simple random chance game with no rewards.
 * Spins for a random amount of time, then produces
 * a "Compatibility Rating" for a set of 2 players.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
public class CompatibilityTester extends Game {

    /* Array of display blocks, 2 heats, 2 buttons, one sign. */
    private final Block[] BLOCKS;
    private final Block HEAD1, HEAD2, BUTTON1, BUTTON2, SIGN;

    /* A list of couples who will always receive a "Perfect Match". */
    private static final Set<String[]> ALWAYS_PERFECT;

    static {
        ALWAYS_PERFECT = new HashSet<>();
        ALWAYS_PERFECT.add(new String[]{"ILavaYou", "JacquiRose"});
    }

    /* Keep track of who has pressed what button. */
    private UUID button1;
    private UUID button2;

    public CompatibilityTester(Main PLUGIN, World WORLD, Location[] blocks, Location... locs) {
        super(PLUGIN, WORLD);
        HEAD1 = locs[0].getBlock();
        HEAD2 = locs[1].getBlock();
        BUTTON1 = locs[2].getBlock();
        BUTTON2 = locs[3].getBlock();
        SIGN = locs[4].getBlock();

        // Convert array of locations into array of blocks.
        BLOCKS = new Block[blocks.length];
        for (int i = 0; i < blocks.length; i++)
            BLOCKS[i] = blocks[i].getBlock();
    }

    @Override
    public GameType getGameType() {
        return GameType.COMPATIBILITY;
    }

    @Override
    public String getName() {
        return "Compatibility Tester";
    }

    /**
     * Checks that the player is eligible to use this machine.
     *
     * @param player The player who is trying to use the machine.
     * @return True, if the player is eligible.
     */
    private boolean check(Player player) {
        Island island = PLUGIN.cloud().islands().getIsland(player.getUniqueId());

        // Players must have an island before using the compatibility tester.
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You need an island to do this!");
            return false;
        }

        // Players using the compatibility tester must have at least $1500 available.
        if (!island.main().getEconomy().has(player, 2500)) {
            player.sendMessage(ChatColor.RED + "You need at least 2,500ƒ to use this!");
            return false;
        }

        // If it is already in use, don't allow a second test to take place.
        if (this.inUse) {
            player.sendMessage(ChatColor.RED + "Hold on! It's already testing people!");
            return false;
        }

        // Withdraw funds and mark as eligible.
        island.main().getEconomy().withdrawPlayer(player, 2500);
        return true;
    }

    /**
     * Procedures undertaken when a player presses button #1.
     *
     * @param player The player who pressed button #1.
     */
    private void b1(Player player) {
        // Check that the person on the other button is not them. That's... sad.
        if (button2 != null && button2.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nice try homeboy, but you can't test yourself!");
            return;
        }

        // Check that this button has not already been pressed.
        if (button1 == null) {
            // Check that they are eligible.
            if (!check(player)) return;

            // Set them as the person who pressed button 1.
            button1 = player.getUniqueId();

            // Update sign.
            Sign sign = (Sign) SIGN.getState();
            sign.setLine(1, player.getName());

            // Update skull.
            Skull skull = (Skull) HEAD1.getState();
            skull.setOwningPlayer(player);
            skull.update(true);

            // Check if the other button has also been pressed.
            if (button2 == null) {
                // It hasn't, so we will need to wait for another player to press the button.
                player.sendMessage(ChatColor.RED + "Waiting on another player..");

                // Reset skull from previous test.
                Skull skull2 = (Skull) HEAD2.getState();
                skull2.setOwner("MHF_Skeleton");
                skull2.update(true);

                // Clear out previous sign information, add placeholders.
                sign.setLine(2, "???");
                sign.setLine(3, "Waiting...");
            } else {
                // Do the compatibility test!
                sign.setLine(3, "Testing...");
                go();
            }
            sign.update(true);
        } else
            player.sendMessage(ChatColor.RED + "Someone has already pushed the button!");
    }

    /**
     * Procedures undertaken when a player presses button #2.
     *
     * @param player The player who pressed button #2.
     */
    private void b2(Player player) {
        // Check that the person on the other button is not them. That's... sad.
        if (button1 != null && button1.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nice try homeboy, but you can't test yourself!");
            return;
        }

        // Check that this button has not already been pressed.
        if (button2 == null) {
            // Check that they are eligible.
            if (!check(player)) return;

            // Set them as the person who pressed button 2.
            button2 = player.getUniqueId();

            // Update sign.
            Sign sign = (Sign) SIGN.getState();
            sign.setLine(2, player.getName());

            // Update skull.
            Skull skull = (Skull) HEAD2.getState();
            skull.setOwningPlayer(player);
            skull.update(true);

            // Check if the other button has also been pressed.
            if (button1 == null) {
                // It hasn't, so we will need to wait for another player to press the button.
                player.sendMessage(ChatColor.RED + "Waiting on another player..");

                // Reset skull from previous test.
                Skull skull2 = (Skull) HEAD1.getState();
                skull2.setOwner("MHF_Skeleton");
                skull2.update(true);

                // Clear out previous sign information, add placeholders.
                sign.setLine(1, "???");
                sign.setLine(3, "Waiting...");
            } else {
                // Do the compatibility test!
                sign.setLine(3, "Testing...");
                go();
            }
            sign.update(true);
        } else {
            player.sendMessage(ChatColor.RED + "Someone has already pushed the button!");
        }
    }

    /**
     * Once both buttons have been pressed by 2 different players,
     * the compatibility test can be undertaken.
     */
    private void go() {
        // Get players who pressed the buttons.
        OfflinePlayer p1 = Bukkit.getOfflinePlayer(button1);
        OfflinePlayer p2 = Bukkit.getOfflinePlayer(button2);
        inUse = true;

        // Generate a random number of spins to undertake.
        int noOfSpins = 100 + RNG.nextInt(76);

        new BukkitRunnable() {
            int spins = noOfSpins;

            public void run() {
                this.spins -= 1;

                // Generate a new block type to update the 4 decoration blocks.
                // Set the last spin to Magma if the couple is in the 'Always Perfect Match' list of couples!
                ItemStack gen = spins == 0 && isAlwaysPerfectMatch(p1.getName(), p2.getName()) ? new ItemStack(Material.MAGMA, 1) : genSlot();
                for (Block block : BLOCKS) {
                    block.setType(gen.getType());
                    block.setData(gen.getData().getData());
                }

                // Play a clicking sound and spawn hearts above the slot rows.
                WORLD.playSound(SIGN.getLocation().subtract(0.5, -0.5, 0.5), Sound.BLOCK_TRIPWIRE_CLICK_ON, 2.0F, 2.0F);
                WORLD.spawnParticle(Particle.HEART, SIGN.getLocation().add(0.5, 2, 0.5), RNG.nextBoolean() ? 0 : RNG.nextInt(4) == 1 ? 1 : 0);

                if (spins == 0) {
                    // Translate block data into result.
                    String result = translateData(gen.getType(), gen.getData().getData());

                    // Update sign.
                    Sign sign = (Sign) SIGN.getState();
                    sign.setLine(3, result);
                    sign.update(true);

                    // Produce message result to show to whoever necessary.
                    String message = (p1.isOnline() ? p1.getPlayer().getDisplayName() : p1.getName()) + ChatColor.RED + " + ";
                    message += (p2.isOnline() ? p2.getPlayer().getDisplayName() : p2.getName()) + " = " + ChatColor.GOLD + result;

                    if (result.equals("Perfect Match!"))
                        Bukkit.broadcastMessage(message + ChatColor.RED + " (/warp casino)");
                    else {
                        // If it's a non-Perfect Match, then send the message to whoever is still online.
                        if (p1.isOnline()) {
                            p1.getPlayer().sendMessage(message);
                            Utility.spawnFirework(p1.getPlayer().getLocation());
                        }
                        if (p2.isOnline()) {
                            p2.getPlayer().sendMessage(message);
                            Utility.spawnFirework(p2.getPlayer().getLocation());
                        }
                    }

                    // Reset the tester for next usage.
                    inUse = false;
                    button1 = null;
                    button2 = null;
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
        if (click.equals(BUTTON1.getLocation()))
            b1(event.getPlayer());
        else if (click.equals(BUTTON2.getLocation()))
            b2(event.getPlayer());
    }

    /**
     * @param name1 First candidate.
     * @param name2 Second candidate.
     * @return True, if the two names exist in an "Always Perfect Match" couple.
     */
    private boolean isAlwaysPerfectMatch(String name1, String name2) {
        for (String[] couples : ALWAYS_PERFECT)
            if ((couples[0].equals(name1) || couples[0].equals(name2)) && (couples[1].equals(name1) || couples[1].equals(name2)))
                return true;
        return false;
    }

    /**
     * Translates block data into a compatibility result.
     *
     * @param type The material type.
     * @param data Any data attached.
     * @return The compatibility result.
     */
    private String translateData(Material type, byte data) {
        switch (data) {
            case 0:
                if (type.equals(Material.STONE))
                    return "Incompatible...";
                else if (type.equals(Material.WOOL))
                    return "Most Likely Not";
                else return "Perfect Match!";
            case 6:
                return "Unlikely";
            case 2:
                return "It Could Work!";
            case 14:
                return "#Goals";
            default:
                throw new IllegalStateException("Unknown data?");
        }
    }

    /**
     * @return A new block value for the next tick.
     */
    private ItemStack genSlot() {
        switch (RNG.nextInt(6)) {
            case 0:
                return new ItemStack(Material.STONE, 1, (short) 0);
            case 1:
                return new ItemStack(Material.WOOL, 1, (short) 0);
            case 2:
                return new ItemStack(Material.WOOL, 1, (short) 6);
            case 3:
                return new ItemStack(Material.WOOL, 1, (short) 2);
            case 4:
                return new ItemStack(Material.WOOL, 1, (short) 14);
            default:
                return new ItemStack(Material.MAGMA, 1);
        }
    }
}