package musify.handlers;

import ivorius.reccomplex.events.StructureGenerationEventLite;
import musify.Musify;
import musify.config.BiomeMusicConfig;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class RecurrentEventHandler {
    private static final String LOG_FILE_NAME = "recurrent_structures.csv";

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onStructureGenerationLitePost(StructureGenerationEventLite.Post event) {
        String structureName = event.getStructureName();
        StructureBoundingBox bb = event.getBoundingBox();
        int dim = event.getWorld().provider.getDimension();

        int width = bb.maxX - bb.minX + 1;
        int height = bb.maxY - bb.minY + 1;
        int depth = bb.maxZ - bb.minZ + 1;

        int volume = width * height * depth;

        int minVolume = BiomeMusicConfig.recurrentComplexOptions.recurrentMinimalStructureVolume;

        if (volume < minVolume) {
            return;
        }

        int x1 = bb.minX;
        int y1 = bb.minY;
        int z1 = bb.minZ;
        int x2 = bb.maxX;
        int y2 = bb.maxY;
        int z2 = bb.maxZ;
        String boundingBoxString = String.format("%d,%d,%d:%d,%d,%d", x1, y1, z1, x2, y2, z2);
        logToWorldFile(structureName, dim, boundingBoxString);
    }

    private static void logToWorldFile(String structureId, int dimension, String boundingBox) {
        try {
            File worldDir;

            // Try to get world directory from server
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
                // Fallback for integrated server
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
