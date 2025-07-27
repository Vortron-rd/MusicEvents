package musify.recurrent;

import ivorius.reccomplex.world.gen.feature.StructureGenerator;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import ivorius.reccomplex.world.gen.feature.structure.generic.GenericStructure;
import musify.Musify;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Mixin(StructureGenerator.class)
public class RecurrentStructureLoggerMixin {

    private static final String LOG_FILE_NAME = "recurrent_structures.csv";

    @Inject(method = "generate", at = @At(value = "RETURN"), remap = false)
    private void logImportantStructures(CallbackInfoReturnable<StructureGenerator.GenerationResult> cir) {
        try {
            StructureGenerator<?> generator = (StructureGenerator<?>) (Object) this;
            StructureGenerator.GenerationResult result = cir.getReturnValue();

            // Filter 1: Must be a successful generation
            if (!(result instanceof StructureGenerator.GenerationResult.Success)) {
                return;
            }

            // Get structure ID
            String structureID = getFieldValue(generator, "structureID");

            // Filter 2: Structure ID must not be null or "unknown"
            if (structureID == null || "unknown".equals(structureID)) {
                return;
            }

            // Get world info
            WorldServer world = getFieldValue(generator, "world");
            if (world == null) {
                return;
            }

            Structure<?> structure = getFieldValue(generator, "structure");
            if (structure instanceof GenericStructure) {
                GenericStructure genericStructure = (GenericStructure) structure;
                Optional<?> presetTitle = null;

                try {
                    presetTitle = (Optional<?>) genericStructure.transformer.getData().presetTitle();

                    // Filter 3: Exclude trees
                    if (presetTitle != null && presetTitle.isPresent() && "Tree".equals(presetTitle.get().toString())) {
                        return;
                    }
                } catch (Exception ignored) {
                }
            }

            // Get additional information
            int dimension = world.provider.getDimension();

            StructureBoundingBox boundingBox = getFieldValue(generator, "boundingBox");
            String bbString = boundingBox != null ?
                    String.format("%d,%d,%d:%d,%d,%d",
                            boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                            boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ) :
                    "unknown";

            // Log to file in the world directory
            logToWorldFile(world, structureID, dimension, bbString);

        } catch (Exception e) {
            Musify.LOGGER.error("Error logging structure to file", e);
        }
    }

    private void logToWorldFile(WorldServer world, String structureId, int dimension, String boundingBox) {
        try {
            File worldDir = world.getSaveHandler().getWorldDirectory();
            File logFile = new File(worldDir, LOG_FILE_NAME);

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {

                writer.println(String.format("%s,%d,%s", structureId, dimension, boundingBox));
            }
        } catch (IOException e) {
            Musify.LOGGER.error("Failed to write to structure log file in world directory", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return (T) field.get(obj);
            }
        } catch (Exception e) {
            // Silent failure
        }
        return null;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return findField(superClass, fieldName);
            }
        }
        return null;
    }
}