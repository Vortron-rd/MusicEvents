package musify.musicplayer;

import musify.Musify;
import musify.handlers.HandleCombatMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;

import javax.sound.sampled.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static musify.config.BiomeMusicConfig.combatOptions;
import static musify.config.BiomeMusicConfig.musicLink;

public class MusicPlayer implements Runnable {
    private final File mp3File;
    private volatile boolean playing = false;
    private Thread playThread;
    private volatile boolean paused = false;
    private SourceDataLine line;
    private FloatControl gainControl;
    public String fileName = null;
    private volatile boolean isFading = false;
    public volatile boolean isCombatMusic = false;
    private String currentCombatMusic = null;
    private static final Random random = new Random();

    /**
     * Constructs a MusicPlayer for the specified music file.
     * @param name The name of the music file (should be in the Musify music folder).
     */
    public MusicPlayer(String name, boolean isCombatMusic) {
        String filePath = Musify.musicFolder.getPath() + "/" + name;
        this.mp3File = new File(filePath);
        fileName = name;
        this.isCombatMusic = isCombatMusic;
        Musify.LOGGER.debug("FILE PATH: {}", filePath);
    }

    /**
     * Constructs a MusicPlayer for the specified music file.
     */
    public void play() {
        if (playing) return;
        playing = true;
        playThread = new Thread(this, "MusicPlayer-" + mp3File.getName());
        playThread.start();
    }

