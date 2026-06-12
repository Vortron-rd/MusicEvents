package musicevents.handlers;

import musicevents.MusicEvents;
import musicevents.config.MusicEventsConfig;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.*;

public class BiomeTagMusicHandler {
    public static String getBiomeTagPlaylist(Biome biome) {
        String x;
        Set<BiomeDictionary.Type> biomeTags = new HashSet<>(BiomeDictionary.getTypes(biome));
        if(biomeTags.isEmpty()) return "";
        while(Objects.equals(x = MusicEventsConfig.biomeTagMusicMap.getOrDefault(biomeTags.stream().findFirst().get().toString().toLowerCase(), ""), "")) {
            MusicEvents.LOGGER.debug("No songs found for : {}", biomeTags.stream().findFirst().toString().toLowerCase());
            biomeTags.remove(biomeTags.stream().findFirst().get());
            if(biomeTags.isEmpty()) return "";
        }
        return x;
    }
}
