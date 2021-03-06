package meerkat.java.httpservice.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import clojure.lang.APersistentMap;
import clojure.lang.IFn;

public final class HttpServiceImpl {

  private final ConfiguratorImpl configurator;
  
  private volatile EventLoopGroup acceptorGroup;
  private volatile EventLoopGroup workerGroup;
  private volatile Channel channel;

  public HttpServiceImpl(APersistentMap configuration) {
    configurator = new ConfiguratorImpl(configuration);
  }

  public void start() throws Exception {
    configurator.initalize();
    
    // Configure the server.
    acceptorGroup = new NioEventLoopGroup(configurator.acceptorPoolSize());

    if (configurator.workerPoolSize() > 0) {
      workerGroup = new NioEventLoopGroup(configurator.workerPoolSize());
    }
    else {
      workerGroup = acceptorGroup;
    }

    ServerBootstrap b = new ServerBootstrap();
    b.option(ChannelOption.SO_BACKLOG, configurator.backlog());
    b.group(acceptorGroup, workerGroup).channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .childHandler(new HttpServiceInitializer(configurator));

    channel = b.bind(configurator.port()).sync().channel();
  }

  public void stop() throws Exception {
    acceptorGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
    channel.closeFuture().sync();
  }

  public void setRouter(IFn router) {
    configurator.setRouter(router);
  }

}
