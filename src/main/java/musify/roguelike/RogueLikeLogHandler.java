package musify.roguelike;

import com.github.fnar.roguelike.events.StructurePartsGenerationEvent;
import musify.Musify;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class RogueLikeLogHandler {
    private static final String LOG_FILE_NAME = "roguelike_dungeons.csv";

    @SubscribeEvent
    public static void onStructurePartsGeneration(StructurePartsGenerationEvent event) {
        String structureId = event.getIdentifier().toString();
        int dim = event.getDimension();
        String coords = event.getCoords().toString();
        String cleanedCoords = coords.replace("[", "").replace("]", "").replace(" ", "");
        logToWorldFile(structureId, dim, cleanedCoords);
    }

    private static void logToWorldFile(String structureId, int dimension, String boundingBox) {
        try {
            File worldDir;

            if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
                WorldServer world = net.minecraftforge.fml.common.FMLCommonHandler.instance()
                        .getMinecraftServerInstance()
                        .getWorld(dimension);

                if (world != null) {
                    worldDir = world.getSaveHandler().getWorldDirectory();
                } else {
                    worldDir = net.minecraftforge.fml.common.FMLCommonHandler.instance()
                            .getMinecraftServerInstance()
                            .getActiveAnvilConverter()
                            .getFile(net.minecraftforge.fml.common.FMLCommonHandler.instance()
                                    .getMinecraftServerInstance().getFolderName(), "");
                }
            } else {
                worldDir = new File(".", "saves/" + net.minecraft.client.Minecraft.getMinecraft().getIntegratedServer().getFolderName());
                Musify.LOGGER.warn("Using fallback world directory: {}", worldDir.getAbsolutePath());
            }

            File logFile = new File(worldDir, LOG_FILE_NAME);

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(String.format("%s,%d,%s", structureId, dimension, boundingBox));
            }
        } catch (IOException e) {
            Musify.LOGGER.error("Failed to write to structure log file in world directory", e);
        }
    }
}