package meerkat.java.httpservice.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import clojure.lang.IPersistentMap;

/**
 * A factory interface which should produce request processing context.
 * @author ipogudin
 *
 */
public interface ContextFactory {

  IPersistentMap build(FullHttpRequest httpRequest, ChannelHandlerContext channelHandlerContext);

}