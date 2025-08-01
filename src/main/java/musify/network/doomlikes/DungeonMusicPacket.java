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

    public static class RequestHandler implements IMessageHandler<Request, Response> {
        @Override
        public Response onMessage(Request message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            DungeonHandler.DungeonInfo dungeon = DungeonHandler.findNearbyDungeons(player, message.distance);

            if (dungeon != null) {
                if (message.checkOnly) {
                    return new Response("", true);
                } else {
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

    public static class ResponseHandler implements IMessageHandler<Response, IMessage> {
        @Override
        public IMessage onMessage(Response message, MessageContext ctx) {
            if (message.hasDungeon) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (!message.musicFile.isEmpty()) {
                        DungeonHandler.playDungeonMusic(message.musicFile);
                    } else {
                        DungeonHandler.setDungeonCount();
                    }
                });
            }

            return null;
        }
    }
}