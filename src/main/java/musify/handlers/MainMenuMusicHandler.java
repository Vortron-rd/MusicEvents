package musify.handlers;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.fml.common.Loader;
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
    private static boolean errq = false;

    public static boolean isMainMenuMusicPlaying = false;
    public static boolean isPaused = false;

    private static int ticksSinceStart = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ticksSinceStart++;
        if (ticksSinceStart < 20) return; // wait 1 second (20 ticks)

        String mainMenuMusicPath = BiomeMusicConfig.acmainMenuMusic;
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null) return;

        if (mainMenuMusicPath != null && !mainMenuMusicPath.equals("default_music")) {

            if (!Loader.isModLoaded("custommainmenu")) {

                if (isMainMenuScreen(mc)) {
                    stopVanillaMusicMainMenu();
                    mainMenuMusicSetup(mainMenuMusicPath);

                }
                else {
                    if (mainMenuMusicPlayer != null && isMainMenuMusicPlaying && !isMainMenuScreen(mc) && !isPaused) {
                        mainMenuMusicPlayer.pause();

                        isPaused = true;
                    }
                    if (mainMenuMusicPlayer != null && mc.world != null) {
                        mainMenuMusicPlayer.stop();
                        isMainMenuMusicPlaying = false;
                        isPaused = false;
                    }
                }
            }
            else if (Loader.isModLoaded("custommainmenu")) {
                if (mc.currentScreen != null && mc.currentScreen.getClass().getName().contains("lumien.custommainmenu")) {
                    stopVanillaMusicMainMenu();
                    mainMenuMusicSetup(mainMenuMusicPath);
                } else {
                    if (mainMenuMusicPlayer != null && isMainMenuMusicPlaying && !isPaused) {
                        mainMenuMusicPlayer.pause();
                        isPaused = true;
                        if (mainMenuMusicPlayer != null && mc.world != null) {
                            mainMenuMusicPlayer.stop();
                            isMainMenuMusicPlaying = false;
                            isPaused = false;
                        }
                    }

                }
            }
            else {
                if (mainMenuMusicPlayer != null && mc.world != null) {
                    mainMenuMusicPlayer.stop();
                    isMainMenuMusicPlaying = false;
                    isPaused = false;
                }
            }
        }
        else {
            if (mainMenuMusicPath.equals("default_music") && isMainMenuMusicPlaying && mainMenuMusicPlayer != null) {
                mainMenuMusicPlayer.stop();

                isMainMenuMusicPlaying = false;
            }
        }
    }

    private static void mainMenuMusicSetup(String mainMenuMusicPath) {
        if (activeMusic != null) {
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
    }


    public static boolean isMainMenuScreen(Minecraft mc) {
        return mc.currentScreen instanceof GuiMainMenu;
    }

    private static void playMainMenuMusic() throws Exception {

        if (isMainMenuMusicPlaying || errq) {
            return;
        }

        String mainMenuMusicConfig = BiomeMusicConfig.acmainMenuMusic;
        String[] musicFiles = mainMenuMusicConfig.split(",");
        String mainMenuMusic = musicFiles[(int) (Math.random() * musicFiles.length)].trim();

        if (mainMenuMusic.equals("default_music")) {
            Musify.LOGGER.warn("Main menu music enabled, but set to default. Please disable it or set a custom music file.");
            errq = true;
            return;
        }

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
