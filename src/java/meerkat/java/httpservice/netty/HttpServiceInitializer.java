package meerkat.java.httpservice.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpServiceInitializer extends ChannelInitializer<SocketChannel> {

    private final Configurator configurator;

    public HttpServiceInitializer(Configurator configurator) {
        this.configurator = configurator;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (configurator.sslContext() != null) {
            p.addLast(configurator.sslContext().newHandler(ch.alloc()));
        }
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(8192));
        p.addLast(new HttpServiceHandler(configurator));
    }
}
