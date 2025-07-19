package musify.handlers;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import musify.musicplayer.MusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.*;

import static musify.config.BiomeMusicConfig.adambientMode;
import static musify.config.BiomeMusicConfig.fadeOptions;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class BiomeMusicEventHandler {


    /**
     * The original music volume setting value for fading vanilla music.
     */
    private static float originalMusicVolume;

    /**
     * The active music player instance that is currently playing music.
     */
    public static MusicPlayer activeMusic = null;
    public static MusicPlayer activeTagMusic = null;

    private static String currentMusicFile = null;

    private static EntityPlayer player;
    private static final Random random = new Random();
    private static int tickCounter = 0;
    private static boolean isVanillaMusicFading = false;

    private static net.minecraft.world.World lastWorld = null;

    /**
     * Handles the player tick event to manage biome music.
     * This method checks the player's current biome and plays the corresponding music based on the biome configuration.
     * It also handles the fading of vanilla music when custom music is played.
     * Based on the polling rate defined in the configuration, it will check the player's biome every few ticks.
     * @param event The player tick event.
     * @throws Exception If an error occurs during the event handling.
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) throws Exception {

        if (player == null || player != event.player || player.getUniqueID() != event.player.getUniqueID()) {
            player = event.player;
        }

        tickCounter++;

        if (fadeOptions.pollingRate == 0) {
            if (tickCounter % 1000 == 0) {
                Musify.LOGGER.error("POLLING RATE IS SET TO 0 IN MUSIFY CONFIG! THIS WILL BREAK THE MOD! \n (forcibly stopped the mod to prevent a crash.)");
            }
            return;
        }

        if (tickCounter % fadeOptions.pollingRate == 0 && event.player != null && event.player.world != null) {

            BlockPos pos = event.player.getPosition();
            Biome biome = event.player.world.getBiome(pos);
            handleBiomeMusic(biome);

        }
    }

    @SideOnly(Side.CLIENT)
    public static void handleBiomeMusic(Biome biome) {
        ResourceLocation biomeRegistryName = biome.getRegistryName();
        if (biomeRegistryName != null) {

            String configSet = BiomeMusicConfig.biomeMusicMap.get(biomeRegistryName.toString());
            String musicFile = null;

            if (configSet != null) {
                musicFile = getRandomSongForBiome(configSet);
            }

            if (musicFile != null && !configSet.equals("default_music") && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
                Musify.LOGGER.debug("PASSED BIOME SPECIFIC MUSIC CHECK");
                if (activeMusic != null && !isMusicBiomeCorrect(currentMusicFile, configSet)) {
                    Musify.LOGGER.debug("PASSED NON NULL CHECK AND CORRECT CHECK");
                    if (activeTagMusic != null) {
                        activeTagMusic.stopWithFadeOut(12500);
                        activeTagMusic = null;
                    }
                    activeMusic.stopWithFadeOut(12500);
                    Musify.LOGGER.debug("ACTIVE MUSIC NOT NULL, PLAYING NEW MUSIC: {}", musicFile);
                    activeMusic = new MusicPlayer(musicFile);
                    currentMusicFile = musicFile;
                    activeMusic.playWithFadeIn(15000);
                } else if (activeMusic == null) {
                    activeMusic = new MusicPlayer(musicFile);
                    currentMusicFile = musicFile;
                    activeMusic.playWithFadeIn(15000);
                    fadeOutVanillaMusic();
                    Musify.LOGGER.debug("ACTIVE MUSIC NULL, PLAYING NEW MUSIC: {}", musicFile);
                    if (activeTagMusic != null) {
                        activeTagMusic.stopWithFadeOut(12500);
                        activeTagMusic = null;
                    }
                }

            } else {
                // getting the biome and checking the config
                Set<BiomeDictionary.Type> biomeTags = BiomeDictionary.getTypes(biome);
                if (!biomeTags.isEmpty() && biome != Biomes.RIVER) {
                    String randomTagMusicFile = biomeTags.stream()
                            .map(type -> getRandomSongForBiomeTag(type.getName().toLowerCase()))
                            .filter(song -> !song.equals("default_music"))
                            .findFirst()
                            .orElse("default_music");

                    if (!randomTagMusicFile.equals("default_music")) {

                        Musify.LOGGER.debug("PASSED DEFAULT CHECK BIOME TAGS");

                        Set<String> possibleSongs = new HashSet<>();
                        // picking one random song from the config list
                        for (BiomeDictionary.Type type : biomeTags) {
                            String possibleSongList = BiomeMusicConfig.biomeTagMusicMap.getOrDefault(type.getName().toLowerCase(), "default_music");
                            String[] songList = possibleSongList.split(",");
                            possibleSongs.addAll(Arrays.asList(songList));
                        }

                        if (activeTagMusic == null) {
                            if (activeMusic != null && activeMusic.isPlaying()) {
                                activeMusic.stopWithFadeOut(12500);
                            }
                            activeTagMusic = new MusicPlayer(randomTagMusicFile);
                            activeTagMusic.playWithFadeIn(15000);
                            currentMusicFile = musicFile;
                            if (!isVanillaMusicFading && !adambientMode) {
                                fadeOutVanillaMusic();
                            }
                        } else if (!activeTagMusic.isPlaying() || !possibleSongs.contains(activeTagMusic.getFileName())) {
                            if (activeMusic != null && activeMusic.isPlaying()) {
                                activeMusic.stopWithFadeOut(12500);
                            }
                            activeTagMusic = new MusicPlayer(randomTagMusicFile);
                            activeTagMusic.playWithFadeIn(15000);
                            currentMusicFile = musicFile;
                            if (!isVanillaMusicFading && !adambientMode) {
                                fadeOutVanillaMusic();
                            }
                        }
                        if (!isVanillaMusicFading && activeTagMusic != null && !adambientMode) {
                            stopVanillaMusic();
                        }
                    } else {
                        Musify.LOGGER.debug("FAILD DEFAULT CHECK BIOME TAGS");
                        if (activeTagMusic != null && activeTagMusic.isPlaying()) {
                            Musify.LOGGER.debug("PASSED TAG MUSIC CHECK");
                            activeTagMusic.stopWithFadeOut(12500);
                        }
                        if (activeMusic != null) {
                            activeMusic.stopWithFadeOut(15000);
                            activeMusic = null;
                        }
                    }
                    if (activeTagMusic != null && activeTagMusic.isPlaying() && !isVanillaMusicFading) {
                        stopVanillaMusic();
                    }
                }
            }
        } else if (activeMusic != null) {
            activeMusic.stopWithFadeOut(12500);
            activeMusic = null;
        }
    }

// ----------------------- VANILLA MUSIC STUFF -----------------------

    /**
     * Fades out the vanilla music when custom music is played.
     * This method retrieves the current music from the MusicTicker and fades it out.
     */
    @SideOnly(Side.CLIENT)
    private static void fadeOutVanillaMusic() {
        try {
            isVanillaMusicFading = true;
            Minecraft mc = Minecraft.getMinecraft();
            originalMusicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
            MusicTicker musicTicker = mc.getMusicTicker();

            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);

            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);

            if (currentMusic != null) {

                new Thread(() -> {
                    try {
                        float volume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
                        float fadeDuration = (float) fadeOptions.vanillaMusicFadeOutTime;
                        float fadeSteps = 100;
                        float stepTime = fadeDuration / fadeSteps;
                        float volumeStep = volume / fadeSteps;

                        for (int i = 0; i < fadeSteps; i++) {
                            volume -= volumeStep;
                            setMusicVolume(SoundCategory.MUSIC, Math.max(0, volume));
                            Thread.sleep((long) stepTime);
                        }

                        mc.getSoundHandler().stopSound(currentMusic);

                        restoreMusicVolume();
                        isVanillaMusicFading = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void setMusicVolume(SoundCategory category, float volume) {
        Minecraft mc = Minecraft.getMinecraft();

        mc.gameSettings.setSoundLevel(category, volume);

        try {
            Field sndManagerField = ObfuscationReflectionHelper.findField(SoundHandler.class, "field_147694_f");
            sndManagerField.setAccessible(true);

            SoundManager soundManager = (SoundManager) sndManagerField.get(mc.getSoundHandler());

            soundManager.setVolume(category, volume);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void restoreMusicVolume() {
        Minecraft mc = Minecraft.getMinecraft();
        setMusicVolume(SoundCategory.MUSIC, originalMusicVolume);
    }

    @SideOnly(Side.CLIENT)
    public static void stopVanillaMusic() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            originalMusicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
            MusicTicker musicTicker = mc.getMusicTicker();
            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);
            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);

            if (currentMusic != null && !isVanillaMusicFading) {
                isVanillaMusicFading = true;
                new Thread(() -> {
                    mc.getSoundHandler().stopSound(currentMusic);
                    isVanillaMusicFading = false;
                    restoreMusicVolume();
                }).start();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

// ----------------------- HELPER METHODS -----------------------

    private static boolean isMusicBiomeCorrect(String musicfilename, String configSet) {
        if (musicfilename == null) {
            return false;
        }
        List<String> list = Arrays.asList(configSet.split(","));
        return list.contains(musicfilename);
    }

    private static String getRandomSongForBiome(String musiclist) {
        List<String> list = Arrays.asList(musiclist.split(","));
        return list.get(random.nextInt(list.size())).trim();
    }

    /**
     * Fetches a random song for a given biome tag.
     *
     * @param biomeTag The tag to look up in biomeTagMusicMap.
     * @return The file name of a randomly chosen song, or "default_music" if none is set.
     */
    public static String getRandomSongForBiomeTag(String biomeTag) {
        String songList = BiomeMusicConfig.biomeTagMusicMap.getOrDefault(biomeTag, "default_music");

        List<String> songs = Arrays.asList(songList.split(","));

        return songs.get(random.nextInt(songs.size())).trim();
    }
}
