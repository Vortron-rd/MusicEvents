package musicevents.musicplayer;

import musicevents.MusicEvents;
import musicevents.utils.RandomUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;

import javax.annotation.Nonnull;
import javax.sound.sampled.*;
import java.io.File;

import static musicevents.config.MusicEventsConfig.*;

public class MusicPlayer implements Runnable {
    public String playlist;
    private File musicFile;
    private volatile boolean playing = false;
    private Thread playThread;
    private volatile boolean paused = false;
    private SourceDataLine line;
    private FloatControl gainControl;
    public String fileName = null;
    private volatile boolean isFading = false;

    /**
     * Constructs a MusicPlayer for the specified music file.
     * @param playlist The name of the music file (should be in the musicevents music folder).
     */
    public MusicPlayer(String playlist) {
        this.playlist = playlist;
        setMusicFileFromPlaylist();
    }
    /** Sets the current musicFile from a random entry in the playlist */
    public void setMusicFileFromPlaylist() {
        if (playlist != null) setMusicFileFromName(RandomUtils.getRandomValueFromListExcluding(playlist, fileName));
    }
    /** Sets the current music file from a file in the music folder.*/
    private void setMusicFileFromName(String name) {
        String filePath = MusicEvents.musicFolder.getPath() + "/" + name;
        fileName = name;
        try {
            musicFile = new File(filePath);
        } catch (Exception e) {
            MusicEvents.LOGGER.error("Cannot access file :{}\n", filePath);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        MusicEvents.LOGGER.debug("Setting Music file to : {}", filePath);
    }

    /**
     * Constructs a MusicPlayer for the specified music file.
     */
    public void play() {
        if (playing) return;
        playing = true;
        playThread = new Thread(this, "MusicPlayer-" + musicFile.getName());
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
            try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(musicFile)) {
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
                try (AudioInputStream ignored1 = AudioSystem.getAudioInputStream(decodedFormat, audioIn)) {
                    isFading = true;


                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(decodedFormat);
                    gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

                    float targetVolume = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC);
                    float min = gainControl.getMinimum();
                    float max = gainControl.getMaximum();
                    float targetDb = (targetVolume == 0.0f) ? min : (float) (fadeOptions.musicVolumeMultiplier * (20.0 * Math.log10(targetVolume)));
                    targetDb = Math.max(min, Math.min(targetDb, max));
                    gainControl.setValue(min);

                    line.start();

                    Thread fadeThread = getThread(fadeMillis, targetDb, min);
                    fadeThread.start();

                    byte[] buffer = new byte[4096];
                    do {
                        try (AudioInputStream loopStream = AudioSystem.getAudioInputStream(decodedFormat, AudioSystem.getAudioInputStream(musicFile))) {
                            int bytesRead;
                            while (playing && (bytesRead = loopStream.read(buffer, 0, buffer.length)) != -1) {
                                while (paused) {
                                    Thread.sleep(10);
                                }
                                line.write(buffer, 0, bytesRead);
                            }
                            setMusicFileFromPlaylist();
                        }
                    } while (playing);
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
        }, "MusicPlayer-FadeIn-" + musicFile.getName());
        playThread.start();
    }

    @Nonnull
    private Thread getThread(int fadeMillis, float targetDb, float min) {
        return new Thread(() -> {
            int steps = 50;
            long sleep = fadeMillis / steps;
            for (int i = 1; i <= steps && playing; i++) {
                float db = min + (targetDb - min) * i / steps;
                gainControl.setValue(db);
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
            gainControl.setValue(targetDb);
            isFading = false;

        });
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

                stop();
            }
        }, "MusicPlayer-FadeOut-" + musicFile.getName()).start();
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
        }, "MusicPlayer-PauseFadeOut-" + musicFile.getName()).start();
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
        float targetDb = (targetVolume == 0.0f) ? min : (float) (fadeOptions.musicVolumeMultiplier * (20.0 * Math.log10(targetVolume)));
        targetDb = Math.max(min, Math.min(targetDb, max));
        gainControl.setValue(min);
        line.start();

        float finalTargetDb = targetDb;
        new Thread(() -> {
            try {
                isFading = true;

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

            }
        }, "MusicPlayer-ResumeFadeIn-" + musicFile.getName()).start();
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
                MusicEvents.LOGGER.warn("Volume control not supported for this audio line.");
            }
        }
    }

    @Override
    public void run() {
        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(musicFile)) {
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
                do {
                    try (AudioInputStream loopStream = AudioSystem.getAudioInputStream(decodedFormat, AudioSystem.getAudioInputStream(musicFile))) {
                        int bytesRead;
                        while (playing && (bytesRead = loopStream.read(buffer, 0, buffer.length)) != -1) {
                            while (paused) {
                                Thread.sleep(10);
                            }
                            line.write(buffer, 0, bytesRead);
                        }
                        setMusicFileFromPlaylist();
                    }
                } while (playing);


                line.drain();
                line.stop();
                line.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            MusicEvents.LOGGER.error("\n playlist: {}\n currentFile: {}", playlist, fileName);
        } finally {
            playing = false;
        }
    }


}