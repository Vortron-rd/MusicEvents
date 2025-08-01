package musify.handlers;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import musify.musicplayer.MusicPlayer;
import musify.network.NetworkManager;
import musify.network.recurrent.StructureInfoPacket;
import net.minecraft.entity.player.EntityPlayer;
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
        int playerDimension = world.provider.getDimension();
        double playerX = player.posX;
        double playerY = player.posY;
        double playerZ = player.posZ;

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

                String[] initialParts = line.split(",", 3);
                if (initialParts.length < 3) {
                    continue;
                }

                String structureName = initialParts[0];

                int dimension;
                try {
                    dimension = Integer.parseInt(initialParts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (dimension != playerDimension) {
                    continue;
                }

                String coordsString = initialParts[2];
                String[] boundingBoxParts = coordsString.split(":");

                if (boundingBoxParts.length != 2) {
                    continue;
                }

                int colonIndex = line.indexOf(':');
                String minPart = line.substring(line.indexOf(',', line.indexOf(',') + 1) + 1, colonIndex);
                String maxPart = line.substring(colonIndex + 1);

                String[] minCoords = minPart.split(",");
                String[] maxCoords = maxPart.split(",");

                if (minCoords.length < 3 || maxCoords.length < 3) {
                    continue;
                }

                try {
                    int extraDistance = BiomeMusicConfig.frecurrentComplexOptions.recurrentComplexExtraDistance;

                    int minX = Integer.parseInt(minCoords[0]) - extraDistance;
                    int minY = Integer.parseInt(minCoords[1]) - extraDistance;
                    int minZ = Integer.parseInt(minCoords[2]) - extraDistance;

                    int maxX = Integer.parseInt(maxCoords[0]) + extraDistance;
                    int maxY = Integer.parseInt(maxCoords[1]) + extraDistance;
                    int maxZ = Integer.parseInt(maxCoords[2]) + extraDistance;

                    if (playerX >= minX && playerX <= maxX &&
                            playerY >= minY && playerY <= maxY &&
                            playerZ >= minZ && playerZ <= maxZ) {
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
                BiomeMusicConfig.frecurrentComplexOptions.recurrentComplexMusicList
        ));
    }

    /**
     * Finds the appropriate music file for a structure
     * @param structureName The name of the structure
     * @param musicList Available music tracks in format "structureName:musicFile.ogg" or "structureName:music1.ogg,music2.mp3,music3.ogg"
     * @return The music file name or null if not found
     */
    public static String findStructureMusic(String structureName, String[] musicList) {
        if (structureName == null || structureName.isEmpty() || musicList == null) {
            return null;
        }

        String normalizedName = structureName.toLowerCase().trim();

        for (String entry : musicList) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }

            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String entryStructureName = parts[0].toLowerCase().trim();
            String musicFilesStr = parts[1];

            if (normalizedName.equals(entryStructureName)) {
                String[] musicFiles = musicFilesStr.split(",");
                if (musicFiles.length == 0) {
                    continue;
                }

                return musicFiles[new java.util.Random().nextInt(musicFiles.length)];
            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void playStructuremusic(String musicFile) {
        recurrentCount = 200;
        isDungeonMusicPlaying = true;
        if (currentMusicFile != null && currentMusicFile.equals(musicFile)) {
            return;
        }
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
            activeMusic = null;
            activeMusic = new MusicPlayer(musicFile, true);
            activeMusic.playWithFadeIn(BiomeMusicConfig.lfadeOptions.customMusicFadeInTime);
            currentMusicFile = musicFile;
        } else if (activeMusic == null) {
            activeMusic = new MusicPlayer(musicFile, true);
            activeMusic.playWithFadeIn(BiomeMusicConfig.lfadeOptions.customMusicFadeInTime);
            currentMusicFile = musicFile;
        }
        stopVanillaMusic();
    }

    @SideOnly(Side.CLIENT)
    public static void setRecurrentCounter() {
        recurrentCount = 200;
    }
}
