package musify.handlers;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import musify.musicplayer.MusicPlayer;
import musify.network.NetworkManager;
import musify.network.recurrent.StructureInfoPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static musify.handlers.BiomeMusicEventHandler.*;
import static musify.handlers.HandleCombatMusic.combatMusicPlayer;

public class RecurrentMusicHandler {

    private static final String LOG_FILE_NAME = "recurrent_structures.csv";

    /**
     * Checks if a player is inside any structure listed in the recurrent_structures.csv file
     * @param player The player to check
     * @return The structure name if the player is inside a structure, null otherwise
     */
    public static String findPlayerStructure(EntityPlayer player) {
        if (player == null || !(player.world instanceof WorldServer)) {
            return null;
        }

        WorldServer world = (WorldServer) player.world;
        Musify.LOGGER.info("PLAYER WORLD: {}", world);
        int playerDimension = world.provider.getDimension();
        Musify.LOGGER.info("PLAYER DIMENSION: {}", playerDimension);
        double playerX = player.posX;
        double playerY = player.posY;
        double playerZ = player.posZ;
        Musify.LOGGER.info("PLAYER POSITION: X={}, Y={}, Z={}", playerX, playerY, playerZ);

        File worldDir = world.getSaveHandler().getWorldDirectory();
        File structureFile = new File(worldDir, LOG_FILE_NAME);

        if (!structureFile.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(structureFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Split by the first two commas to get name and dimension
                String[] initialParts = line.split(",", 3);
                if (initialParts.length < 3) {
                    continue;
                }

                String structureName = initialParts[0];

                // Parse dimension
                int dimension;
                try {
                    dimension = Integer.parseInt(initialParts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }

                // Skip if not in the same dimension
                if (dimension != playerDimension) {
                    continue;
                }

                // Process the rest of the line for coordinates
                String coordsString = initialParts[2];
                String[] boundingBoxParts = coordsString.split(":");

                if (boundingBoxParts.length != 2) {
                    continue;
                }

                // Find where the colon is in the original line to split correctly
                int colonIndex = line.indexOf(':');
                String minPart = line.substring(line.indexOf(',', line.indexOf(',') + 1) + 1, colonIndex);
                String maxPart = line.substring(colonIndex + 1);

                String[] minCoords = minPart.split(",");
                String[] maxCoords = maxPart.split(",");

                if (minCoords.length < 3 || maxCoords.length < 3) {
                    continue;
                }

                try {
                    int minX = Integer.parseInt(minCoords[0]) - 5;
                    int minY = Integer.parseInt(minCoords[1]) - 5;
                    int minZ = Integer.parseInt(minCoords[2]) - 5;

                    int maxX = Integer.parseInt(maxCoords[0]) + 5;
                    int maxY = Integer.parseInt(maxCoords[1]) + 5;
                    int maxZ = Integer.parseInt(maxCoords[2]) + 5;

                    // Check if player is inside this bounding box
                    if (playerX >= minX && playerX <= maxX &&
                            playerY >= minY && playerY <= maxY &&
                            playerZ >= minZ && playerZ <= maxZ) {
                        Musify.LOGGER.info("STRUCTURE BOUNDING BOX: Min [{}, {}, {}] Max [{}, {}, {}]",
                                minX, minY, minZ, maxX, maxY, maxZ);
                        Musify.LOGGER.info("PLAYER IS INSIDE STRUCTURE: {}", structureName);
                        return structureName;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            Musify.LOGGER.error("Failed to read structure file", e);
        }

        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void handleRecurrentMusic(EntityPlayer player, boolean checkOnly) {
        NetworkManager.INSTANCE.sendToServer(new StructureInfoPacket.Request(
                checkOnly,
                BiomeMusicConfig.recurrentComplexOptions.recurrentComplexMusicList
        ));
    }

    /**
     * Finds the appropriate music file for a structure
     * @param structureName The name of the structure
     * @param musicList Available music tracks in format "structureName:musicFile.ogg"
     * @return The music file name or null if not found
     */
    public static String findStructureMusic(String structureName, String[] musicList) {
        if (structureName == null || structureName.isEmpty() || musicList == null) {
            return null;
        }

        // Normalize structure name for comparison
        String normalizedName = structureName.toLowerCase().trim();

        for (String entry : musicList) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }

            // Split the entry into structure name and music file
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String entryStructureName = parts[0].toLowerCase().trim();
            String musicFile = parts[1];

            // Check if this entry matches the structure name
            if (normalizedName.equals(entryStructureName)) {
                Musify.LOGGER.info("Found music for structure {}: {}", structureName, musicFile);
                return musicFile;
            }
        }
        Musify.LOGGER.info("No specific music found for structure: {}", structureName);
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void playStructuremusic(String musicFile) {
        recurrentCount = 200;
        isDungeonMusicPlaying = true;
        if (currentMusicFile != null && currentMusicFile.equals(musicFile)) {
            return; // Already playing this music
        }
        if (activeTagMusic != null) {
            activeTagMusic.stopWithFadeOut(BiomeMusicConfig.fadeOptions.customMusicFadeOutTime);
            activeTagMusic = null;
        }
        if (combatMusicPlayer != null) {
            combatMusicPlayer.stopWithFadeOut(BiomeMusicConfig.fadeOptions.customMusicFadeOutTime);
            combatMusicPlayer = null;
        }
        if (activeMusic != null) {
            activeMusic.stopWithFadeOut(BiomeMusicConfig.fadeOptions.customMusicFadeOutTime);
            activeMusic = null;
            activeMusic = new MusicPlayer(musicFile, true);
            activeMusic.playWithFadeIn(BiomeMusicConfig.fadeOptions.customMusicFadeInTime);
            currentMusicFile = musicFile;
        } else if (activeMusic == null) {
            activeMusic = new MusicPlayer(musicFile, true);
            activeMusic.playWithFadeIn(BiomeMusicConfig.fadeOptions.customMusicFadeInTime);
            currentMusicFile = musicFile;
        }
        stopVanillaMusic();
    }

    @SideOnly(Side.CLIENT)
    public static void setRecurrentCounter() {
        Musify.LOGGER.info("RESETTING RECURRENT COUNTER");
        recurrentCount = 200;
    }
}
