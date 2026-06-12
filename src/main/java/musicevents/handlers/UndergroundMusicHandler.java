package musicevents.handlers;

import java.util.Objects;

import static musicevents.config.MusicEventsConfig.undergroundOptions;
import static musicevents.handlers.EventHandler.MusicType;

import net.minecraft.entity.player.EntityPlayer;

public class UndergroundMusicHandler {

    public static boolean UndergroundMusicSuitable(EntityPlayer player) {
        if(!undergroundOptions.enableUndergroundMusic) return false;
        if(Objects.equals(MusicType, "underground"))
        return player.world != null && player.posY <= undergroundOptions.undergroundMusicYLevelStop && player.world.provider.getDimension() == 0;
        else return    player.world != null && player.posY <= undergroundOptions.undergroundMusicYLevelStart && player.world.provider.getDimension() == 0;
    }


}
