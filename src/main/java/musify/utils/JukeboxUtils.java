package musify.utils;

import net.minecraft.block.BlockJukebox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class JukeboxUtils {

    /**
     * Checks if there is a jukebox within the specified radius of the player that is currently playing music.
     *
     * @param player The player to check around.
     * @param radius The search radius.
     * @return true if a jukebox is nearby and playing, false otherwise.
     */
    public static boolean isJukeBoxNearAndPlaying(EntityPlayer player, double radius) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();
        int r = (int) Math.ceil(radius);

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (world.getBlockState(checkPos).getBlock() == Blocks.JUKEBOX) {
                        if (world.getBlockState(checkPos).getValue(BlockJukebox.HAS_RECORD)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}