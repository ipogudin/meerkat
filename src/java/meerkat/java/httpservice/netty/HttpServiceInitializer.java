package meerkat.java.httpservice.netty;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

public class HttpServiceInitializer extends ChannelInitializer<SocketChannel> {

    private final Configurator configurator;

    public HttpServiceInitializer(Configurator configurator) {
        this.configurator = configurator;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new ReadTimeoutHandler(configurator.readTimeout(), TimeUnit.MILLISECONDS));
        p.addLast(new WriteTimeoutHandler(configurator.writeTimeout(), TimeUnit.MILLISECONDS));
        configurator.sslContext().ifPresent(
            sslContext -> p.addLast(sslContext.newHandler(ch.alloc())));
        
        p.addLast(new HttpServerCodec(
            configurator.maxInitialLineLength(), 
            configurator.maxHeaderSize(),
            configurator.maxChunkSize()));
        p.addLast(new HttpObjectAggregator(
            configurator.maxInitialLineLength()
            + configurator.maxHeaderSize()
            + configurator.maxChunkSize()));
        p.addLast(new HttpServiceHandler(configurator));
    }
}
