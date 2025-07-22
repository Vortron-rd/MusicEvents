package musify.utils;

import musify.musicplayer.MusicPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Objects;

import static musify.config.BiomeMusicConfig.bossMusicOptions;
import static musify.handlers.BiomeMusicEventHandler.*;

@SideOnly(Side.CLIENT)
public class BossTargetUtils {

    public static boolean isBossMusicPlaying = false;
    private static String currentBossMusic = null;

    @SideOnly(Side.CLIENT)
    public static String bossMusicFile(EntityPlayer player) {
        if (bossMusicOptions.enableBossMusic) {
            for (String entry : bossMusicOptions.bossMusicList) {
                String[] parts = entry.split(",");
                if (parts.length != 2) continue;
                String mobId = parts[0].trim();
                String musicFile = parts[1].trim();
                AxisAlignedBB searchBox = new AxisAlignedBB(
                        player.posX - bossMusicOptions.bossMusicRange, player.posY - bossMusicOptions.bossMusicRange, player.posZ - bossMusicOptions.bossMusicRange,
                        player.posX + bossMusicOptions.bossMusicRange, player.posY + bossMusicOptions.bossMusicRange, player.posZ + bossMusicOptions.bossMusicRange
                );
                List<Entity> nearbyEntities = player.world.getEntitiesWithinAABB(Entity.class, searchBox, entity -> {
                    ResourceLocation entityID = EntityList.getKey(entity);
                    return entityID != null && entityID.toString().equals(mobId);
                });
                if (!nearbyEntities.isEmpty()) {
                    return musicFile;
                }
            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void handleBossMusic(String musicFile) {

        if (!isBossMusicPlaying && activeMusic != null && !activeMusic.isFading()) {
            isBossMusicPlaying = true;
            activeMusic.stopWithFadeOut(7500);
            activeMusic = new MusicPlayer(musicFile, true);
            activeMusic.playWithFadeIn(8500);
            currentBossMusic = musicFile;
            currentMusicFile = musicFile;
        }
        else if (!isBossMusicPlaying && activeMusic == null) {
            if (activeTagMusic != null && !activeTagMusic.isFading()) {
                activeTagMusic.stopWithFadeOut(7500);
            }
            isBossMusicPlaying = true;
            activeMusic = new MusicPlayer(musicFile, true);
            activeMusic.playWithFadeIn(8500);
            currentBossMusic = musicFile;
            currentMusicFile = musicFile;
        } else if (isBossMusicPlaying && !Objects.equals(currentBossMusic, musicFile)) {
            activeMusic.stopWithFadeOut(7500);
            activeMusic = new MusicPlayer(musicFile, true);
            activeMusic.playWithFadeIn(8500);
            currentBossMusic = musicFile;
            currentMusicFile = musicFile;
        }
    }
}