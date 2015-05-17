package meerkat.java.httpservice.netty;

import io.netty.handler.ssl.SslContext;

public class ConfiguratorImpl implements Configurator {

  private final RequestFactory requestFactory;
  private final RequestHandlingContext requestHandlingContext;
  private final SslContext sslContext;

  ConfiguratorImpl(RequestFactory requestFactory,
      RequestHandlingContext requestHandlingContext, SslContext sslContext) {
    super();
    this.requestFactory = requestFactory;
    this.requestHandlingContext = requestHandlingContext;
    this.sslContext = sslContext;
  }

  @Override
  public RequestFactory requestFactory() {
    return requestFactory;
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
