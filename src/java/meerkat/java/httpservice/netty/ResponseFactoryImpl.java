package meerkat.java.httpservice.netty;

import io.netty.channel.ChannelHandlerContext;

public class ResponseFactoryImpl implements ResponseFactory {

  @Override
  public NettyResponse build(ChannelHandlerContext channelHandlerContext) {
    return new NettyResponseImpl(channelHandlerContext);
  }

}
