package musify.network.roguelike;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RoguelikeInfoPacket {

    public static class Request implements IMessage {

        @Override
        public void fromBytes(ByteBuf buf) {

        }

        @Override
        public void toBytes(ByteBuf buf) {

        }
    }

    public static class Response implements IMessage {

        @Override
        public void fromBytes(ByteBuf buf) {

        }

        @Override
        public void toBytes(ByteBuf buf) {

        }
    }

    public static class RequestHandler implements IMessageHandler<Request, Response> {

        @Override
        public Response onMessage(Request message, MessageContext ctx) {
            return null;
        }
    }

    public static class ResponseHandler implements IMessageHandler<Response, Request> {

        @Override
        public Request onMessage(Response message, MessageContext ctx) {
            return null;
        }
    }

}
