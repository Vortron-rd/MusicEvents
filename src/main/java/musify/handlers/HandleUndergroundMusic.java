package musify.handlers;

import musify.musicplayer.MusicPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static musify.config.BiomeMusicConfig.iundergroundOptions;
import static musify.config.BiomeMusicConfig.lfadeOptions;
import static musify.handlers.BiomeMusicEventHandler.activeMusic;

public class HandleUndergroundMusic {

    private static final Random random = new Random();
    public static boolean isUndergroundMusicPlaying = false;


    public static void handleUndergroundMusic() {
        if (!isUndergroundMusicPlaying) {
            if (activeMusic != null && !activeMusic.isFading()) {
                activeMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                String musicFile = getRandomSongForCavern();
                if (!musicFile.equals("default_music") && musicFile != null) {
                    isUndergroundMusicPlaying = true;
                    activeMusic = new MusicPlayer(musicFile, false);
                    activeMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                } else return;
            } else if (activeMusic == null) {
                String musicFile = getRandomSongForCavern();
                if (!musicFile.equals("default_music") && musicFile != null) {
                    isUndergroundMusicPlaying = true;
                    activeMusic = new MusicPlayer(musicFile, false);
                    activeMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                } else return;
            }
        }
    }


    public static String getRandomSongForCavern() {
        String songList = (iundergroundOptions.CavernMusic);

        List<String> songs = Arrays.asList(songList.split(","));

        return songs.get(random.nextInt(songs.size())).trim();
    }

}
