package meerkat.java.httpservice.netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;

@Sharable
public class HttpServiceHandler extends ChannelInboundHandlerAdapter {

    private final Configurator configurator;

    public HttpServiceHandler(Configurator configurator) {
      this.configurator = configurator;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) {
      if (msg instanceof FullHttpRequest) {
          try {
            FullHttpRequest httpRequest = (FullHttpRequest) msg;

            configurator.requestHandlingContext().router().invoke(
                configurator.contextFactory().build(httpRequest, context)
                );

          } finally {
            ReferenceCountUtil.release(msg);
          }
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
