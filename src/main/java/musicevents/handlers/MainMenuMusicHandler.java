package musicevents.handlers;

import musicevents.MusicEvents;
import musicevents.config.MusicEventsConfig;
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

import static musicevents.handlers.EventHandler.activeMusic;

@Mod.EventBusSubscriber
@SideOnly(Side.CLIENT)
public class MainMenuMusicHandler {

    private static musicevents.musicplayer.MusicPlayer mainMenuMusicPlayer = null;
    private static boolean errq = false;

    public static boolean isMainMenuMusicPlaying = false;
    public static boolean isPaused = false;

    private static int ticksSinceStart = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ticksSinceStart++;
        if (ticksSinceStart < 20) return;

        String mainMenuMusicPath = MusicEventsConfig.mainMenuMusic;
        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null) return;

        if (mainMenuMusicPath != null && !mainMenuMusicPath.isEmpty()) {

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
            if (mainMenuMusicPath.equals("") && isMainMenuMusicPlaying && mainMenuMusicPlayer != null) {
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
            MusicEvents.LOGGER.error("Failed to play main menu music. File not found or invalid: {}", mainMenuMusicPath, e);
        }
    }


    public static boolean isMainMenuScreen(Minecraft mc) {
        return mc.currentScreen instanceof GuiMainMenu;
    }

    private static void playMainMenuMusic() {

        if (isMainMenuMusicPlaying || errq) {
            return;
        }

        String mainMenuMusicConfig = MusicEventsConfig.mainMenuMusic;
        String[] musicFiles = mainMenuMusicConfig.split(",");
        String mainMenuMusic = musicFiles[(int) (Math.random() * musicFiles.length)].trim();

        if (mainMenuMusic.isEmpty()) {
            MusicEvents.LOGGER.warn("Main menu music enabled, but set to default. Please disable it or set a custom music file.");
            errq = true;
            return;
        }

        if (mainMenuMusicPlayer != null && mainMenuMusicPlayer.isPlaying()) {
            mainMenuMusicPlayer.stop();
        }

        mainMenuMusicPlayer = new musicevents.musicplayer.MusicPlayer(mainMenuMusicConfig);
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
