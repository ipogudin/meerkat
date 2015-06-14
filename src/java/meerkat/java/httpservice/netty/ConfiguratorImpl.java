package meerkat.java.httpservice.netty;

import io.netty.handler.ssl.SslContext;

public class ConfiguratorImpl implements Configurator {

  private final ContextFactory contextFactory;
  private final RequestHandlingContext requestHandlingContext;
  private final SslContext sslContext;

  ConfiguratorImpl(
      ContextFactory requestFactory,
      RequestHandlingContext requestHandlingContext, 
      SslContext sslContext) {
    super();
    this.contextFactory = requestFactory;
    this.requestHandlingContext = requestHandlingContext;
    this.sslContext = sslContext;
  }

  @Override
  public ContextFactory contextFactory() {
    return contextFactory;
  }

  @Override
  public RequestHandlingContext requestHandlingContext() {
    return requestHandlingContext;
  }

  @Override
  public SslContext sslContext() {
    return sslContext;
  }

}
