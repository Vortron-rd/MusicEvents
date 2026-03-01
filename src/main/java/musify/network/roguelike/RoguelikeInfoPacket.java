package musify.network.roguelike;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import musify.roguelike.RoguelikeMusicHandler;
import musify.config.BiomeMusicConfig;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class RoguelikeInfoPacket {

    public static class Request implements IMessage {
        private boolean checkOnly;
        private String[] musicList;

        public Request() {}
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

    public static class RequestHandler implements IMessageHandler<Request, Response> {
        @Override
        public Response onMessage(Request message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            String structureName = RoguelikeMusicHandler.findPlayerRoguelikeStructure(player);
            if (structureName != null && !structureName.isEmpty()) {
                if (message.checkOnly) {
                    return new Response(structureName, "", true);
                } else {
                    String musicFile = RoguelikeMusicHandler.findRoguelikeStructureMusic(structureName, message.musicList);
                    if (musicFile != null) {
                        return new Response(structureName, musicFile, true);
                    }
                }
            }
            return new Response("", "", false);
        }
    }

    public static class ResponseHandler implements IMessageHandler<Response, IMessage> {
        @Override
        public IMessage onMessage(Response message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                if (message.hasStructure) {
                    if (message.musicFile != null) {
                        if (!message.musicFile.isEmpty()) {
                            RoguelikeMusicHandler.playRoguelikeMusic(message.musicFile);
                        } else {
                            RoguelikeMusicHandler.setRoguelikeCounter();
                        }
                    }
                }
            });
            return null;
        }
    }

}
