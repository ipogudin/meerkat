package meerkat.java.httpservice.netty;

import static meerkat.java.utils.APersistentMapUtils.getIntValue;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import clojure.lang.APersistentMap;
import clojure.lang.IFn;
import clojure.lang.Keyword;

public final class HttpService implements RequestHandlingContext {

    private static final Keyword SSL = Keyword.find("ssl");
    private static final Keyword PORT = Keyword.find("port");
    private static final Keyword ACCEPTOR_POOL_SIZE = Keyword.find("acceptor-pool-size");
    private static final Keyword WORKER_POOL_SIZE = Keyword.find("worker-pool-size");
    private static final Keyword BACKLOG = Keyword.find("backlog");

    private final boolean ssl;
    private final int port;
    private final int acceptorPoolSize;
    private final int workerPoolSize;
    private final int backlog;
    private volatile EventLoopGroup acceptorGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel channel;
    private volatile IFn router;

    public HttpService(APersistentMap configuration) {
      ssl = (boolean) configuration.get(SSL);
      port = ((Long) configuration.get(PORT)).intValue();
      acceptorPoolSize = getIntValue(configuration, ACCEPTOR_POOL_SIZE);
      workerPoolSize = getIntValue(configuration, WORKER_POOL_SIZE);
      backlog = getIntValue(configuration, BACKLOG);
    }

    public void start() throws Exception {
        // Configure SSL.s
        final SslContext sslContext;
        if (ssl) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslContext = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
        } else {
            sslContext = null;
        }

        // Configure the server.
        acceptorGroup = new NioEventLoopGroup(acceptorPoolSize);
        workerGroup = new NioEventLoopGroup(workerPoolSize);

        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, backlog);
        b.group(acceptorGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .handler(new LoggingHandler(LogLevel.DEBUG))
         .childHandler(new HttpServiceInitializer(Configurator.build(RequestFactory.build(), this, sslContext)));

        channel = b.bind(port).sync().channel();
    }

    public void stop() throws Exception {
      acceptorGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
      channel.closeFuture().sync();
    }

    public void setRouter(IFn router) {
      this.router = router;
    }

    @Override
    public IFn router() {
      return router;
    }

}
