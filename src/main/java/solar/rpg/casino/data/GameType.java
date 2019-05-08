package solar.rpg.casino.data;

/**
 * Denotes all supported game types.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.1
 */
public enum GameType {
    SLOTS,
    TRIPLESLOTS,
    LOVETESTER,
    COMPATIBILITY,
    ROULETTE,
    BLACKJACK,
    UNKNOWN;

    /**
     * Converts an XML tag into a GameType.
     *
     * @param tag XML tag.
     * @return Game type.
     */
    public static GameType fromXML(String tag) {
        try {
            return valueOf(tag.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
