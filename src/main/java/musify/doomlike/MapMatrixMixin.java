package musify.doomlike;

import jaredbgreat.dldungeons.planner.Dungeon;
import jaredbgreat.dldungeons.planner.Node;
import jaredbgreat.dldungeons.planner.mapping.MapMatrix;
import jaredbgreat.dldungeons.rooms.Room;
import jaredbgreat.dldungeons.themes.Theme;
import musify.Musify;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

@Mixin(MapMatrix.class)
public class MapMatrixMixin {

    @Inject(method = "buildInChunk", at = @At(value = "TAIL"), remap = false)
    private void onBuildInChunk(Dungeon dungeon, int cx0, int cz0, CallbackInfo ci) {
        World world = dungeon.map.world;
        Theme theme = dungeon.theme;
        String themeName = theme != null ? theme.name : "unknown";
        File file = new File(world.getSaveHandler().getWorldDirectory(), "dungeon_rooms.csv");

        try (FileWriter writer = new FileWriter(file, true)) {
            java.lang.reflect.Field hubRoomField = Node.class.getDeclaredField("hubRoom");
            hubRoomField.setAccessible(true);

            for (Node node : dungeon.nodes) {
                if (node != null) {
                    Room room = (Room) hubRoomField.get(node);
                    if (room != null) {
                        float worldBeginX = dungeon.map.origenX + room.realX;
                        float worldBeginZ = dungeon.map.origenZ + room.realZ;
                        writer.write(
                                themeName + "," +
                                        worldBeginX + "," +
                                        room.floorY + "," +
                                        room.ceilY + "," +
                                        worldBeginZ + "," +
                                        dungeon.biome.getBiomeName() + "\n"

                        );
                    }
                }
            }
        } catch (IOException | ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}