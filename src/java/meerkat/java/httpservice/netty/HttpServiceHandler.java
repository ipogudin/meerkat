package meerkat.java.httpservice.netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Sharable
public class HttpServiceHandler extends ChannelInboundHandlerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(HttpServiceHandler.class);
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
    if (cause instanceof TimeoutException) {
      logger.trace("Exception caught in channel handler.", cause);
    } else {
      logger.error("Exception caught in channel handler.", cause);
    }
    try {
      ctx.close();
    } catch (Exception e) {
      logger.error("Can not close channel handler context.", cause);
    }
  }
}
