package musify.doomlike;

import jaredbgreat.dldungeons.api.DLDEvent;
import jaredbgreat.dldungeons.planner.Dungeon;
import jaredbgreat.dldungeons.planner.Node;
import jaredbgreat.dldungeons.rooms.Room;
import jaredbgreat.dldungeons.themes.Theme;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DLDEventHandler {

    @SubscribeEvent
    public static void DLDEventHandler(DLDEvent.BeforeBuild event) {

        Dungeon dungeon = event.getDungeon();
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
