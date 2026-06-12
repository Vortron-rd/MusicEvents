package musicevents.handlers;

import musicevents.config.MusicEventsConfig;
import net.minecraft.world.biome.Biome;

import java.util.Objects;

public class BiomeMusicHandler {
    public static String getBiomePlaylist(Biome biome) {
        return MusicEventsConfig.biomeMusicMap.getOrDefault(Objects.requireNonNull(biome.getRegistryName()).toString(),"");
    }
}