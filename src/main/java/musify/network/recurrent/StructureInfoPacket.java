package musify.network.recurrent;

import io.netty.buffer.ByteBuf;
import musify.Musify;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import static musify.handlers.RecurrentMusicHandler.*;

public class StructureInfoPacket {
    public static class Request implements IMessage {
        private boolean checkOnly;
        private String[] musicList;

        public Request() {} // Required empty constructor

        public Request(boolean checkOnly, String[] musicList) {
            this.checkOnly = checkOnly;
            this.musicList = musicList;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.checkOnly = buf.readBoolean();

            int size = buf.readInt();
            musicList = new String[size];

            for (int i = 0; i < size; i++) {
                musicList[i] = ByteBufUtils.readUTF8String(buf);
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeBoolean(checkOnly);
            buf.writeInt(musicList.length);

            for (String entry : musicList) {
                ByteBufUtils.writeUTF8String(buf, entry);
            }
        }
    }

    // Response sent from server to client
    public static class Response implements IMessage {
        private String musicFile;
        private String structureName;
        private boolean hasStructure;

        public Response() {}

        public Response(String structureName, String musicFile, boolean hasStructure) {
            this.structureName = structureName;
            this.musicFile = musicFile;
            this.hasStructure = hasStructure;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            structureName = ByteBufUtils.readUTF8String(buf);
            musicFile = ByteBufUtils.readUTF8String(buf);
            hasStructure = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, structureName != null ? structureName : "");
            ByteBufUtils.writeUTF8String(buf, musicFile != null ? musicFile : "");
            buf.writeBoolean(hasStructure);
        }
    }

    // Handler for client->server requests
    public static class RequestHandler implements IMessageHandler<Request, Response> {
        @Override
        public Response onMessage(Request message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // Check if player is in a structure
            String structureName = findPlayerStructure(player);

            if (structureName != null && !structureName.isEmpty()) {
                if (message.checkOnly) {
                    // Just checking if still in structure
                    return new Response(structureName, "", true);
                } else {
                    // Select appropriate music for the structure
                    String musicFile = findStructureMusic(structureName, message.musicList);
                    return new Response(structureName, musicFile, true);
                }
            }

            return new Response("", "", false);
        }
    }

    // Handler for server->client responses
    public static class ResponseHandler implements IMessageHandler<Response, IMessage> {
        @Override
        public IMessage onMessage(Response message, MessageContext ctx) {
            // This runs on the client side
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                if (message.hasStructure) {
                    Musify.LOGGER.info("Player is in structure: {}", message.structureName);

                    if (message.musicFile != null) {
                        if (!message.musicFile.isEmpty()) {
                            playStructuremusic(message.musicFile);
                        } else {
                            setRecurrentCounter();
                        }
                    }
                }
            });
            return null;
        }
    }
}