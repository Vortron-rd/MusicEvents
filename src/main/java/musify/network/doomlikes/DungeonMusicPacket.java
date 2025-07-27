package musify.network.doomlikes;

import io.netty.buffer.ByteBuf;
import musify.handlers.DungeonHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class DungeonMusicPacket {
    public static class Request implements IMessage {
        private int distance;
        private boolean checkOnly;
        private String[] musicList;

        public Request() {}

        public Request(int distance, boolean checkOnly, String[] musicList) {
            this.distance = distance;
            this.checkOnly = checkOnly;
            this.musicList = musicList;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            distance = buf.readInt();
            checkOnly = buf.readBoolean();

            int size = buf.readInt();
            musicList = new String[size];

            for (int i = 0; i < size; i++) {
                musicList[i] = ByteBufUtils.readUTF8String(buf);
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(distance);
            buf.writeBoolean(checkOnly);

            buf.writeInt(musicList.length);

            for (String entry : musicList) {
                ByteBufUtils.writeUTF8String(buf, entry);
            }
        }
    }

    public static class Response implements IMessage {
        private String musicFile;
        private boolean hasDungeon;

        public Response() {}

        public Response(String musicFile, boolean hasDungeon) {
            this.musicFile = musicFile;
            this.hasDungeon = hasDungeon;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            musicFile = ByteBufUtils.readUTF8String(buf);
            hasDungeon = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, musicFile != null ? musicFile : "");
            buf.writeBoolean(hasDungeon);
        }
    }

    // Handler for client->server requests
    public static class RequestHandler implements IMessageHandler<Request, Response> {
        @Override
        public Response onMessage(Request message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // Use existing method to find dungeon
            DungeonHandler.DungeonInfo dungeon = DungeonHandler.findNearbyDungeons(player, message.distance);

            if (dungeon != null) {
                if (message.checkOnly) {
                    // Just checking if still in dungeon
                    return new Response("", true);
                } else {
                    // Using client's music list instead of server's
                    String theme = dungeon.getTheme();
                    String musicFile = DungeonHandler.findDungeonMusic(theme, message.musicList);

                    if (musicFile != null) {
                        return new Response(musicFile, true);
                    }
                }
            }

            return new Response("", false);
        }
    }

    // Handler for server->client responses
    public static class ResponseHandler implements IMessageHandler<Response, IMessage> {
        @Override
        public IMessage onMessage(Response message, MessageContext ctx) {
            // This runs on the client side
            if (message.hasDungeon) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (!message.musicFile.isEmpty()) {
                        // Play the new music
                        DungeonHandler.playDungeonMusic(message.musicFile);
                    } else {
                        // Just extend current music duration
                        DungeonHandler.setDungeonCount();
                    }
                });
            }

            return null;
        }
    }
}