package meerkat.java.httpservice.netty;

import io.netty.handler.ssl.SslContext;

public interface Configurator {

  RequestFactory requestFactory();
  ResponseFactory responseFactory();
  RequestHandlingContext requestHandlingContext();
  SslContext sslContext();

}
