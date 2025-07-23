package musify.utils;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;

public class DungeonUtils {

    private static volatile String cachedDungeonMusic = null;
    private static volatile long lastScanTime = 0;
    private static final long SCAN_INTERVAL_MS = 2000; // scan every 2 seconds

    public static String getDungeonMusic(EntityPlayer player) {
        long now = System.currentTimeMillis();
        if (now - lastScanTime > SCAN_INTERVAL_MS) {
            lastScanTime = now;
            // Run scan in a background thread
            new Thread(() -> {
                cachedDungeonMusic = scanForDungeonMusic(player);
            }, "DungeonMusicScanThread").start();
        }
        return cachedDungeonMusic;
    }


    /**
     * Checks for a dungeon around the player and returns the music file if found.
     * @param player The player to check around.
     * @return The music file if a dungeon is found, otherwise null.
     */
    public static String scanForDungeonMusic(EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        String[] dungeonDefs = BiomeMusicConfig.dungeonDefinitionOptions.dungeonDefinitionList;
        int minBlocks = BiomeMusicConfig.dungeonDefinitionOptions.minBlocks;
        int minSpawners = BiomeMusicConfig.dungeonDefinitionOptions.minSpawners;
        int radius = BiomeMusicConfig.dungeonDefinitionOptions.dungeonRadius;

        for (String def : dungeonDefs) {
            String[] parts = def.split(":");
            if (parts.length < 3) continue;
            String blockName = parts[0] + ":" + parts[1];
            String music = parts[2];

            Block block = Block.getBlockFromName(blockName);
            if (block == null) continue;

            int blockCount = 0;
            int spawnerCount = 0;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos checkPos = playerPos.add(x, y, z);
                        if (!world.isBlockLoaded(checkPos)) continue;

                        if (world.getBlockState(checkPos).getBlock() == block) {
                            blockCount++;
                        }
                        TileEntity te = world.getTileEntity(checkPos);
                        if (te instanceof TileEntityMobSpawner) {
                            spawnerCount++;
                        }
                    }
                }
            }
            if (blockCount >= minBlocks && spawnerCount >= minSpawners) {
                return music;
            }
        }
        return null;
    }
}