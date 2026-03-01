package musify.network;

import musify.Musify;
import musify.network.doomlikes.DungeonMusicPacket;
import musify.network.recurrent.StructureInfoPacket;
import musify.network.roguelike.RoguelikeInfoPacket;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkManager {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Musify.MODID);
    private static int packetId = 0;

    public static void registerPackets() {
        INSTANCE.registerMessage(DungeonMusicPacket.RequestHandler.class, DungeonMusicPacket.Request.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(DungeonMusicPacket.ResponseHandler.class, DungeonMusicPacket.Response.class, packetId++, Side.CLIENT);

        INSTANCE.registerMessage(StructureInfoPacket.RequestHandler.class, StructureInfoPacket.Request.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(StructureInfoPacket.ResponseHandler.class, StructureInfoPacket.Response.class, packetId++, Side.CLIENT);

        INSTANCE.registerMessage(RoguelikeInfoPacket.RequestHandler.class, RoguelikeInfoPacket.Request.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(RoguelikeInfoPacket.ResponseHandler.class, RoguelikeInfoPacket.Response.class, packetId++, Side.CLIENT);
    }
}