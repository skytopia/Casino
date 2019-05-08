package solar.rpg.casino.data;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import solar.rpg.casino.Main;
import solar.rpg.casino.games.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Parses casino game instances from the configuration file.
 *
 * @author lavuh
 * @version 1.1
 * @since 1.1
 */
public class Loader {

    private final Main PLUGIN;

    public Loader(Main PLUGIN) {
        this.PLUGIN = PLUGIN;
    }

    public Set<Game> loadGames() {
        Set<Game> result = new HashSet<>();
        Main.log(Level.FINE, "Attempting to scan configuration!");

        // Check for XML config file.
        File config = new File(PLUGIN.getDataFolder() + File.separator + "config.xml");
        if (!config.exists())
            Main.log(Level.WARNING, "Configuration file does not exist @ /plugins/Casino/config.xml!");
        else {
            try {
                // Load in XML config file to DOM parser.
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(config);
                doc.normalize();

                // Look through each individual tag under <games>.
                NodeList games = doc.getDocumentElement().getChildNodes();
                for (int i = 0; i < games.getLength(); i++) {
                    Node node = games.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element game = (Element) node;
                        try {
                            String worldName = game.getAttribute("world");
                            World world = PLUGIN.getServer().getWorld(worldName);
                            switch (GameType.fromXML(game.getTagName())) {
                                case SLOTS:
                                    // Get appropriate XML child elements.
                                    Element[] data = getAllFirstElementsByName(game, "top", "middle", "bottom", "button");

                                    // Convert elements into locations.
                                    Location TOP = elementToLocation(world, data[0]);
                                    Location MIDDLE = elementToLocation(world, data[1]);
                                    Location BOTTOM = elementToLocation(world, data[2]);
                                    Location BUTTON = elementToLocation(world, data[3]);

                                    // Create slots game.
                                    result.add(new Slots(PLUGIN, world, TOP, MIDDLE, BOTTOM, BUTTON));
                                    break;
                                case TRIPLESLOTS:
                                    // Get appropriate XML child elements.
                                    data = getAllFirstElementsByName(game, "top1", "top2", "top3", "middle1", "middle2", "middle3", "bottom1", "bottom2", "bottom3", "button");

                                    // Convert elements into locations.
                                    Location TOP1 = elementToLocation(world, data[0]);
                                    Location TOP2 = elementToLocation(world, data[1]);
                                    Location TOP3 = elementToLocation(world, data[2]);
                                    Location MIDDLE1 = elementToLocation(world, data[3]);
                                    Location MIDDLE2 = elementToLocation(world, data[4]);
                                    Location MIDDLE3 = elementToLocation(world, data[5]);
                                    Location BOTTOM1 = elementToLocation(world, data[6]);
                                    Location BOTTOM2 = elementToLocation(world, data[7]);
                                    Location BOTTOM3 = elementToLocation(world, data[8]);
                                    BUTTON = elementToLocation(world, data[9]);

                                    // Create slots game.
                                    result.add(new TripleSlots(PLUGIN, world, TOP1, TOP2, TOP3, MIDDLE1, MIDDLE2, MIDDLE3, BOTTOM1, BOTTOM2, BOTTOM3, BUTTON));
                                    break;
                                case LOVETESTER:
                                    // Get appropriate XML child elements.
                                    data = getAllFirstElementsByName(game, "bottom", "top", "head", "sign", "button");

                                    // Convert elements into locations.
                                    BOTTOM = elementToLocation(world, data[0]);
                                    TOP = elementToLocation(world, data[1]);
                                    Location HEAD = elementToLocation(world, data[2]);
                                    Location SIGN = elementToLocation(world, data[3]);
                                    BUTTON = elementToLocation(world, data[4]);

                                    // Create love tester game.
                                    result.add(new LoveTester(PLUGIN, world, BOTTOM, TOP, HEAD, SIGN, BUTTON));
                                    break;
                                case COMPATIBILITY:
                                    // Get the appropriate finite XML elements for compatibility tester.
                                    data = getAllFirstElementsByName(game, "head1", "head2", "button1", "button2", "sign", "blocks");

                                    // Convert elements into locations.
                                    Location HEAD1 = elementToLocation(world, data[0]);
                                    Location HEAD2 = elementToLocation(world, data[1]);
                                    Location BUTTON1 = elementToLocation(world, data[2]);
                                    Location BUTTON2 = elementToLocation(world, data[3]);
                                    SIGN = elementToLocation(world, data[4]);

                                    // Get all blocks provided in the <blocks> section.
                                    NodeList blocks = data[5].getElementsByTagName("block");
                                    Location[] BLOCKS = new Location[blocks.getLength()];
                                    for (int ci = 0; ci < blocks.getLength(); ci++)
                                        // We are assuming that there are only valid <block> tags within the <blocks> section.
                                        BLOCKS[ci] = elementToLocation(world, (Element) blocks.item(ci));

                                    // Create compatibility tester game.
                                    result.add(new CompatibilityTester(PLUGIN, world, BLOCKS, HEAD1, HEAD2, BUTTON1, BUTTON2, SIGN));
                                    break;
                                case ROULETTE:
                                    // Get appropriate XML child elements.
                                    data = getAllFirstElementsByName(game, "head", "button", "win");

                                    // Convert elements into locations.
                                    HEAD = elementToLocation(world, data[0]);
                                    BUTTON = elementToLocation(world, data[1]);

                                    // Convert <win> tag attribute into BlockFace.
                                    BlockFace WINDIR = BlockFace.valueOf(data[2].getAttribute("direction"));

                                    // Create roulette game.
                                    result.add(new Roulette(PLUGIN, world, WINDIR, HEAD, BUTTON));
                                    break;
                                case BLACKJACK:
                                    // Get appropriate XML child elements.
                                    data = getAllFirstElementsByName(game, "hitme", "stand", "info", "cards1to3", "cards4to6");

                                    // Convert elements into locations.
                                    Location HITME = elementToLocation(world, data[0]);
                                    Location STAND = elementToLocation(world, data[1]);
                                    Location INFO = elementToLocation(world, data[2]);
                                    Location CARDS1TO3 = elementToLocation(world, data[3]);
                                    Location CARDS4TO6 = elementToLocation(world, data[4]);

                                    // Create blackjack game.
                                    result.add(new Blackjack(PLUGIN, world, HITME, STAND, INFO, CARDS1TO3, CARDS4TO6));
                                    break;
                                default:
                                    throw new IllegalArgumentException(String.format("Unknown game tag '%s'", game.getTagName()));
                            }
                        } catch (Exception ex) {
                            // This exception can be thrown when required tags are not found. (SAXException)
                            // It can also be thrown by invalid values. (IllegalArgumentException)
                            // It should be caught and then should move onto the next element.
                            Main.log(Level.WARNING, String.format("Unable to load a game of %s: Printing stack trace", game.getTagName()));
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // After parsing configuration, return resulting set of games.
        return result;
    }

    /**
     * Searches through an element's children for specified tag names, returning the first element from each.
     *
     * @param root     Root element.
     * @param elements Elements, by tag name, to search for and return.
     * @return Array of elements with specified tag names.
     * @throws IllegalArgumentException Thrown if tag could not be found.
     */
    private Element[] getAllFirstElementsByName(Element root, String... elements) {
        Element[] result = new Element[elements.length];
        for (int i = 0; i < elements.length; i++) {
            NodeList list = root.getElementsByTagName(elements[i]);
            for (int j = 0; j < list.getLength(); j++) {
                Node found = list.item(j);
                if (found.getNodeType() == Node.ELEMENT_NODE)
                    result[i] = (Element) found;
            }
            if (result[i] == null)
                throw new IllegalArgumentException(String.format("Could not find tag '%s' in '%s'", elements[i], root.getTagName()));
        }
        return result;
    }

    /**
     * Converts an XML element into a Location.
     *
     * @param world   Provided world.
     * @param element Element containing attributes x,y,z.
     * @return Parsed location.
     * @throws NumberFormatException Values may not be integers.
     */
    private Location elementToLocation(World world, Element element) throws NumberFormatException {
        int x = Integer.parseInt(element.getAttribute("x"));
        int y = Integer.parseInt(element.getAttribute("y"));
        int z = Integer.parseInt(element.getAttribute("z"));
        return new Location(world, x, y, z);
    }
}
