package solar.rpg.casino.games;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import solar.rpg.casino.Main;
import solar.rpg.casino.data.GameType;
import solar.rpg.skyblock.island.Island;
import solar.rpg.skyblock.util.StringUtility;
import solar.rpg.skyblock.util.Title;
import solar.rpg.skyblock.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Blackjack is a card game where the goal is to get
 * as close to 21 as possible without going over.
 * The player is dealt random cards and can choose to
 * stand whenever they feel they are most likely to win.
 * In order to win, you must get a better hand than the dealer.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.0
 */
public class Blackjack extends Game {

    /* The types of cards and suits you can be dealt. */
    private final static String[] CARDS = {"Ace;-1", "Two;2", "Three;3", "Four;4", "Five;5", "Six;6", "Seven;7", "Eight;8", "Nine;9", "Ten;10", "Jack;10", "Queen;10", "King;10"};
    private final static String[] SUITS = {"Hearts ❤ ", "Diamonds ♦", "Clubs ♣", "Spades ♠"};

    /* Hit me, stand, information, cards 1 to 3, cards 4 to 6. */
    private final Block HITME, STAND, INFO, CARDS13, CARDS46;

    /* The remaining types of cards that can be drawn. */
    private final List<String[]> deck;

    /* The list of cards that the player has received. */
    private final List<String[]> selected;

    /* The dealer's total. This is randomly generated. */
    private int dealerTotal;

    /* The amount of time the player has left to make a move before the game stops. */
    private int timeout;

    public Blackjack(Main PLUGIN, World WORLD, Location... locs) {
        super(PLUGIN, WORLD);
        HITME = locs[0].getBlock();
        STAND = locs[1].getBlock();
        INFO = locs[2].getBlock();
        CARDS13 = locs[3].getBlock();
        CARDS46 = locs[4].getBlock();

        deck = new ArrayList<>();
        selected = new ArrayList<>();

        // Runs a timer that decrements the timeout value.
        new BukkitRunnable() {
            public void run() {
                // Only run the timer code if the game is in use.
                if (inUse) {
                    Sign infoSign = (Sign) INFO.getState();
                    infoSign.setLine(2, String.valueOf(timeout));
                    infoSign.update(true);
                    if (timeout == 0)
                        reset();
                    timeout--;
                }
            }
        }.runTaskTimer(PLUGIN, 0L, 20L);
    }

    @Override
    public GameType getGameType() {
        return GameType.BLACKJACK;
    }

    @Override
    public String getName() {
        return "Blackjack";
    }

    /**
     * Updates the card and information signs with game state values.
     */
    private void updateSigns() {
        /* Cast the sign blocks as their Sign block state. */
        Sign infoSign = (Sign) INFO.getState();
        Sign card13Sign = (Sign) CARDS13.getState();
        Sign card46Sign = (Sign) CARDS46.getState();

        if (inUse) {
            // Set the information sign to the name of the player playing.
            // Include how long they have in seconds before their game times out.
            infoSign.setLine(1, Bukkit.getOfflinePlayer(using).getName());
            infoSign.setLine(2, String.valueOf(timeout));

            // Write the received cards to the 1-3 and 4-6 signs in their appropriate order.
            for (int i = 0; i < selected.size(); i++)
                signFromIndex(card13Sign, card46Sign, i).setLine(i < 3 ? i + 1 : i - 3 + 1, selected.get(i)[0].split(";")[0]);
        } else {
            // If there is no active game, clear the information sign.
            infoSign.setLine(1, "Nobody");
            infoSign.setLine(2, "");
        }

        /* Perform an update to reflect changes via plugin. */
        infoSign.update(true);
        card13Sign.update(true);
        card46Sign.update(true);
    }

    /**
     * When a new card is dealt, if is from the first to third card,
     * it needs to be placed on the first sign. If it is from the fourth
     * to sixth card, it needs to be placed on the second sign.
     *
     * @param sign1 Sign block state of the first sign.
     * @param sign2 Sign block state of the second sign.
     * @param i     What number card this is, from 0 to 5.
     * @return The appropriate sign to write the new card on.
     */
    private Sign signFromIndex(Sign sign1, Sign sign2, int i) {
        if (i <= 2)
            return sign1;
        return sign2;
    }

    /**
     * Denotes a Hit Me action in blackjack.
     * Starts the game if there is no game running.
     * Adds a random card to the player's hand otherwise.
     *
     * @param player The player who is playing the game.
     */
    private void hitMe(Player player) {
        // Get the island the player belongs to.
        Island island = PLUGIN.cloud().islands().getIsland(player.getUniqueId());

        // Players must have an island before using the slots machine.
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You need an island to do this!");
            return;
        }

        // Players using the blackjack table must have at least $5000 available.
        // The game must be also free to use.
        if (!inUse) {
            if (!PLUGIN.cloud().getEconomy().has(player, 5000)) {
                player.sendMessage(ChatColor.RED + "You need at least 5,000ƒ to play this!");
                return;
            }
            start(player);
        }

