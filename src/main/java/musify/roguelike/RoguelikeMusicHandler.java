package musify.roguelike;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import musify.Musify;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
                // Format: fnar:structurename,dimension,x1,y1,z1,x2,y2,z2
                if (!line.startsWith("fnar:")) {
                    continue;
                }
                String content = line.substring(5); // after "fnar:"
                String[] parts = content.split(",");
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
}
