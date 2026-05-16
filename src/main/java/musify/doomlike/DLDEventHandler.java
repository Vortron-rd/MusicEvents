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
                        System.out.println("WRITTEN TO FILE?");
                    }
                }
            }
        } catch (IOException | ReflectiveOperationException e) {
            e.printStackTrace();
        }

        System.out.println("------------------------------ DUNGEON THEME AND INFO ------------------------------");
        System.out.println(" ");
        dungeon = event.getDungeon();
        for(Node node : dungeon.nodes) {
            if (node != null) {
                Room room = node.getHubRoom();
                if (room != null) {
                    float worldBeginX = (float)dungeon.map.origenX + room.realX;
                    float worldBeginZ = (float)dungeon.map.origenZ + room.realZ;
                    System.out.println("-------------------------------room-----------------------------");
                    System.out.println(event.getDungeon().theme);
                    System.out.println("DUNGEON MAP X: " + dungeon.map.origenX);
                    System.out.println("DUNGEON MAP Z: " + dungeon.map.origenZ);
                    System.out.println("ROOM REAL X: " + room.realX);
                    System.out.println("ROOM REAL Z: " + room.realZ);
                    System.out.println("ROOM FLOOR Y: " + room.floorY);
                    System.out.println("ROOM CEIL Y: " + room.ceilY);
                    System.out.println("WORLD BEGIN X: " + worldBeginX);
                    System.out.println("WORLD BEGIN Z: " + worldBeginZ);
                    System.out.println("------------------------------------------------------------");
                    System.out.println(dungeon.biome.getBiomeName());
                }
            }
        }

        System.out.println(" ");
        System.out.println("------------------------------------------------------------");
    }
}
