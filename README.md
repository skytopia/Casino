Casino
=======================

Casino is a lightweight plugin that integrates casino-like games into Skytopia's spawn.

**Please note:** This plugin in its current state will not work outside of Skytopia due to the usage of multiple
 parts of the Skyblock plugin. This can be easily adapted to work standalone, so feel free to do so with credit to us.

Contributors
------------
* [lavuh](https://github.com/lavuh) (Plugin development)
* [JacquiRose](https://github.com/JacquiRose) (Plugin integration, testing)

Features
--------
**Warning**: There is no validation or safety checks to stop players from tampering with the game blocks. This must be done by WorldGuard or other similar plugin.
- Game configurations are located in `/plugins/Casino/games.xml`. Check `example.xml` for configuration structure.
- Any amount of games can be placed in any world. 
- `Single Slots`: Click the button and spin the slots! The rarer the ore is, the better the reward!
- `Triple Slots`: Triple the fun of regular slots with even greater rewards! Go for a three-in-a-row for the greatest rewards!
- `Roulette`: A silly, simple premise of which a skull spins around clockwise in 8 directions. If it lands in the winning direction, you get a reward!
- `Hot or Not Test`: Click the button and let the machine do its work! You will receive a rating from "Stone Cold" to "Hot Stuff" at _random_!
- `Compatibility Test`: Two people operate the machine and receive feedback on their compatibility! This ranges from "Incompatible" to "Perfect Match"!
- `Blackjack`: A classic cards game where you draw cards into your hand and get as close to 21 as possible without going over! Beat the dealer to get a reward!

Compiling
---------
Compiling is not recommended at this current stage. You will need to use [Maven](https://maven.apache.org/) to compile:
* Spigot/CraftBukkit libraries from [BuildTools](https://www.spigotmc.org/wiki/buildtools/)
* Floating-Anvil libraries, which are not open source. There is no API currently.

Once you have these libraries compiled, on your commandline, type the following.
```
cd /path/to/Casino
mvn clean install
```
Maven automatically downloads the other required dependencies.
Output JAR will be placed in the `/target` folder which can be then put into the plugins folder.
