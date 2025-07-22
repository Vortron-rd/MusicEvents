package musify.handlers;

import musify.Musify;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static musify.handlers.BiomeMusicEventHandler.activeMusic;
import static musify.handlers.BiomeMusicEventHandler.activeTagMusic;
import static musify.handlers.HandleCombatMusic.getCombatMusicPlayer;
import static musify.handlers.HandleCombatMusic.isCombatMusicPlaying;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class PauseEventHandler {

    private boolean donePause = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world == null && !MainMenuMusicHandler.isMainMenuMusicPlaying) {
            if (activeMusic != null) {
                activeMusic.stop();
                activeMusic = null;
            }
            if (activeTagMusic != null) {
                activeTagMusic.stop();
                activeTagMusic = null;
            }
        }

        if (isPauseMenuOpen(mc) && !donePause) {
            if (activeMusic != null) {
                if (!activeMusic.isPaused()) {
                    activeMusic.pause();
                }
            }
            if (activeTagMusic != null) {
                if (!activeTagMusic.isPaused()) {
                    activeTagMusic.pause();
                }
            }
            if (getCombatMusicPlayer() != null) {
                getCombatMusicPlayer().pause();
            }
            donePause = true;
        } else if (donePause && !isPauseMenuOpen(mc)) {
            if (activeMusic != null) {
                if (activeMusic.isPaused() && !isCombatMusicPlaying()) {
                    activeMusic.resume();
                    activeMusic.adjustVolume();
                }
            }
            if (activeTagMusic != null) {
                if (activeTagMusic.isPaused() && !isCombatMusicPlaying()) {
                    activeTagMusic.resume();
                    activeTagMusic.adjustVolume();
                }
            }
            if (getCombatMusicPlayer() != null) {
                if (getCombatMusicPlayer().isPaused()) {
                    getCombatMusicPlayer().resume();
                    if (isCombatMusicPlaying()) {
                        getCombatMusicPlayer().adjustVolume();
                    }
                }
            }
            donePause = false;
        }
    }



    private boolean isPauseMenuOpen(Minecraft mc) {
        if (mc.currentScreen == null) {
            return false;
        }

        boolean isKnownPauseMenu = mc.currentScreen instanceof GuiIngameMenu
                || mc.currentScreen instanceof GuiOptions
                || mc.currentScreen instanceof GuiStats
                || mc.currentScreen instanceof GuiVideoSettings
                || mc.currentScreen instanceof GuiControls
                || mc.currentScreen instanceof GuiLanguage
                || mc.currentScreen instanceof GuiScreenOptionsSounds
                || mc.currentScreen instanceof GuiConfig
                || mc.currentScreen instanceof GuiGameOver
                || mc.currentScreen instanceof GuiScreenResourcePacks
                || mc.currentScreen instanceof GuiShareToLan
                || mc.currentScreen instanceof GuiModList
                || mc.currentScreen instanceof GuiCustomizeSkin
                || mc.currentScreen instanceof GuiSnooper
                || mc.currentScreen instanceof ScreenChatOptions
                ;

        if (mc.currentScreen.getClass().getName().contains("Advancement")) {
            return true;
        }

        if (mc.currentScreen.getClass().getName().contains("LockIconButton")) {
            return true;
        }

        return isKnownPauseMenu;
    }
}