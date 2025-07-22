package musify.handlers;

import musify.Musify;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static musify.config.BiomeMusicConfig.adambientMode;
import static musify.handlers.BiomeMusicEventHandler.activeMusic;
import static musify.handlers.BiomeMusicEventHandler.activeTagMusic;
import static musify.handlers.HandleCombatMusic.getCombatMusicPlayer;

@Mod.EventBusSubscriber
public class DisableVanillaMusic {

    @SubscribeEvent
    public static void disableVanillaMusic(PlaySoundEvent event) {
        if (event.getSound().getCategory() == SoundCategory.MUSIC && !adambientMode) {
            if (activeMusic != null && activeMusic.isPlaying()) {
                event.setResultSound(null);
                Musify.LOGGER.debug("Disabling Vanilla Music 1");
            }
            if (activeTagMusic != null && activeTagMusic.isPlaying()) {
                event.setResultSound(null);
                Musify.LOGGER.debug("Disabling Vanilla Music 2");
            }
            if (getCombatMusicPlayer() != null && getCombatMusicPlayer().isPlaying()) {
                event.setResultSound(null);
                Musify.LOGGER.debug("Disabling Vanilla Music 3");
            }
        }
    }

}
