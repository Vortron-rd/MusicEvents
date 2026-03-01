package musify.roguelike;

import musify.config.BiomeMusicConfig;
import musify.musicplayer.MusicPlayer;
import musify.network.NetworkManager;
import musify.network.recurrent.StructureInfoPacket;
import musify.network.roguelike.RoguelikeInfoPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import musify.Musify;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static musify.handlers.BiomeMusicEventHandler.*;
import static musify.handlers.BiomeMusicEventHandler.activeMusic;
import static musify.handlers.BiomeMusicEventHandler.activeTagMusic;
import static musify.handlers.BiomeMusicEventHandler.currentMusicFile;
import static musify.handlers.BiomeMusicEventHandler.stopVanillaMusic;
import static musify.handlers.HandleCombatMusic.combatMusicPlayer;

public class RoguelikeMusicHandler {
    private static final String LOG_FILE_NAME = "roguelike_dungeons.csv";

    /**
     * Checks if a player is inside any Roguelike Dungeon structure listed in roguelike_dungeons.csv
     * @param player The player to check
     * @return The structure name if the player is inside a structure, null otherwise
     */
    public static String findPlayerRoguelikeStructure(EntityPlayer player) {
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
                // Format: structurename,dimension,x1,y1,z1,x2,y2,z2 (may or may not start with fnar:)
                String[] parts = line.split(",");
                if (parts.length < 8) {
                    continue;
                }
                String structureName = parts[0];
                int dimension;
                try {
                    dimension = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (dimension != playerDimension) {
                    continue;
                }
                try {
                    int minX = Integer.parseInt(parts[2]);
                    int minY = Integer.parseInt(parts[3]);
                    int minZ = Integer.parseInt(parts[4]);
                    int maxX = Integer.parseInt(parts[5]);
                    int maxY = Integer.parseInt(parts[6]);
                    int maxZ = Integer.parseInt(parts[7]);

                    // Ensure min <= max
                    int realMinX = Math.min(minX, maxX);
                    int realMaxX = Math.max(minX, maxX);
                    int realMinY = Math.min(minY, maxY);
                    int realMaxY = Math.max(minY, maxY);
                    int realMinZ = Math.min(minZ, maxZ);
                    int realMaxZ = Math.max(minZ, maxZ);

                    // Allow Y to be within 15 blocks above or below the dungeon's Y range
                    int yLowerBound = realMinY - 15;
                    int yUpperBound = realMaxY + 15;

                    if (playerX >= realMinX && playerX <= realMaxX &&
                        playerY >= yLowerBound && playerY <= yUpperBound &&
                        playerZ >= realMinZ && playerZ <= realMaxZ) {
                        return structureName;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            Musify.LOGGER.error("Failed to read Roguelike structure file", e);
        }
        return null;
    }

    /**
     * Finds the appropriate music file for a Roguelike Dungeon structure
     * @param structureName The name of the structure
     * @param musicList Available music tracks in format "structureName:musicFile.ogg" or "structureName:music1.ogg,music2.mp3,music3.ogg"
     * @return The music file name or null if not found
     */
    public static String findRoguelikeStructureMusic(String structureName, String[] musicList) {
        if (structureName == null || structureName.isEmpty() || musicList == null) {
            return null;
        }
        String normalizedName = structureName.toLowerCase().trim();
        for (String entry : musicList) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            // Split on the second ':' only
            int firstColon = entry.indexOf(":");
            int secondColon = entry.indexOf(":", firstColon + 1);
            if (firstColon == -1 || secondColon == -1) {
                continue;
            }
            String entryStructureName = entry.substring(0, secondColon).toLowerCase().trim();
            String musicFilesStr = entry.substring(secondColon + 1);
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
    public static void handleRoguelikeDungeonsMusic(boolean checkOnly) {
        NetworkManager.INSTANCE.sendToServer(new RoguelikeInfoPacket.Request(
                checkOnly,
                BiomeMusicConfig.ecroguelikeDungeonsOptions.roguelikeDungeonsMusicList
        ));
    }

    @SideOnly(Side.CLIENT)
    public static void playRoguelikeMusic(String musicFile) {
        roguelikeCount = 200;
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
    public static void setRoguelikeCounter() {
        roguelikeCount = 200;
    }
}