    /**
     * Stops the music playback.
     */
    public void stop() {
        playing = false;
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    public String getFilename() {
        if (fileName != null) {
            return fileName;
        }
        else return "No Music Set";
    }

    /**
     * Plays the music file with a fade-in effect.
     * @param fadeMillis The duration of the fade-in effect in milliseconds.
     */
    public void playWithFadeIn(int fadeMillis) {
        if (playing) return;
        playing = true;
        playThread = new Thread(() -> {
            try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(mp3File)) {
                AudioFormat baseFormat = audioIn.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );
                try (AudioInputStream decodedIn = AudioSystem.getAudioInputStream(decodedFormat, audioIn)) {
                    isFading = true;

                    // COMBAT MUSIC STUFF
                    if (combatOptions.enableCombatMusic && !this.isCombatMusic) {
                        String linkedMusic = null;
                        linkedMusic = musicLink.get(fileName);
                        if (Objects.equals(linkedMusic, "") || Objects.equals(linkedMusic, null)) {
                            if (Objects.equals(combatOptions.combatMusicList, "default_music")) {
                                Musify.LOGGER.warn("No combat music specified. If you do not plan on using combat music, please disable it in the config.");
                            }
                            linkedMusic = getRandomSongForCombat();
                        }
                        if (!Objects.equals(linkedMusic, "")) {
                            HandleCombatMusic.startCombatMusic(linkedMusic);
                            currentCombatMusic = linkedMusic;
                        }
                    }

                    if (this.isCombatMusic) {
                        HandleCombatMusic.isCombatMusicFading = true;
                    }



                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(decodedFormat);
                    gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

                    // Fade-in: start at min volume
                    float targetVolume = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC);
                    float min = gainControl.getMinimum();
                    float max = gainControl.getMaximum();
                    float targetDb = (targetVolume == 0.0f) ? min : (float) (20.0 * Math.log10(targetVolume));
                    targetDb = Math.max(min, Math.min(targetDb, max));
                    gainControl.setValue(min);

                    line.start();

                    // Start fade-in in a separate thread
                    float finalTargetDb = targetDb;
                    Thread fadeThread = new Thread(() -> {
                        int steps = 50;
                        long sleep = fadeMillis / steps;
                        for (int i = 1; i <= steps && playing; i++) {
                            float db = min + (finalTargetDb - min) * i / steps;
                            gainControl.setValue(db);
                            try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
                        }
                        gainControl.setValue(finalTargetDb);
                        isFading = false;
                        if (this.isCombatMusic) {
                            HandleCombatMusic.isCombatMusicFading = false;
                        }
                    });
                    fadeThread.start();

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while (playing && (bytesRead = decodedIn.read(buffer, 0, buffer.length)) != -1) {
                        while (paused) {
                            Thread.sleep(10);
                        }
                        line.write(buffer, 0, bytesRead);
                    }
                    line.drain();
                    line.stop();
                    line.close();
                    playing = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                playing = false;
            }
        }, "MusicPlayer-FadeIn-" + mp3File.getName());
        playThread.start();
    }

    /**
     * Plays the music file silently (with volume set to minimum).
     * This is useful for starting the music without audible sound.
     */
    public void playSilent() {
        if (playing) return;
        playing = true;
        playThread = new Thread(() -> {
            try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(mp3File)) {
                AudioFormat baseFormat = audioIn.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );
                try (AudioInputStream decodedIn = AudioSystem.getAudioInputStream(decodedFormat, audioIn)) {
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(decodedFormat);
                    gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

                    // Set volume to minimum (silent)
                    float min = gainControl.getMinimum();
                    gainControl.setValue(min);

                    line.start();

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while (playing && (bytesRead = decodedIn.read(buffer, 0, buffer.length)) != -1) {
                        while (paused) {
                            Thread.sleep(10);
                        }
                        line.write(buffer, 0, bytesRead);
                    }
                    line.drain();
                    line.stop();
                    line.close();
                    playing = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                playing = false;
            }
        }, "MusicPlayer-Silent-" + mp3File.getName());
        playThread.start();
    }

    /**
     * Stops the music with a fade-out effect.
     * @param fadeMillis The duration of the fade-out effect in milliseconds.
     */
    public void stopWithFadeOut(int fadeMillis) {
        if (!playing || line == null || gainControl == null) {
            stop();
            return;
        }
        new Thread(() -> {
            try {
                if (this.isCombatMusic) {
                    HandleCombatMusic.isCombatMusicFading = true;
                }
                isFading = true;
                float min = gainControl.getMinimum();
                float currentDb = gainControl.getValue();
                int steps = 50;
                long sleep = fadeMillis / steps;
                for (int i = 1; i <= steps && playing; i++) {
                    float db = currentDb + (min - currentDb) * i / steps;
                    gainControl.setValue(db);
                    Thread.sleep(sleep);
                }
                gainControl.setValue(min);
            } catch (InterruptedException ignored) {
            } finally {
                isFading = false;
                if (this.isCombatMusic && HandleCombatMusic.isCombatMusicFading) {
                    HandleCombatMusic.isCombatMusicFading = false;
                }
                stop();
            }
        }, "MusicPlayer-FadeOut-" + mp3File.getName()).start();
    }


    /** * Pauses the music with a fade-out effect.
     * @param fadeMillis The duration of the fade-out effect in milliseconds.
     */
    public void pauseWithFadeOut(int fadeMillis) {
        if (!playing || line == null || gainControl == null || paused) {
            return;
        }
        new Thread(() -> {
            try {
                isFading = true;
                float min = gainControl.getMinimum();
                float currentDb = gainControl.getValue();
                int steps = 50;
                long sleep = fadeMillis / steps;
                for (int i = 1; i <= steps && playing && !paused; i++) {
                    float db = currentDb + (min - currentDb) * i / steps;
                    gainControl.setValue(db);
                    Thread.sleep(sleep);
                }
                gainControl.setValue(min);
                paused = true;
                if (line.isRunning()) {
                    line.stop();
                }
            } catch (InterruptedException ignored) {
            } finally {
                isFading = false;
            }
        }, "MusicPlayer-PauseFadeOut-" + mp3File.getName()).start();
    }

    /** * Resumes the music playback with a fade-in effect.
     * @param fadeMillis The duration of the fade-in effect in milliseconds.
     */
    public void resumeWithFadeIn(int fadeMillis) {
        if (!paused || line == null || gainControl == null || isFading) {
            return;
        }
        paused = false;
        float min = gainControl.getMinimum();
        float max = gainControl.getMaximum();
        float targetVolume = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC);
        float targetDb = (targetVolume == 0.0f) ? min : (float) (20.0 * Math.log10(targetVolume));
        targetDb = Math.max(min, Math.min(targetDb, max));
        gainControl.setValue(min);
        line.start();

        float finalTargetDb = targetDb;
        new Thread(() -> {
            try {
                isFading = true;
                if (this.isCombatMusic) {
                    HandleCombatMusic.isCombatMusicFading = true;
                }
                int steps = 50;
                long sleep = fadeMillis / steps;
                for (int i = 1; i <= steps && playing && !paused; i++) {
                    float db = min + (finalTargetDb - min) * i / steps;
                    gainControl.setValue(db);
                    Thread.sleep(sleep);
                }
                gainControl.setValue(finalTargetDb);
            } catch (InterruptedException ignored) {
            } finally {
                isFading = false;
                if (this.isCombatMusic) {
                    HandleCombatMusic.isCombatMusicFading = false;
                }
            }
        }, "MusicPlayer-ResumeFadeIn-" + mp3File.getName()).start();
    }

    /** * Fades in the music playback over a specified duration.
     * @param fadeMillis The duration of the fade-in effect in milliseconds.
     */
    public void fadeIn(int fadeMillis) {
        if (!playing || line == null || gainControl == null || isFading) {
            return;
        }
        new Thread(() -> {
            try {
                isFading = true;
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                float targetVolume = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC);
                float targetDb = (targetVolume == 0.0f) ? min : (float) (20.0 * Math.log10(targetVolume));
                targetDb = Math.max(min, Math.min(targetDb, max));
                float currentDb = gainControl.getValue();
                int steps = 50;
                long sleep = fadeMillis / steps;
                for (int i = 1; i <= steps && playing && !paused; i++) {
                    float db = currentDb + (targetDb - currentDb) * i / steps;
                    gainControl.setValue(db);
                    Thread.sleep(sleep);
                }
                gainControl.setValue(targetDb);
            } catch (InterruptedException ignored) {
            } finally {
                isFading = false;
            }
        }, "MusicPlayer-FadeIn-" + mp3File.getName()).start();
    }

    /** * Fades out the music playback over a specified duration.
     * @param fadeMillis The duration of the fade-out effect in milliseconds.
     */
    public void fadeOut(int fadeMillis) {
        if (!playing || line == null || gainControl == null || isFading) {
            return;
        }
        new Thread(() -> {
            try {
                isFading = true;
                float min = gainControl.getMinimum();
                float currentDb = gainControl.getValue();
                int steps = 50;
                long sleep = fadeMillis / steps;
                for (int i = 1; i <= steps && playing && !paused; i++) {
                    float db = currentDb + (min - currentDb) * i / steps;
                    gainControl.setValue(db);
                    Thread.sleep(sleep);
                }
                gainControl.setValue(min);
            } catch (InterruptedException ignored) {
            } finally {
                isFading = false;
            }
        }, "MusicPlayer-FadeOutOnly-" + mp3File.getName()).start();
    }

    public boolean isFading() {
        return isFading;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void pause() {
        if (line != null && line.isRunning()) {
            paused = true;
            line.stop();
        }
    }

    public void resume() {
        if (line != null && paused) {
            paused = false;
            line.start();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    /**
     * Adjusts the volume of the music playback based on the game's sound settings.
     * To be called whenever the sound settings could have been changed. (like exiting settings menu)
     */
    public void adjustVolume() {
        if (gainControl != null) {
            try {
                float volume = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC);
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                float dB = (volume == 0.0f) ? min : (float) (20.0 * Math.log10(volume));
                dB = Math.max(min, Math.min(dB, max));
                gainControl.setValue(dB);
            } catch (IllegalArgumentException e) {
                Musify.LOGGER.warn("Volume control not supported for this audio line.");
            }
        }
    }

    @Override
    public void run() {
        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(mp3File)) {
            AudioFormat baseFormat = audioIn.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            try (AudioInputStream decodedIn = AudioSystem.getAudioInputStream(decodedFormat, audioIn)) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(decodedFormat);
                gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                adjustVolume();
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while (playing && (bytesRead = decodedIn.read(buffer, 0, buffer.length)) != -1) {
                    while (paused) {
                        Thread.sleep(10);
                    }
                    line.write(buffer, 0, bytesRead);
                }
                line.drain();
                line.stop();
                line.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            playing = false;
        }
    }

    public String getFileName() {
        if (fileName == null) {
            return "null";
        }
        return fileName;
    }

    // ------------- HELPER METHODS ------------- //
    public static String getRandomSongForCombat() {
        String songList = (combatOptions.combatMusicList);

        List<String> songs = Arrays.asList(songList.split(","));

        return songs.get(random.nextInt(songs.size())).trim();
    }

}