# Oozings Settings (Fabric, server-side only)

Build:  ./gradlew build   (needs JDK 25)  -> build/libs/oozings-settings-1.0.0.jar
Install: put the jar in the server's mods folder (+ Fabric API). Players need NO mod.

/oozings  (op only) opens the GUI:
- Brewing Blocker: click a potion to block/unblock it (strength, strong_strength, long_strength, etc.)
- Item Cooldowns: hold an item, click "Add", then left/right click to change seconds. 0 = off.
Settings are saved in config/oozings-settings.json
