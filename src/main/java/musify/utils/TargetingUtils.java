package musify.utils;

import musify.Musify;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class TargetingUtils {

    private static final Map<UUID, Long> targetingMobs = new HashMap<>();
    private static final int CACHE_TIMEOUT = 200;

    /**
     * Counts the number of hostile mobs targeting the specified player within a given radius.
     *
     * - Mobs with the player as their attack target are added to the cache.
     * - If a mob is already in the cache, its timeout is renewed.
     * - Mobs that have not updated within the CACHE_TIMEOUT period are removed.
     *
     * @param player The player to check for mobs targeting.
     * @param radius The radius to search for mobs.
     * @return The count of hostile mobs currently targeting the player.
     */
    public static int countMobsTargetingPlayer(EntityPlayer player, double radius) {
        World world = player.getEntityWorld();

        AxisAlignedBB searchArea = new AxisAlignedBB(
                player.posX - radius, player.posY - radius, player.posZ - radius,
                player.posX + radius, player.posY + radius, player.posZ + radius
        );
        List<EntityLiving> entities = world.getEntitiesWithinAABB(EntityLiving.class, searchArea);

        int count = 0;
        for (EntityLiving mob : entities) {
            // Only count mobs that are alive and not peaceful
            if (!mob.isEntityAlive() || mob.isAIDisabled()) continue;

            if (mob.getAttackTarget() != null && mob.getAttackTarget().getUniqueID().equals(Minecraft.getMinecraft().player.getUniqueID())) {
                count++;
                continue;
            }

            if (mob.getRevengeTarget() != null && mob.getRevengeTarget().getUniqueID().equals(Minecraft.getMinecraft().player.getUniqueID())) {
                count++;
                continue;
            }
        }
        Musify.LOGGER.debug("TARGET COUNT: {} for player: {}", count, player.getName());
        return count;
    }
}
