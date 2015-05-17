package meerkat.java.httpservice.netty;

import io.netty.handler.ssl.SslContext;

public interface Configurator {

  RequestFactory requestFactory();
  RequestHandlingContext requestHandlingContext();
  SslContext sslContext();

  static Configurator build(RequestFactory requestFactory,
      RequestHandlingContext requestHandlingContext, SslContext sslContext) {
    return new ConfiguratorImpl(requestFactory,
        requestHandlingContext, sslContext);
  }
}
