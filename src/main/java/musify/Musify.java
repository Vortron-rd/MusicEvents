package musify;

import musify.commands.getMusicCommand;
import musify.config.BiomeMusicConfig;
import musify.doomlike.DLDEventHandler;
import musify.handlers.PauseEventHandler;
import musify.network.NetworkManager;
import musify.proxy.CommonProxy;
import musify.handlers.RecurrentEventHandler;
import musify.roguelike.RogueLikeLogHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Musify.MODID, version = Musify.VERSION, name = Musify.NAME, clientSideOnly = true)
public class Musify {
    public static final String MODID = "musify";
    public static final String VERSION = "Beta 1.2.3";
    public static final String NAME = "Musify!";
    public static final Logger LOGGER = LogManager.getLogger();
	
    @SidedProxy(clientSide = "musify.proxy.ClientProxy", serverSide = "musify.proxy.CommonProxy")
    public static CommonProxy PROXY;
	
	@Instance(MODID)
	public static Musify instance;

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
        LOGGER.info("====================================MUSIFY====================================");
        if (Loader.isModLoaded("reccomplex")) {
            MinecraftForge.EVENT_BUS.register(RecurrentEventHandler.class);
            LOGGER.info("Recurrent Complex detected, Loading Recurrent Complex integration.");
        }
        if (Loader.isModLoaded("roguelike")) {
            try {
                Class.forName("com.github.fnar.roguelike.events.StructurePartsGenerationEvent");
                MinecraftForge.EVENT_BUS.register(RogueLikeLogHandler.class);
                LOGGER.info("RogueLike Dungeons detected, Loading RogueLike Dungeons integration.");
            } catch (ClassNotFoundException e) {

                LOGGER.error("You are using a version of RogueLike Dungeons that is not compatible with Musify.");
                LOGGER.error("Please update RogueLike Dungeons to the latest version.");
            }
        }

        if (Loader.isModLoaded("dldungeonsjbg")) {
            try {
                Class<?> clazz = Class.forName("jaredbgreat.dldungeons.api.DLDEvent$BeforeBuild");
                clazz.getMethod("getDungeon");
                MinecraftForge.TERRAIN_GEN_BUS.register(DLDEventHandler.class);
                LOGGER.info("Doomlike Dungeons detected, Loading Doomlike Dungeons integration.");
            }
            catch (ClassNotFoundException e) {
                LOGGER.error("Doomlike Dungeons mod does not contain event class. Doomlike Dungeons integration will be disabled.");
                LOGGER.error("How does that even happen? Try updating Doomlike Dungeons to the latest version.");
            }
            catch (NoSuchMethodException e) {
                LOGGER.error("You are using a version of Doomlike Dungeons that is not compatible with Musify.");
                LOGGER.error("Doomlike Dungeons integration will be disabled. Please update Doomlike Dungeons to the latest version.");
            }
        }
        LOGGER.info("==============================================================================");
        MinecraftForge.EVENT_BUS.register(new PauseEventHandler());
        NetworkManager.registerPackets();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        BiomeMusicConfig.updateBiomeList();
        BiomeMusicConfig.updateBiomeTagList();
        BiomeMusicConfig.updateMusicList();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new getMusicCommand());
    }
}