package musify.handlers;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

import static musify.handlers.BiomeMusicEventHandler.activeMusic;
import static musify.handlers.BiomeMusicEventHandler.activeTagMusic;

@Mod.EventBusSubscriber
@SideOnly(Side.CLIENT)
public class MainMenuMusicHandler {

    private static musify.musicplayer.MusicPlayer mainMenuMusicPlayer = null;

    public static boolean isMainMenuMusicPlaying = false;
    public static boolean isPaused = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) throws Exception {

        String mainMenuMusicPath = BiomeMusicConfig.acmainMenuMusic;
        Minecraft mc = Minecraft.getMinecraft();

        if (isMainMenuScreen(mc)) {
            stopVanillaMusicMainMenu();
            if (activeMusic != null) {
                Musify.LOGGER.debug("TRIED STOPPING BIOME MUSIC FOR MAIN MENU");
                activeMusic.stop();
                activeMusic = null;
            }
            if (activeTagMusic != null) {
                activeTagMusic.stop();
                activeTagMusic = null;
            }

            try {
                if (isPaused && isMainMenuMusicPlaying) {
                    if (mainMenuMusicPlayer != null) {
                        mainMenuMusicPlayer.resume();
                        mainMenuMusicPlayer.adjustVolume();
                        isPaused = false;
                    }
                } else {
                    playMainMenuMusic();
                }
            } catch (Exception e) {
                Musify.LOGGER.error("Failed to play main menu music. File not found or invalid: {}", mainMenuMusicPath, e);
            }

            if (isMainMenuMusicPlaying && !isMainMenuScreen(mc) && !isPaused) {
                Musify.LOGGER.debug("STOPPING MAIN MENU MUSIC");
                mainMenuMusicPlayer.pause();

                isPaused = true;
            }
        } else {
            if (isMainMenuMusicPlaying && !isPaused) {
                Musify.LOGGER.debug("STOPPING MAIN MENU MUSIC");
                mainMenuMusicPlayer.pause();
                isPaused = true;
            }
            if (mc.world != null) {
                mainMenuMusicPlayer.stop();
                isMainMenuMusicPlaying = false;
                isPaused = false;
            }
        }
    }

    public static boolean isMainMenuScreen(Minecraft mc) {
        return mc.currentScreen instanceof GuiMainMenu;
    }

    private static void playMainMenuMusic() throws Exception {

        if (isMainMenuMusicPlaying) {
            return;
        }

        String mainMenuMusicConfig = BiomeMusicConfig.acmainMenuMusic;
        String[] musicFiles = mainMenuMusicConfig.split(",");
        String mainMenuMusic = musicFiles[(int) (Math.random() * musicFiles.length)].trim();

        Musify.LOGGER.debug("PLAYING MAIN MENU MUSIC: {}", mainMenuMusic);

        // Stop previous player if running
        if (mainMenuMusicPlayer != null && mainMenuMusicPlayer.isPlaying()) {
            mainMenuMusicPlayer.stop();
        }

        mainMenuMusicPlayer = new musify.musicplayer.MusicPlayer(mainMenuMusic, false);
        mainMenuMusicPlayer.play();

        isMainMenuMusicPlaying = true;
    }

    @SideOnly(Side.CLIENT)
    public static void stopVanillaMusicMainMenu() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            MusicTicker musicTicker = mc.getMusicTicker();
            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);
            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);

            if (currentMusic != null) {
                new Thread(() -> {
                    mc.getSoundHandler().stopSound(currentMusic);
                }).start();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
