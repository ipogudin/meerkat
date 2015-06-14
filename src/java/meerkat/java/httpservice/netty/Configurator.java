package meerkat.java.httpservice.netty;

import io.netty.handler.ssl.SslContext;

public interface Configurator {

  ContextFactory contextFactory();
  RequestHandlingContext requestHandlingContext();
  SslContext sslContext();

}
