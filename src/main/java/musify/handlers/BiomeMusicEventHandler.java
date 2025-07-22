package musify.handlers;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import musify.musicplayer.MusicPlayer;
import musify.utils.BossTargetUtils;
import musify.utils.JukeboxUtils;
import musify.utils.TargetingUtils;
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

import static musify.config.BiomeMusicConfig.*;
import static musify.handlers.HandleCombatMusic.*;
import static musify.handlers.HandleUndergroundMusic.handleUndergroundMusic;
import static musify.handlers.HandleUndergroundMusic.isUndergroundMusicPlaying;
import static musify.utils.BossTargetUtils.handleBossMusic;
import static musify.utils.BossTargetUtils.isBossMusicPlaying;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class BiomeMusicEventHandler {

    private static final Random random = new Random();

    /**
     * The original music volume setting value for fading vanilla music.
     */
    private static float originalMusicVolume;

    /**
     * The active music player instance that is currently playing music.
     */
    public static MusicPlayer activeMusic = null;
    public static MusicPlayer activeTagMusic = null;
    public static String currentMusicFile = null;
    public static boolean isVanillaMusicFading = false;

    private static EntityPlayer player;
    private static int aggroCounter = 0;
    private static int tickCounter = 0;

    private static int jukeboxTicks = 0;
    private static boolean jukeboxPause = false;

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

        if (fadeOptions.pollingRate == 0) {
            if (tickCounter % 1000 == 0) {
                Musify.LOGGER.error("POLLING RATE IS SET TO 0 IN MUSIFY CONFIG! THIS WILL BREAK THE MOD! \n (forcibly stopped the mod to prevent a crash.)");
            }
            return;
        }

        tickCounter++;

        if (jukeboxTicks > 0) {
            jukeboxTicks--;
            if (JukeboxUtils.isJukeBoxNearAndPlaying(event.player, miscOptions.jukeboxRange)) {
                jukeboxTicks = 200;
            }
            return;
        }

        if (aggroCounter > 0) {
            aggroCounter--;

            if (tickCounter % (fadeOptions.pollingRate / 4) == 0 && event.player != null && event.player.world != null) {
                int aggrocount = TargetingUtils.countMobsTargetingPlayer(event.player, combatOptions.combatRadius);
                if (aggrocount >= combatOptions.combatStopNumber) {
                    aggroCounter = 400;
                }
            }
            if (isCombatMusicPlaying()) {
                if (!isVanillaMusicFading) {
                    stopVanillaMusic();
                }
            }
        }


        if (tickCounter % fadeOptions.pollingRate == 0 && event.player != null && event.player.world != null && !isCombatMusicFading && jukeboxTicks == 0 && !isAllFading()) {

            Musify.LOGGER.debug("ENTERED HANDLER");
            // -----------------------  JUKEBOX HANDLING  -----------------------
            if (JukeboxUtils.isJukeBoxNearAndPlaying(event.player, miscOptions.jukeboxRange)) {
                Musify.LOGGER.debug("Jukebox found nearby, pausing music.");
                jukeboxTicks = 200;
                if (activeMusic != null && !activeMusic.isFading() && !activeMusic.isPaused()) {
                    Musify.LOGGER.debug("Stopping active music due to jukebox presence.");
                    activeMusic.pauseWithFadeOut(7500);

                }
                if (activeTagMusic != null && !activeTagMusic.isFading() && !activeTagMusic.isPaused()) {
                    Musify.LOGGER.debug("Stopping active tag music due to jukebox presence.");
                    activeTagMusic.pauseWithFadeOut(7500);
                }
                if (combatMusicPlayer != null && !combatMusicPlayer.isFading()) {
                    combatMusicPlayer.pauseWithFadeOut(7500);
                }
                jukeboxPause = true;
                return;
            }

            if (jukeboxPause) {
                if (!isCombatMusicPlaying()) {
                    if (activeMusic != null && !activeMusic.isFading() && activeMusic.isPaused()) {
                        activeMusic.resumeWithFadeIn(10000);
                    }
                    if (activeTagMusic != null && !activeTagMusic.isFading() && activeTagMusic.isPaused()) {
                        activeTagMusic.resumeWithFadeIn(10000);
                    }
                } else {
                    if (combatMusicPlayer != null && !combatMusicPlayer.isFading() && combatMusicPlayer.isPaused()) {
                        combatMusicPlayer.resumeWithFadeIn(10000);
                    }
                    if (activeMusic != null && !activeMusic.isFading() && activeMusic.isPaused()) {
                        activeMusic.resume();
                    }
                    if (activeTagMusic != null && !activeTagMusic.isFading() && activeTagMusic.isPaused()) {
                        activeTagMusic.resume();
                    }
                }
                jukeboxPause = false;
                return;
            }

            if (aggroCounter == 0) {

                // ----------------------- BOSS MUSIC HANDLING -----------------------
                if (bossMusicOptions.enableBossMusic) {
                    String bossMusic = BossTargetUtils.bossMusicFile(event.player);
                    if (bossMusic != null && !bossMusic.isEmpty()) {
                        handleBossMusic(bossMusic);
                        return;
                    }
                }
                if (BossTargetUtils.isBossMusicPlaying) {
                    isBossMusicPlaying = false;
                }

                // ----------------------- COMBAT MUSIC HANDLING -----------------------
                if (combatOptions.enableCombatMusic) {
                    int aggrocount = TargetingUtils.countMobsTargetingPlayer(event.player, combatOptions.combatRadius);
                    if (aggrocount >= combatOptions.combatStartNumber) {
                        aggroCounter = 400;
                        handleCombatMusic();
                        return;
                    }
                }
                if (isCombatMusicPlaying() && !isCombatMusicFading) {
                    if (getCombatMusicPlayer() != null && getCombatMusicPlayer().isPlaying()) {
                        getCombatMusicPlayer().fadeOut(7500);
                        setCombatMusicPlaying(false);
                    } else if (getCombatMusicPlayer() != null && !getCombatMusicPlayer().isPlaying()) {
                        getCombatMusicPlayer().stop();
                        setCombatMusicPlaying(false);
                        combatMusicPlayer = null;
                    }
                    if (activeMusic != null && !activeMusic.isFading()) {
                        activeMusic.fadeIn(8500);
                        return;
                    } else if (activeTagMusic != null && !activeTagMusic.isFading()) {
                        activeTagMusic.fadeIn(8500);
                        return;
                    }
                }

                // --w--------------------- UNDERGROUND MUSIC HANDLING -----------------------
                if (event.player.world != null && event.player.posY <= cpundergroundOptions.undergroundMusicYLevelStart && event.player.world.provider.getDimension() == 0 && !isUndergroundMusicPlaying) {
                    handleUndergroundMusic();
                    return;
                }
                if (isUndergroundMusicPlaying && event.player.world != null && event.player.posY <= cpundergroundOptions.undergroundMusicYLevelStop && event.player.world.provider.getDimension() == 0) {
                    return;
                } else {
                    isUndergroundMusicPlaying = false;
                }

                // // ----------------------- BIOME MUSIC HANDLING -----------------------
                BlockPos pos = event.player.getPosition();
                Biome biome = event.player.world.getBiome(pos);
                handleBiomeMusic(biome);
            }
        }
    }

    public static boolean isAllFading() {
        boolean activeMusicFading = activeMusic != null && activeMusic.isFading();
        boolean activeTagMusicFading = activeTagMusic != null && activeTagMusic.isFading();
        return activeMusicFading || activeTagMusicFading;
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
                // specific music not null, tag null
                if (activeMusic != null && !isMusicBiomeCorrect(currentMusicFile, configSet) && !activeMusic.isFading() && activeTagMusic == null) {
                    if (activeTagMusic != null) {
                        activeTagMusic.stopWithFadeOut(12500);
                        activeTagMusic = null;
                    }
                    activeMusic.stopWithFadeOut(12500);
                    activeMusic = new MusicPlayer(musicFile, false);
                    currentMusicFile = musicFile;
                    activeMusic.playWithFadeIn(15000);
                }
                //specific music null, tag not null
                else if (activeMusic == null && activeTagMusic != null && !activeTagMusic.isFading()) {
                    activeMusic = new MusicPlayer(musicFile, false);
                    currentMusicFile = musicFile;
                    activeMusic.playWithFadeIn(15000);
                    stopVanillaMusic();
                    if (activeTagMusic != null) {
                        activeTagMusic.stopWithFadeOut(12500);
                        activeTagMusic = null;
                    }
                }
                // specific music null, tag null
                else if (activeMusic == null && activeTagMusic == null) {
                    activeMusic = new MusicPlayer(musicFile, false);
                    currentMusicFile = musicFile;
                    activeMusic.playWithFadeIn(15000);
                    stopVanillaMusic();
                }
                if (activeTagMusic != null && activeTagMusic.isPlaying() && !isVanillaMusicFading) {
                    stopVanillaMusic();
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

                        Set<String> possibleSongs = new HashSet<>();
                        // picking one random song from the config list
                        for (BiomeDictionary.Type type : biomeTags) {
                            String possibleSongList = BiomeMusicConfig.biomeTagMusicMap.getOrDefault(type.getName().toLowerCase(), "default_music");
                            String[] songList = possibleSongList.split(",");
                            possibleSongs.addAll(Arrays.asList(songList));
                        }

                        if (activeTagMusic == null && activeMusic != null && !activeMusic.isFading() && !possibleSongs.contains(activeMusic.getFileName())) {
                            FadeOutActiveMusicAndPlayNew(musicFile, randomTagMusicFile);
                        }
                        else if (activeTagMusic != null && activeMusic != null && (!activeTagMusic.isPlaying() || !possibleSongs.contains(activeTagMusic.getFileName())) && !activeTagMusic.isFading() && !activeMusic.isFading()) {
                            FadeOutActiveMusicAndPlayNew(musicFile, randomTagMusicFile);
                        }
                        else if (activeMusic == null && activeTagMusic == null) {
                            activeTagMusic = new MusicPlayer(randomTagMusicFile, false);
                            currentMusicFile = musicFile;
                            activeTagMusic.playWithFadeIn(15000);
                            stopVanillaMusic();
                        }
                        else if (activeTagMusic != null && activeMusic == null && !activeTagMusic.isFading() && !possibleSongs.contains(activeTagMusic.getFileName())) {
                            activeTagMusic.stopWithFadeOut(12500);
                            activeTagMusic = new MusicPlayer(randomTagMusicFile, false);
                            currentMusicFile = musicFile;
                            activeTagMusic.playWithFadeIn(15000);
                            stopVanillaMusic();
                        }
                        if (!isVanillaMusicFading && activeTagMusic != null && !adambientMode) {
                            stopVanillaMusic();
                        }
                    } else {
                        if (activeTagMusic != null && activeTagMusic.isPlaying() && !activeTagMusic.isFading()) {
                            activeTagMusic.stopWithFadeOut(12500);
                            activeTagMusic = null;
                        }
                        if (activeMusic != null && activeMusic.isPlaying() && !activeMusic.isFading()) {
                            activeMusic.stopWithFadeOut(15000);
                            activeMusic = null;
                        }
                        if (getCombatMusicPlayer() != null && getCombatMusicPlayer().isPlaying() && !getCombatMusicPlayer().isFading()) {
                            getCombatMusicPlayer().stopWithFadeOut(15000);
                            setCombatMusicPlaying(false);
                            combatMusicPlayer = null;
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

    private static void FadeOutActiveMusicAndPlayNew(String musicFile, String randomTagMusicFile) {
        if (activeMusic != null && activeMusic.isPlaying()) {
            activeMusic.stopWithFadeOut(12500);
            activeMusic = null;
        }
        activeTagMusic = new MusicPlayer(randomTagMusicFile, false);
        activeTagMusic.playWithFadeIn(15000);
        currentMusicFile = musicFile;
        if (!isVanillaMusicFading && !adambientMode) {
            stopVanillaMusic();
        }
    }

// ----------------------- VANILLA MUSIC STUFF -----------------------

    @SideOnly(Side.CLIENT)
    public static void stopVanillaMusic() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            MusicTicker musicTicker = mc.getMusicTicker();

            // Stop current vanilla music
            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);
            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);
            if (currentMusic != null) {
                mc.getSoundHandler().stopSound(currentMusic);
            }

        } catch (Exception e) {
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
