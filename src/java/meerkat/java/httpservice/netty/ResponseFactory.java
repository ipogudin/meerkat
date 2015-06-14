package meerkat.java.httpservice.netty;

import io.netty.channel.ChannelHandlerContext;

public interface ResponseFactory {

  NettyResponse build(ChannelHandlerContext channelHandlerContext);

}
