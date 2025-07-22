package musify.handlers;

import musify.Musify;
import musify.musicplayer.MusicPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

import static musify.config.BiomeMusicConfig.*;
import static musify.handlers.BiomeMusicEventHandler.*;
import static musify.musicplayer.MusicPlayer.getRandomSongForCombat;

public class HandleCombatMusic {

    private static boolean isCombatMusicPlaying = false;
    public static MusicPlayer combatMusicPlayer = null;
    public static boolean isCombatMusicFading = false;
    public static boolean backgroundCombatMusic = false;

    @SideOnly(Side.CLIENT)
    public static void handleCombatMusic() {

        if (!isCombatMusicPlaying && backgroundCombatMusic) {
            isCombatMusicPlaying = true;
            if (activeMusic != null && !activeMusic.isFading()) {
                activeMusic.fadeOut(fadeOptions.combatMusicFadeInTime);
            }
            if (activeTagMusic != null && !activeTagMusic.isFading()) {
                activeTagMusic.fadeOut(fadeOptions.combatMusicFadeInTime);
            }

            if (!isVanillaMusicFading) {
                if (!adambientMode) {
                    stopVanillaMusic();
                }
            }
            combatMusicPlayer.fadeIn(fadeOptions.combatMusicFadeInTime);
        } else if (!isCombatMusicPlaying && !backgroundCombatMusic) {
            isCombatMusicPlaying = true;

            String linkedMusic = null;
            if (Objects.equals(combatOptions.combatMusicList, "default_music")) {
                Musify.LOGGER.warn("No combat music specified. If you do not plan on using combat music, please disable it in the config.");
                return;
            }
            linkedMusic = getRandomSongForCombat();
            if (activeMusic != null && !activeMusic.isFading()) {
                activeMusic.stopWithFadeOut(fadeOptions.combatMusicFadeInTime);
                activeMusic = null;
            }
            if (activeTagMusic != null && !activeTagMusic.isFading()) {
                activeTagMusic.stopWithFadeOut(fadeOptions.combatMusicFadeInTime);
                activeTagMusic = null;
            }
            startCombatMusicWithFadeIn(linkedMusic);
        }
    }

    public static void startCombatMusic(String music) {
        combatMusicPlayer = new MusicPlayer(music, true);
        combatMusicPlayer.playSilent();
        backgroundCombatMusic = true;
    }

    public static void startCombatMusicWithFadeIn(String music) {
        combatMusicPlayer = new MusicPlayer(music, true);
        combatMusicPlayer.playWithFadeIn(fadeOptions.combatMusicFadeInTime);
        backgroundCombatMusic = true;
    }
    public static boolean isCombatMusicPlaying() {
        return isCombatMusicPlaying;
    }

    public static void setCombatMusicPlaying(boolean isPlaying) {
        isCombatMusicPlaying = isPlaying;
    }

    public static MusicPlayer getCombatMusicPlayer() {
        return combatMusicPlayer;
    }

}
