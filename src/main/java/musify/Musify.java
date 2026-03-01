package musify;

import musify.commands.getMusicCommand;
import musify.config.BiomeMusicConfig;
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
    public static final String VERSION = "Beta 1.2.2";
    public static final String NAME = "Musify!";
    public static final Logger LOGGER = LogManager.getLogger();

    //TODO: add roguelike music support
    /*
    TODO: 1. Add method to check if player is inside roguelike structure. DONE
          2. Add networking for roguelike structures. Needs config list getter from client. DONE
          3. Add method to get music for roguelike structure. DONE


    TODO:        STRUCTURE:
                - Check if player is inside structure, by sending a handle request to the server.
                - Server checks if player is inside structure, and if so, sends back the music file to play.
                - Client receives music file.
     */
	
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
        if (Loader.isModLoaded("reccomplex")) {
            MinecraftForge.EVENT_BUS.register(RecurrentEventHandler.class);
        }
        if (Loader.isModLoaded("roguelike")) {
            MinecraftForge.EVENT_BUS.register(RogueLikeLogHandler.class);
        }
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