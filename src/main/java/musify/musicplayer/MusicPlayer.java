package musify.musicplayer;

import musify.Musify;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;

import javax.sound.sampled.*;
import java.io.File;

public class MusicPlayer implements Runnable {
    private final File mp3File;
    private volatile boolean playing = false;
    private Thread playThread;
    private volatile boolean paused = false;
    private SourceDataLine line;
    private FloatControl gainControl;
    private String fileName = null;

    /**
     * Constructs a MusicPlayer for the specified music file.
     * @param name The name of the music file (should be in the Musify music folder).
     */
    public MusicPlayer(String name) {
        String filePath = Musify.musicFolder.getPath() + "/" + name;
        this.mp3File = new File(filePath);
        fileName = name;
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
                stop();
            }
        }, "MusicPlayer-FadeOut-" + mp3File.getName()).start();
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
        return fileName;
    }
}