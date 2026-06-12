package musicevents.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

import static musicevents.config.MusicEventsConfig.ambientMode;
import static musicevents.handlers.EventHandler.activeMusic;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class DisableVanillaMusic {

    @SubscribeEvent
    public static void disableVanillaMusic(PlaySoundEvent event) {
        if (event.getSound().getCategory() == SoundCategory.MUSIC && !ambientMode) {
            if (activeMusic != null && activeMusic.isPlaying()) {
                event.setResultSound(null);
            }

        }
    }
    @SideOnly(Side.CLIENT)
    public static void stopVanillaMusic() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            MusicTicker musicTicker = mc.getMusicTicker();

            // Stop current vanilla music
            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);
            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);
            if (currentMusic != null) {
                mc.getSoundHandler().stopSound(currentMusic);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
