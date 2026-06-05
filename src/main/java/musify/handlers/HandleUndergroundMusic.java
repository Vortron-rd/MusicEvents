package musify.handlers;

import musify.musicplayer.MusicPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static musify.config.BiomeMusicConfig.iundergroundOptions;
import static musify.config.BiomeMusicConfig.lfadeOptions;
import static musify.handlers.BiomeMusicEventHandler.activeMusic;
import static musify.handlers.BiomeMusicEventHandler.activeTagMusic;

public class HandleUndergroundMusic {

    private static final Random random = new Random();
    public static boolean isUndergroundMusicPlaying = false;

    @SuppressWarnings("ConstantConditions")
    public static void handleUndergroundMusic() {
        System.out.println("***********\nCalled handleUndergroundMusic\n**************");
        if (!isUndergroundMusicPlaying) {
            String musicFile = getRandomSongForCavern();

            /* Check for Biome Tag Music */
            if (activeTagMusic != null && !activeTagMusic.isFading()) {
                activeTagMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                System.out.println("\n***********\nSTOPPED activeTagMusic\n**************");

            }

            if (activeMusic != null && !activeMusic.isFading()) {
                activeMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                System.out.println("\n***********\nSTOPPED activeMusic\n**************");
            }

            if (!musicFile.equals("default_music") && musicFile != null) {
                System.out.println("\n***********\nSTARTED activeMusic with Cavern themes\n**************");
                isUndergroundMusicPlaying = true;
                activeMusic = new MusicPlayer(musicFile, false);
                activeMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
            }
        }
    }


    public static String getRandomSongForCavern() {
        String songList = (iundergroundOptions.CavernMusic);

        List<String> songs = Arrays.asList(songList.split(","));

        return songs.get(random.nextInt(songs.size())).trim();
    }

}
