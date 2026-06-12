package musicevents.handlers;

import musicevents.MusicEvents;
import musicevents.config.MusicEventsConfig;
import musicevents.musicplayer.MusicPlayer;
import musicevents.utils.JukeboxUtils;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;


import static musicevents.config.MusicEventsConfig.fadeOptions;
import static musicevents.config.MusicEventsConfig.miscOptions;
import static musicevents.handlers.BiomeMusicHandler.getBiomePlaylist;
import static musicevents.handlers.DisableVanillaMusic.stopVanillaMusic;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class EventHandler {

    public static MusicPlayer activeMusic = new MusicPlayer(null);
    private static int tickCounter = 0;
    public static String MusicType = null;

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        String tempPlaylist;
        tickCounter++;
        if (tickCounter == fadeOptions.pollingRate) {
            if(activeMusic == null) activeMusic = new MusicPlayer(null);
            tickCounter = 0;
            /* Stop for jukeboxes */
            if (JukeboxUtils.isJukeBoxNearAndPlaying(event.player, miscOptions.jukeboxRange)) {
                if (activeMusic != null && activeMusic.isPlaying()) {
                    activeMusic.stopWithFadeOut(fadeOptions.customMusicFadeOutTime);
                }
                return;
            }

            /* decide which music list to pull from */
            if (event.player.world != null) {
                Biome biome = event.player.world.getBiome(event.player.getPosition());
                if (UndergroundMusicHandler.UndergroundMusicSuitable(event.player)) {
                    tempPlaylist = MusicEventsConfig.undergroundOptions.CavernMusic;
                    MusicType = "underground";
                }
                else if(!Objects.equals(tempPlaylist = getBiomePlaylist(biome), "")) {
                    MusicType = "biome";
                }
                else if (!Objects.equals(tempPlaylist = BiomeTagMusicHandler.getBiomeTagPlaylist(biome), "")) {
                    MusicType = "biomeTag";
                }
                else {
                    activeMusic.playlist = null;
                    activeMusic.stopWithFadeOut(fadeOptions.customMusicFadeOutTime);
                    MusicType = "none";
                    MusicEvents.LOGGER.debug("Couldn't match a music condition.");
                    return;
                }

                if(!Objects.equals(tempPlaylist, activeMusic.playlist)) {
                    MusicEvents.LOGGER.debug("Switching playlist from : {}\n to : {}", activeMusic.playlist, tempPlaylist);
                    activeMusic.playlist = tempPlaylist;
                    if(activeMusic.playlist == null) activeMusic.stopWithFadeOut(fadeOptions.customMusicFadeOutTime);
                    if(activeMusic.isPlaying() && !activeMusic.isFading()) {
                        activeMusic.stopWithFadeOut(fadeOptions.customMusicFadeOutTime);
                        activeMusic.setMusicFileFromPlaylist();
                        activeMusic.playWithFadeIn(fadeOptions.customMusicFadeInTime);
                        stopVanillaMusic();
                    }
                }
                if(!activeMusic.isPlaying() && !Objects.equals(MusicType, "none")) {
                    if(activeMusic.playlist != null) activeMusic.setMusicFileFromPlaylist();
                    else if(activeMusic.fileName == null) return;
                    activeMusic.playWithFadeIn(fadeOptions.customMusicFadeInTime);
                    stopVanillaMusic();
                    MusicEvents.LOGGER.debug("Starting MusicPlayer...");
                }
            }
        }
    }
}