        // Only allow the person using the game to continue playing it.
        if (!using.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Someone else is using this at the moment!");
            return;
        }

        // Reset timeout counter.
        timeout = 30;

        // Randomly pick out a card of a specific suit from the deck.
        String[] gen = deck.remove(RNG.nextInt(deck.size()));
        selected.add(gen);

        // Calculate the user's total.
        int total = total(false);
        if (total > 21)
            total = total(true);

        // Produce the literal word as opposed to the number.
        String literal = gen[0].split(";")[0];

        // Send the player a message and title card to notify them of their card selection.
        player.sendMessage(ChatColor.GOLD + "You were dealt a " + ChatColor.RED + literal + " of " + gen[1] + "!" + " (" + total + ")");
        Title.showTitle(player, ChatColor.RED + literal + " of " + gen[1] + "!" + " (" + total + ")", ChatColor.GOLD + "" + StringUtility.ordinal(selected.size()) + " card dealt!", 10, 35, 10);

        // Update the signs, total the cards, then update the signs again.
        // We update the signs again just in case the game has ended.
        updateSigns();
        if (selected.size() >= 6 || total >= 21)
            // Check state if the number of cards drawn is equal to 6, or the total has exceeded 21.
            checkState(player);
        updateSigns();
    }

    /**
     * Denotes a Stand action in Blackjack.
     * When a player stands, they are done drawing cards.
     * They are then totalled and compared to the dealer's.
     *
     * @param player The player who is playing the game.
     */
    private void stand(Player player) {
        // You can only start the game by clicking 'Hit Me'.
        if (!inUse) {
            player.sendMessage(ChatColor.RED + "Press the other button to start!");
            return;
        }

        // Only the person using the game can choose to Stand.
        if (!using.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Someone else is using this at the moment!");
            return;
        }

        // Total the cards and update the signs.
        checkState(player);
        updateSigns();
    }

    /**
     * Starts the Blackjack game.
     *
     * @param player The player who is playing the game.
     */
    private void start(Player player) {
        // Set the game as in use. Withdraw money from the player.
        inUse = true;
        using = player.getUniqueId();
        PLUGIN.cloud().getEconomy().withdrawPlayer(player, 5000);

        // Reset the card signs so they can be re-used at the start of this new game.
        Sign cards13Sign = (Sign) CARDS13.getState();
        Sign cards46Sign = (Sign) CARDS46.getState();
        cards13Sign.setLine(1, "");
        cards13Sign.setLine(2, "");
        cards13Sign.setLine(3, "");
        cards13Sign.update(true);
        cards46Sign.setLine(2, "");
        cards46Sign.setLine(3, "");
        cards46Sign.update(true);

        // Add every card of every type to the list of current cards.
        for (String card : CARDS)
            for (String suit : SUITS)
                deck.add(new String[]{card, suit});

        // Set the dealer's total that the player needs to beat. This is unknown to the player.
        dealerTotal = (16 + RNG.nextInt(7));

        // Spawn a firework using Skyblock's utility class.
        Utility.spawnFirework(player.getLocation());
    }

    /**
     * Resets the state of this Blackjack table.
     */
    private void reset() {
        //TODO: Maybe add cooldowns?
        inUse = false;
        using = null;
        deck.clear();
        selected.clear();
        timeout = 30;
    }

    /**
     * Totals the user's cards, which may produce an end-of-game
     * result depending on certain conditions of cards held.
     *
     * @param pl The player playing the game.
     */
    private void checkState(Player pl) {
        // Calculate the user's total.
        int total = total(false);
        if (total > 21)
            total = total(true);

        // Check if the dealer has busted.
        boolean dealerBust = this.dealerTotal > 21;

        if (total > 21) {
            // If total is above 21, the player has busted.
            if (dealerBust) {
                // If the dealer has busted too, call a MUTUAL_BUST result.
                results(BlackJackResult.MUTUAL_BUST, pl, total);
            } else
                // If the dealer hasn't busted, call a regular BUST result.
                results(BlackJackResult.BUST, pl, total);
        } else if (total == 21) {
            // If the total is equal to 21, the game will immediately stop and produce a result.
            if (this.selected.size() == 2) {
                // If the user got 21 in 2 cards, this is known as a blackjack, the best possible hand of cards.
                if (dealerBust) {
                    // The dealer also busted, so the reward should be even bigger!
                    results(BlackJackResult.BLACKJACK_DEALERBUST, pl, total);
                } else {
                    // If the dealer didn't bust, the user still automatically wins due to blackjack.
                    results(BlackJackResult.BLACKJACK_WIN, pl, total);
                }
            } else if (dealerBust) {
                // The user got a 21 and the dealer busted, the reward should be increased.
                results(BlackJackResult.TWENTYONE_DEALERBUST, pl, total);
            } else {
                if (total == dealerTotal) {
                    // The user got a 21 and the dealer didn't.
                    results(BlackJackResult.TWENTYONE_DEALERLOST, pl, total);
                } else {
                    // The user and the dealer both got a 21.
                    results(BlackJackResult.TIE, pl, total);
                }
            }
        } else if (dealerBust) {
            // The dealer busted. The player didn't, so they automatically win.
            results(BlackJackResult.DEALER_BUST, pl, total);
        } else if (total == dealerTotal) {
            // The user and the dealer got the same score, this is a die.
            results(BlackJackResult.TIE, pl, total);
        } else if (total > dealerTotal) {
            // If the user got a higher score than the dealer, this is a regular win.
            results(BlackJackResult.WIN, pl, total);
        } else
            // Otherwise, the dealer won.
            results(BlackJackResult.DEALER_WIN, pl, total);
    }

    /**
     * If an end-of-game result condition has been met, this
     * method is called to signify the end of the game. The
     * player is notified and rewarded if applicable.
     *
     * @param result The result achieved.
     * @param player The player playing the game.
     * @param total  The total they received.
     */
    private void results(BlackJackResult result, Player player, int total) {
        // Find the user's island.
        Island island = PLUGIN.cloud().islands().getIsland(player.getUniqueId());

        // The user can delete their island in the midst of playing this game, so, perform a quick check.
        if (island != null) {
            if (result.desc.startsWith(ChatColor.RED + ""))
                // Play a bad sound if the user lost.
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_DEATH, 2.0F, 2.0F);
            else
                // Play a good sound if the user won.
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WOLF_HOWL, 1.0F, 3.0F);

            // Spacer message so the player can more easily see the results.
            player.sendMessage("");
            if (result == BlackJackResult.BLACKJACK_DEALERBUST) {
                // A blackjack dealer bust is incredibly rare. Broadcast this event to everyone!
                Bukkit.broadcastMessage(player.getDisplayName() + ChatColor.GOLD + " got a 21 in two cards and busted the dealer in Blackjack!" + ChatColor.RED + " (/warp casino)");
            } else if (result == BlackJackResult.BLACKJACK_WIN)
                // A blackjack is also rare, so let's broadcast this too.
                Bukkit.broadcastMessage(player.getDisplayName() + ChatColor.GOLD + " got a 21 in two cards in Blackjack!" + ChatColor.RED + " (/warp casino)");

            // Send player the results message.
            player.sendMessage(ChatColor.GOLD + "You totalled " + total + " in " + selected.size() + " cards!" + ChatColor.RED + " Dealer totalled " + dealerTotal + "!");
            player.sendMessage(ChatColor.GOLD + "Result: " + result.desc + ChatColor.GRAY + ChatColor.ITALIC + " (" + result.reward + "ƒ awarded!)");
            player.sendMessage("");

            // Deposit any money from achieving the result that the user got. @see BlackJackResult#reward
            if (result.reward > 0)
                island.main().getEconomy().depositPlayer(player, result.reward);
        }

        // Reset the game for the next usage.
        reset();
    }

    /**
     * Totals the user's currently held cards.
     *
     * @param ace If true, ace counts as 1, not 11.
     * @return The total of the current cards held.
     */
    private int total(boolean ace) {
        int total = 0;
        for (String[] i : this.selected) {
            // Add this card to the running total.
            int card = Integer.parseInt(i[0].split(";")[1]);
            if (card == -1)
                if (ace)
                    card = 1;
                else
                    card = 11;
            total += card;
        }
        return total;
    }

    @EventHandler
    public void onServe(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getWorld().equals(WORLD)) return;
        Location click = event.getClickedBlock().getLocation();
        if (click.equals(HITME.getLocation()))
            hitMe(event.getPlayer());
        else if (click.equals(STAND.getLocation()))
            stand(event.getPlayer());
    }

    private enum BlackJackResult {
        BLACKJACK_DEALERBUST(25000, ChatColor.DARK_GREEN + "Blackjack! Dealer busted!"),
        BLACKJACK_WIN(15000, ChatColor.DARK_GREEN + "Blackjack! Dealer loses!"),
        TWENTYONE_DEALERBUST(10000, ChatColor.GREEN + "21! Dealer busted!"),
        TWENTYONE_DEALERLOST(10000, ChatColor.GREEN + "21! Dealer loses!"),
        WIN(7500, ChatColor.GREEN + "You win! Dealer loses!"),
        DEALER_BUST(7500, ChatColor.GREEN + "The dealer busted! You win!"),
        DEALER_WIN(1500, ChatColor.RED + "The dealer won! You lose!"),
        BUST(0, ChatColor.RED + "You busted! You lose!"),
        MUTUAL_BUST(5000, ChatColor.GRAY + "You and the dealer busted!"),
        TIE(5000, ChatColor.GRAY + "You tied with the dealer!");

        final int reward;
        final String desc;

        BlackJackResult(int reward, String desc) {
            this.reward = reward;
            this.desc = desc;
        }
    }
}