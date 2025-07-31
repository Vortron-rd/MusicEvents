package musify.handlers;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import musify.musicplayer.MusicPlayer;
import musify.network.doomlikes.DungeonMusicPacket;
import musify.network.NetworkManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import static musify.handlers.BiomeMusicEventHandler.*;
import static musify.handlers.BiomeMusicEventHandler.activeTagMusic;
import static musify.handlers.HandleCombatMusic.combatMusicPlayer;


public class DungeonHandler {

    /**
     * Checks if the player is near any dungeon from the CSV file
     * @param player The player to check
     * @param maxDistance The maximum horizontal distance to consider "nearby"
     * @return The first dungeon that satisfies the distance conditions, or null if none found
     */
    public static DungeonInfo findNearbyDungeons(EntityPlayer player, double maxDistance) {
        World world = player.getEntityWorld();
        File file;

        // Handle client-side path differently
        if (world.isRemote) {
            String worldName = player.getEntityWorld().getWorldInfo().getWorldName();
            // Try to construct the correct path on client side
            File gameDir = new File(".");
            file = new File(new File(gameDir, "saves"), worldName + "/dungeon_rooms.csv");

            // If still not found, try alternative paths
            if (!file.exists()) {
                // Try parent directory (common in dev environments)
                file = new File(new File(gameDir.getParentFile(), "saves"), worldName + "/dungeon_rooms.csv");
            }
        } else {
            // Server side - normal path
            File worldDir = world.getSaveHandler().getWorldDirectory();
            file = new File(worldDir, "dungeon_rooms.csv");
        }

        if (!file.exists()) {
            Musify.LOGGER.warn("Dungeon rooms CSV file not found 1");
            return null;
        }

        if (!(player.dimension == 0 || player.dimension == -1)) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    try {
                        String theme = parts[0];
                        double x = Double.parseDouble(parts[1]);
                        double minY = Double.parseDouble(parts[2]);
                        double maxY = Double.parseDouble(parts[3]);
                        double z = Double.parseDouble(parts[4]);
                        String biome = parts[5];

                        // Calculate horizontal distance (2D)
                        double distance = Math.sqrt(
                                Math.pow(player.posX - x, 2) +
                                        Math.pow(player.posZ - z, 2)
                        );

                        // Check horizontal distance and vertical position
                        if (distance <= maxDistance &&
                                player.posY >= minY - BiomeMusicConfig.edoomlikeDungeonsOptions.DoomlikeMaxYLevel &&
                                player.posY <= maxY + BiomeMusicConfig.edoomlikeDungeonsOptions.DoomlikeMaxYLevel) {

                            if (biome.equals("hell")) {
                                if (player.dimension != -1) {
                                    continue; // Skip hell biomes if not in Nether
                                }
                            }
                            return new DungeonInfo(theme, x, minY, maxY, z, biome, distance);
                        }
                    } catch (NumberFormatException e) {
                        Musify.LOGGER.error("Error parsing dungeon coordinates: " + line, e);
                    }
                }
            }
        } catch (IOException e) {
            Musify.LOGGER.error("Error reading dungeon_rooms.csv", e);
        }

        return null;
    }

    /**
     * Class to hold information about a dungeon
     */
    public static class DungeonInfo {
        private final String theme;
        private final double x;
        private final double minY;
        private final double maxY;
        private final double z;
        private final String biome;
        private final double distance;

        public DungeonInfo(String theme, double x, double minY, double maxY, double z, String biome, double distance) {
            this.theme = theme;
            this.x = x;
            this.minY = minY;
            this.maxY = maxY;
            this.z = z;
            this.biome = biome;
            this.distance = distance;
        }

        // Getters
        public String getTheme() { return theme; }
        public double getX() { return x; }
        public double getMinY() { return minY; }
        public double getMaxY() { return maxY; }
        public double getZ() { return z; }
        public String getBiome() { return biome; }
        public double getDistance() { return distance; }
    }

    @SideOnly(Side.CLIENT)
    public static void HandleDoomlikeDungeonsMusic(EntityPlayer player, int distance) {
        HandleDoomlikeDungeonsMusic(player, distance, false);
    }

    @SideOnly(Side.CLIENT)
    public static void HandleDoomlikeDungeonsMusic(EntityPlayer player, int distance, boolean checkOnly) {
        // Include client's music list configuration in the request
        NetworkManager.INSTANCE.sendToServer(new DungeonMusicPacket.Request(
                distance,
                checkOnly,
                BiomeMusicConfig.edoomlikeDungeonsOptions.doomlikeDungeonsMusicList
        ));
    }


    public static void setDungeonCount() {
        doomlikeCount = 200;
    }

    @SideOnly(Side.CLIENT)
    public static void playDungeonMusic(String musicFile) {

        doomlikeCount = 200;

        if (activeTagMusic != null) {
            activeTagMusic.stopWithFadeOut(BiomeMusicConfig.lfadeOptions.customMusicFadeOutTime);
            activeTagMusic = null;
        }
        if (combatMusicPlayer != null) {
            combatMusicPlayer.stopWithFadeOut(BiomeMusicConfig.lfadeOptions.customMusicFadeOutTime);
            combatMusicPlayer = null;
        }

        if (activeMusic != null) {
            activeMusic.stopWithFadeOut(BiomeMusicConfig.lfadeOptions.customMusicFadeOutTime);
            activeMusic = new MusicPlayer(musicFile, true);
            currentMusicFile = musicFile;
            activeMusic.playWithFadeIn(BiomeMusicConfig.lfadeOptions.customMusicFadeInTime);
        }
        else if (activeMusic == null) {
            activeMusic = new MusicPlayer(musicFile, true);
            currentMusicFile = musicFile;
            activeMusic.playWithFadeIn(BiomeMusicConfig.lfadeOptions.customMusicFadeInTime);
        }
        stopVanillaMusic();
    }

    public static void removeDuplicatesFromDungeonFile(World world) {
        File file = new File(world.getSaveHandler().getWorldDirectory(), "dungeon_rooms.csv");
        File tempFile = new File(world.getSaveHandler().getWorldDirectory(), "dungeon_rooms_temp.csv");

        if (!file.exists()) {
            Musify.LOGGER.warn("Dungeon rooms CSV file not found 2");
            return;
        }

        try {
            Set<String> uniqueLines = new HashSet<>();
            int originalCount = 0;
            int uniqueCount = 0;

            // Read all unique lines
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    originalCount++;
                    if (uniqueLines.add(line)) {
                        uniqueCount++;
                    }
                }
            }

            // Write unique lines to temp file
            try (FileWriter writer = new FileWriter(tempFile)) {
                for (String line : uniqueLines) {
                    writer.write(line + "\n");
                }
            }

            // Replace original with temp file
            if (tempFile.exists() && file.delete() && tempFile.renameTo(file)) {
                Musify.LOGGER.info("Successfully removed {} duplicate entries from dungeon_rooms.csv",
                        (originalCount - uniqueCount));
            } else {
                Musify.LOGGER.error("Failed to replace original file with deduplicated version");
            }

        } catch (IOException e) {
            Musify.LOGGER.error("Error while removing duplicates from dungeon_rooms.csv", e);
        }
    }

    public static String findDungeonMusic(String theme, String[] musicEntries) {
        for (String entry : musicEntries) {
            String[] parts = entry.split(":", 2); // Split at the first colon
            if (parts.length == 2 && theme.trim().toLowerCase().contains(parts[0].trim().toLowerCase())) {
                return parts[1].trim();
            }
        }
        return null;
    }

}
