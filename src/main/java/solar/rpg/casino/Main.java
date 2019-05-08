package solar.rpg.casino;

import org.bukkit.plugin.java.JavaPlugin;
import solar.rpg.casino.data.Loader;
import solar.rpg.casino.games.Game;
import solar.rpg.skyblock.Cloud;

import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    /* Reference to Skyblock supercontroller. */
    private Cloud CLOUD;

    /* Keep a static instance of the logger after enabling so all classes can log. */
    private static Logger logger;

    /* All random numbers should be generated through this Random. */
    private Random RNG;

    /* Game loader. */
    private Loader LOADER;

    public void onEnable() {
        logger = getLogger();
        log(Level.FINE, String.format("Enabling Casino v%s!", getDescription().getVersion()));

        CLOUD = solar.rpg.skyblock.Main.instance.main();
        RNG = new Random();
        LOADER = new Loader(this);

        // Currently loads the games into a set. This doesn't do anything as of v1.1.
        Set<Game> games = LOADER.loadGames();
    }

    /**
     * @return Instance of Random Number Generator.
     */
    public Random getRNG() {
        return RNG;
    }

    /**
     * @return Instance of Skyblock supercontroller.
     */
    public Cloud cloud() {
        return CLOUD;
    }

    /**
     * Global logging method. Prints out to console with Casino prefix.
     *
     * @param level Logging level.
     * @param msg   Message to log.
     */
    public static void log(Level level, String msg) {
        logger.log(level, String.format("[Casino] %s", msg));
    }
}