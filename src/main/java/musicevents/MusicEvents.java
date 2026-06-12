package musicevents;

import musicevents.commands.getBiomeTagsCommand;
import musicevents.commands.getMusicCommand;
import musicevents.config.MusicEventsConfig;
import musicevents.handlers.PauseEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = MusicEvents.MODID, version = MusicEvents.VERSION, name = MusicEvents.NAME, clientSideOnly = true)
public class MusicEvents {
    public static final String MODID = "musicevents";
    public static final String VERSION = "1.0.0";
    public static final String NAME = "MusicEvents";
    public static final Logger LOGGER = LogManager.getLogger();

	@Instance(MODID)
	public static MusicEvents instance;

    public static File musicFolder;
	
	@Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        File minecraftDir = event.getModConfigurationDirectory().getParentFile();
        musicFolder = new File(minecraftDir, "music");

        if (!musicFolder.exists()) {
            musicFolder.mkdirs();
        }

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Loading " + NAME + "...");
        MinecraftForge.EVENT_BUS.register(new PauseEventHandler());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        MusicEventsConfig.updateBiomeList();
        MusicEventsConfig.updateBiomeTagList();
        MusicEventsConfig.updateMusicList();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new getMusicCommand());
        event.registerServerCommand(new getBiomeTagsCommand());
    }
}