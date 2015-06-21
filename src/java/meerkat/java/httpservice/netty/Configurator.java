package meerkat.java.httpservice.netty;

import java.util.Optional;

import io.netty.handler.ssl.SslContext;

public interface Configurator {

  ContextFactory contextFactory();
  RequestHandlingContext requestHandlingContext();
  Optional<SslContext> sslContext();

  boolean ssl();
  int port();
  int acceptorPoolSize();
  int workerPoolSize();
  int backlog();
  int readTimeout();
  int writeTimeout();
  int maxInitialLineLength();
  int maxHeaderSize();
  int maxChunkSize();
}
