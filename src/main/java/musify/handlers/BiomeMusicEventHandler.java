package musify.handlers;

import musify.Musify;
import musify.config.BiomeMusicConfig;
import musify.musicplayer.MusicPlayer;
import musify.utils.BossTargetUtils;
import musify.utils.DungeonUtils;
import musify.utils.JukeboxUtils;
import musify.utils.TargetingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.*;

import static musify.config.BiomeMusicConfig.*;
import static musify.handlers.DungeonHandler.*;
import static musify.handlers.HandleCombatMusic.*;
import static musify.handlers.HandleUndergroundMusic.handleUndergroundMusic;
import static musify.handlers.HandleUndergroundMusic.isUndergroundMusicPlaying;
import static musify.handlers.RecurrentMusicHandler.handleRecurrentMusic;
import static musify.roguelike.RoguelikeMusicHandler.handleRoguelikeDungeonsMusic;
import static musify.utils.BossTargetUtils.handleBossMusic;
import static musify.utils.BossTargetUtils.isBossMusicPlaying;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class BiomeMusicEventHandler {

    private static final Random random = new Random();

    /**
     * The active music player instance that is currently playing music.
     */
    public static MusicPlayer activeMusic = null;
    public static MusicPlayer activeTagMusic = null;
    public static String currentMusicFile = "";
    public static boolean isVanillaMusicFading = false;

    private static EntityPlayer player;
    private static int aggroCounter = 0;
    private static int tickCounter = 0;
    private static int dungeonCount = 0;
    public static int recurrentCount = 0;
    public static int doomlikeCount = 0;
    public static int roguelikeCount = 0;

    private static int jukeboxTicks = 0;
    private static boolean jukeboxPause = false;

    public static boolean isDungeonMusicPlaying = false;

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

        if (lfadeOptions.pollingRate == 0) {
            if (tickCounter % 1000 == 0) {
                Musify.LOGGER.error("POLLING RATE IS SET TO 0 IN MUSIFY CONFIG! THIS WILL BREAK THE MOD! \n (forcibly stopped the mod to prevent a crash.)");
            }
            return;
        }

        tickCounter++;

        if (Loader.isModLoaded("dldungeonsjbg")) {
            if (tickCounter % (7500) == 0) {
                if (player.world != null) {
                    if (!event.player.world.isRemote) {
                        removeDuplicatesFromDungeonFile(event.player.world);
                    }
                }
            }
        }

        if (jukeboxTicks > 0) {
            jukeboxTicks--;
            if (JukeboxUtils.isJukeBoxNearAndPlaying(event.player, nmiscOptions.jukeboxRange)) {
                jukeboxTicks = 200;
            }
            return;
        }

        if (tickCounter % lfadeOptions.pollingRate == 0 && event.player != null && event.player.world != null && !isCombatMusicFading && jukeboxTicks == 0 && !isAllFading()) {

            // -----------------------  JUKEBOX HANDLING  -----------------------
            if (JukeboxUtils.isJukeBoxNearAndPlaying(event.player, nmiscOptions.jukeboxRange)) {
                jukeboxTicks = 200;
                if (activeMusic != null && !activeMusic.isFading() && !activeMusic.isPaused()) {
                    activeMusic.pauseWithFadeOut(lfadeOptions.customMusicFadeOutTime);

                }
                if (activeTagMusic != null && !activeTagMusic.isFading() && !activeTagMusic.isPaused()) {
                    activeTagMusic.pauseWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                }
                if (combatMusicPlayer != null && !combatMusicPlayer.isFading()) {
                    combatMusicPlayer.pauseWithFadeOut(lfadeOptions.combatMusicFadeInTime);
                }
                jukeboxPause = true;
                return;
            }

            if (jukeboxPause) {
                if (!isCombatMusicPlaying()) {
                    if (activeMusic != null && !activeMusic.isFading() && activeMusic.isPaused()) {
                        activeMusic.resumeWithFadeIn(lfadeOptions.customMusicFadeInTime);
                    }
                    if (activeTagMusic != null && !activeTagMusic.isFading() && activeTagMusic.isPaused()) {
                        activeTagMusic.resumeWithFadeIn(lfadeOptions.customMusicFadeInTime);
                    }
                } else {
                    if (combatMusicPlayer != null && !combatMusicPlayer.isFading() && combatMusicPlayer.isPaused()) {
                        combatMusicPlayer.resumeWithFadeIn(lfadeOptions.customMusicFadeInTime);
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
        }

        if (doomlikeCount > 0 && activeMusic != null && !activeMusic.isPaused()) {
            doomlikeCount--;
            if (doomlikeCount % 50 == 0) {
                handleDoomlikeDungeonsMusic(player, edoomlikeDungeonsOptions.DoomlikeDistance, true);
            }
            stopVanillaMusic();
            return;
        }

        // TODO: roguelike dungeons stop handling. like the two functions below.

        if (recurrentCount > 0 && activeMusic != null && !activeMusic.isPaused()) {
            recurrentCount--;
            if (recurrentCount % 50 == 0) {
                handleRecurrentMusic(true);
            }
            stopVanillaMusic();
            return;
        }

        if (dungeonCount > 0 && activeMusic != null && !activeMusic.isPaused()) {
            dungeonCount--;
            if (dungeonCount % 50 == 0) {
                if (DungeonUtils.getDungeonMusic(event.player) != null && !DungeonUtils.getDungeonMusic(event.player).isEmpty()) {
                    dungeonCount = 200;
                }
            }
            stopVanillaMusic();
            return;
        }

        if (aggroCounter > 0) {
            aggroCounter--;

            if (tickCounter % (lfadeOptions.pollingRate / 4) == 0 && event.player != null && event.player.world != null) {
                int aggrocount = TargetingUtils.countMobsTargetingPlayer(event.player, hcombatOptions.combatRadius);
                if (aggrocount >= hcombatOptions.combatStopNumber) {
                    aggroCounter = 400;
                }
            }
            if (isCombatMusicPlaying()) {
                if (!isVanillaMusicFading) {
                    if (!bdambientMode) {
                        stopVanillaMusic();
                    }
                }
            }
        }


        if (tickCounter % lfadeOptions.pollingRate == 0 && event.player != null && event.player.world != null && !isCombatMusicFading && jukeboxTicks == 0 && !isAllFading()) {

            // ----------------------- BOSS MUSIC HANDLING -----------------------
            if (dbossMusicOptions.enableBossMusic) {
                String bossMusic = BossTargetUtils.bossMusicFile(event.player);
                if (bossMusic != null && !bossMusic.isEmpty()) {
                    handleBossMusic(bossMusic);
                    return;
                }
            }
            if (BossTargetUtils.isBossMusicPlaying) {
                isBossMusicPlaying = false;
            }

            // ----------------------- ROGUELIKE DUNGEONS HANDLING -----------------------

            if (ecroguelikeDungeonsOptions.enableRoguelikeDungeonsMusic && Loader.isModLoaded("roguelike")) {
                if (tickCounter % (lfadeOptions.pollingRate * 2) == 0) {
                    handleRoguelikeDungeonsMusic(false);
                }
            }


            // ----------------------- DOOMLIKE DUNGEONS HANDLING -----------------------

            if (edoomlikeDungeonsOptions.enableDoomlikeDungeonsMusic && Loader.isModLoaded("dldungeonsjbg")) {
                if (tickCounter % (lfadeOptions.pollingRate * 2) == 0) {
                    handleDoomlikeDungeonsMusic(event.player, edoomlikeDungeonsOptions.DoomlikeDistance);
                }
            }


            // ----------------------- RECURRENT COMPLEX HANDLING -----------------------

            if (doomlikeCount == 0) {
                if ((tickCounter % (lfadeOptions.pollingRate * 2) == 0)) {
                    if (frecurrentComplexOptions.enableRecurrentComplexMusic && Loader.isModLoaded("reccomplex")) {
                        handleRecurrentMusic(false);
                        if (isDungeonMusicPlaying) {
                            isDungeonMusicPlaying = false;
                            return;
                        }
                    }
                }

                // ----------------------- DUNGEON MUSIC HANDLING -----------------------

                if (recurrentCount == 0) {
                    if (gdungeonDefinitionOptions.enableDungeonMusic) {
                        String dungeonMusic = DungeonUtils.getDungeonMusic(event.player);
                        if (dungeonMusic != null && !dungeonMusic.isEmpty()) {
                            if (activeMusic != null && !activeMusic.isFading() && !activeMusic.getFileName().equals(dungeonMusic)) {
                                activeMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                                activeMusic = new MusicPlayer(dungeonMusic, true);
                                currentMusicFile = dungeonMusic;
                                activeMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                            } else if (activeTagMusic != null && !activeTagMusic.isFading() && !activeTagMusic.getFileName().equals(dungeonMusic)) {
                                activeTagMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                                activeTagMusic = new MusicPlayer(dungeonMusic, true);
                                currentMusicFile = dungeonMusic;
                                activeTagMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                            } else if (activeTagMusic == null && activeMusic == null) {
                                activeMusic = new MusicPlayer(dungeonMusic, true);
                                currentMusicFile = dungeonMusic;
                                activeMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                            }
                            if (!bdambientMode) {
                                stopVanillaMusic();
                            }
                            dungeonCount = 200;
                            return;
                        }
                    }

                    if (aggroCounter == 0) {
                        // ----------------------- COMBAT MUSIC HANDLING -----------------------
                        if (hcombatOptions.enableCombatMusic) {
                            int aggrocount = TargetingUtils.countMobsTargetingPlayer(event.player, hcombatOptions.combatRadius);
                            if (aggrocount >= hcombatOptions.combatStartNumber) {
                                aggroCounter = 400;
                                handleCombatMusic();
                                return;
                            }
                        }
                        if (isCombatMusicPlaying() && !isCombatMusicFading) {
                            if (getCombatMusicPlayer() != null && getCombatMusicPlayer().isPlaying()) {
                                getCombatMusicPlayer().fadeOut(lfadeOptions.combatMusicFadeInTime);
                                setCombatMusicPlaying(false);
                            } else if (getCombatMusicPlayer() != null && !getCombatMusicPlayer().isPlaying()) {
                                getCombatMusicPlayer().stop();
                                setCombatMusicPlaying(false);
                                combatMusicPlayer = null;
                            }
                            if (activeMusic != null && !activeMusic.isFading()) {
                                activeMusic.fadeIn(lfadeOptions.customMusicFadeInTime);
                                return;
                            } else if (activeTagMusic != null && !activeTagMusic.isFading()) {
                                activeTagMusic.fadeIn(lfadeOptions.customMusicFadeInTime);
                                return;
                            }
                        }

                        // ----------------------- UNDERGROUND MUSIC HANDLING -----------------------
                        if (iundergroundOptions.enableUndergroundMusic) {
                            if (event.player.world != null && event.player.posY <= iundergroundOptions.undergroundMusicYLevelStart && event.player.world.provider.getDimension() == 0 && !isUndergroundMusicPlaying) {
                                handleUndergroundMusic();
                                return;
                            }
                        }
                        if (isUndergroundMusicPlaying && event.player.world != null && event.player.posY <= iundergroundOptions.undergroundMusicYLevelStop && event.player.world.provider.getDimension() == 0) {
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
        }
        if (tickCounter > 1000000) {
            tickCounter = 0;
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

            String configSet = BiomeMusicConfig.jbiomeMusicMap.get(biomeRegistryName.toString());
            String musicFile = null;

            if (configSet != null) {
                musicFile = getRandomSongForBiome(configSet);
            }

            if (musicFile != null && !configSet.equals("default_music") && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
                if (activeMusic != null && !isMusicBiomeCorrect(currentMusicFile, configSet) && !activeMusic.isFading() && activeTagMusic == null) {
                    if (activeTagMusic != null) {
                        activeTagMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                        activeTagMusic = null;
                    }
                    activeMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                    activeMusic = new MusicPlayer(musicFile, false);
                    currentMusicFile = musicFile;
                    activeMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                }
                else if (activeMusic == null && activeTagMusic != null && !activeTagMusic.isFading()) {
                    activeMusic = new MusicPlayer(musicFile, false);
                    currentMusicFile = musicFile;
                    activeMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                    if (!bdambientMode) {
                        stopVanillaMusic();
                    }
                    if (activeTagMusic != null) {
                        activeTagMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                        activeTagMusic = null;
                    }
                }
                else if (activeMusic == null && activeTagMusic == null) {
                    activeMusic = new MusicPlayer(musicFile, false);
                    currentMusicFile = musicFile;
                    activeMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                    if (!bdambientMode) {
                        stopVanillaMusic();
                    }
                }
                if (activeTagMusic != null && activeTagMusic.isPlaying() && !isVanillaMusicFading) {
                    if (!bdambientMode) {
                        stopVanillaMusic();
                    }
                }
            } else {
                Set<BiomeDictionary.Type> biomeTags = BiomeDictionary.getTypes(biome);
                if (!biomeTags.isEmpty() && biome != Biomes.RIVER) {
                    String randomTagMusicFile = biomeTags.stream()
                            .map(type -> getRandomSongForBiomeTag(type.getName().toLowerCase()))
                            .filter(song -> !song.equals("default_music"))
                            .findFirst()
                            .orElse("default_music");

                    if (!randomTagMusicFile.equals("default_music")) {

                        Set<String> possibleSongs = new HashSet<>();
                        for (BiomeDictionary.Type type : biomeTags) {
                            String possibleSongList = BiomeMusicConfig.kbiomeTagMusicMap.getOrDefault(type.getName().toLowerCase(), "default_music");
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
                            activeTagMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                            if (!bdambientMode) {
                                stopVanillaMusic();
                            }
                        }
                        else if (activeTagMusic != null && activeMusic == null && !activeTagMusic.isFading() && !possibleSongs.contains(activeTagMusic.getFileName())) {
                            activeTagMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                            activeTagMusic = new MusicPlayer(randomTagMusicFile, false);
                            currentMusicFile = musicFile;
                            activeTagMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
                            if (!bdambientMode) {
                                stopVanillaMusic();
                            }
                        }
                        if (!isVanillaMusicFading && activeTagMusic != null && !bdambientMode) {
                            if (!bdambientMode) {
                                stopVanillaMusic();
                            }
                        }
                    } else {
                        if (activeTagMusic != null && activeTagMusic.isPlaying() && !activeTagMusic.isFading()) {
                            activeTagMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                            activeTagMusic = null;
                        }
                        if (activeMusic != null && activeMusic.isPlaying() && !activeMusic.isFading()) {
                            activeMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                            activeMusic = null;
                        }
                        if (getCombatMusicPlayer() != null && getCombatMusicPlayer().isPlaying() && !getCombatMusicPlayer().isFading()) {
                            getCombatMusicPlayer().stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
                            setCombatMusicPlaying(false);
                            combatMusicPlayer = null;
                        }
                    }
                    if (activeTagMusic != null && activeTagMusic.isPlaying() && !isVanillaMusicFading) {
                        if (!bdambientMode) {
                            stopVanillaMusic();
                        }
                    }
                }
            }
        } else if (activeMusic != null) {
            activeMusic.stopWithFadeOut(lfadeOptions.customMusicFadeOutTime);
            activeMusic = null;
        }
    }

    private static void FadeOutActiveMusicAndPlayNew(String musicFile, String randomTagMusicFile) {
        if (activeMusic != null && activeMusic.isPlaying()) {
            activeMusic.stopWithFadeOut(12500);
            activeMusic = null;
        }
        activeTagMusic = new MusicPlayer(randomTagMusicFile, false);
        activeTagMusic.playWithFadeIn(lfadeOptions.customMusicFadeInTime);
        currentMusicFile = musicFile;
        if (!isVanillaMusicFading) {
            if (!bdambientMode) {
                stopVanillaMusic();
            }
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
        String songList = BiomeMusicConfig.kbiomeTagMusicMap.getOrDefault(biomeTag, "default_music");

        List<String> songs = Arrays.asList(songList.split(","));

        return songs.get(random.nextInt(songs.size())).trim();
    }
}
