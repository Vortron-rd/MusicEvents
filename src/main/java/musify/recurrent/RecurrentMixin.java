package musify.recurrent;

import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.reccomplex.operation.OperationGenerateStructure;
import ivorius.reccomplex.world.gen.feature.StructureGenerator;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import ivorius.reccomplex.world.gen.feature.structure.generic.GenericStructure;
import musify.Musify;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(StructureGenerator.class)
public class RecurrentMixin {

    @Inject(method = "generate", at = @At(value = "RETURN"), remap = false)
    private void onGenerateStructure(CallbackInfoReturnable<StructureGenerator.GenerationResult> cir) {
        try {
            StructureGenerator<?> generator = (StructureGenerator<?>) (Object) this;
            StructureGenerator.GenerationResult result = cir.getReturnValue();

            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("=== STRUCTURE GENERATED ===\n");

            // Get core structure info
            String structureID = getFieldValue(generator, "structureID");
            logBuilder.append("Structure ID: ").append(structureID != null ? structureID : "unknown").append("\n");

            // Get world info
            WorldServer world = getFieldValue(generator, "world");
            if (world != null) {
                logBuilder.append("Dimension: ").append(world.provider.getDimensionType().getName())
                        .append(" (").append(world.provider.getDimension()).append(")\n");
            }

            // Get position info
            StructureBoundingBox boundingBox = getFieldValue(generator, "boundingBox");
            if (boundingBox != null) {
                logBuilder.append("Bounding Box: Min [").append(boundingBox.minX).append(", ")
                        .append(boundingBox.minY).append(", ")
                        .append(boundingBox.minZ).append("] Max [")
                        .append(boundingBox.maxX).append(", ")
                        .append(boundingBox.maxY).append(", ")
                        .append(boundingBox.maxZ).append("]\n");
            }

            BlockPos lowerCoord = getFieldValue(generator, "lowerCoord");
            if (lowerCoord != null) {
                logBuilder.append("Lower Coord: [").append(lowerCoord.getX()).append(", ")
                        .append(lowerCoord.getY()).append(", ")
                        .append(lowerCoord.getZ()).append("]\n");
            }

            // Get transformation info
            AxisAlignedTransform2D transform = getFieldValue(generator, "transform");
            if (transform != null) {
                logBuilder.append("Transform: Rotation=").append(transform.getRotation())
                        .append(", Mirror=").append(transform.isMirrorX()).append("\n");
            }

            // Get generation info
            Object generationType = getFieldValue(generator, "generationType");
            if (generationType != null) {
                logBuilder.append("Generation Type: ").append(generationType).append("\n");
            }

            int generationLayer = getIntField(generator, "generationLayer");
            logBuilder.append("Generation Layer: ").append(generationLayer).append("\n");

            // Get structure-specific info
            Structure<?> structure = getFieldValue(generator, "structure");
            if (structure != null) {
                logBuilder.append("Structure Class: ").append(structure.getClass().getSimpleName()).append("\n");

                if (structure instanceof GenericStructure) {
                    GenericStructure genericStructure = (GenericStructure) structure;
                    logBuilder.append("Preset Title: ").append(genericStructure.transformer.getData().presetTitle()).append("\n");
                }
            }

            // Get generation flags
            boolean allowOverlaps = getBooleanField(generator, "allowOverlaps");
            logBuilder.append("Allow Overlaps: ").append(allowOverlaps).append("\n");

            boolean memorize = getBooleanField(generator, "memorize");
            logBuilder.append("Memorize: ").append(memorize).append("\n");

            boolean partially = getBooleanField(generator, "partially");
            logBuilder.append("Partially Generated: ").append(partially).append("\n");

            Long seed = getFieldValue(generator, "seed");
            if (seed != null) {
                logBuilder.append("Seed: ").append(seed).append("\n");
            }

            // Get generation result info
            logBuilder.append("Result: ").append(result.getClass().getSimpleName()).append("\n");
            Musify.LOGGER.info(logBuilder.toString());
        } catch (Exception e) {
            Musify.LOGGER.error("Error logging structure generation details", e);
        }
    }

    // Helper method to get field values using reflection
    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return (T) field.get(obj);
            }
        } catch (Exception e) {
            Musify.LOGGER.debug("Could not access field {}", fieldName);
        }
        return null;
    }

    // Helper method to get int field values
    private int getIntField(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.getInt(obj);
            }
        } catch (Exception e) {
            Musify.LOGGER.debug("Could not access int field {}", fieldName);
        }
        return 0;
    }

    // Helper method to get boolean field values
    private boolean getBooleanField(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.getBoolean(obj);
            }
        } catch (Exception e) {
            Musify.LOGGER.debug("Could not access boolean field {}", fieldName);
        }
        return false;
    }

    // Helper method to find a field in a class or its superclasses
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