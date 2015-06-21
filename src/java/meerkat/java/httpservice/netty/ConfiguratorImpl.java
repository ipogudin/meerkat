package meerkat.java.httpservice.netty;

import static meerkat.java.utils.APersistentMapUtils.getIntValue;

import java.util.Optional;

import clojure.lang.APersistentMap;
import clojure.lang.IFn;
import clojure.lang.Keyword;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class ConfiguratorImpl implements Configurator, RequestHandlingContext {

  private static final Keyword SSL = Keyword.intern("ssl");
  private static final Keyword PORT = Keyword.intern("port");
  private static final Keyword ACCEPTOR_POOL_SIZE = Keyword.intern("acceptor-pool-size");
  private static final Keyword WORKER_POOL_SIZE = Keyword.intern("worker-pool-size");
  private static final Keyword BACKLOG = Keyword.intern("backlog");
  private static final Keyword READ_TIMEOUT = Keyword.intern("read-timeout");
  private static final Keyword WRITE_TIMEOUT = Keyword.intern("write-timeout");
  private static final Keyword MAX_INITIAL_LINE_LENGHT = Keyword.intern("max-initial-line-length");
  private static final Keyword MAX_HEADER_SIZE = Keyword.intern("max-header-size");
  private static final Keyword MAX_CHUNK_SIZE = Keyword.intern("max-chunk-size");

  private final boolean ssl;
  private final int port;
  private final int acceptorPoolSize;
  private final int workerPoolSize;
  private final int backlog;
  private final int readTimeout;
  private final int writeTimeout;
  private final int maxInitialLineLength;
  private final int maxHeaderSize;
  private final int maxChunkSize;
  
  private final ContextFactory contextFactory;
  private volatile SslContext sslContext;

  private volatile IFn router;

  private volatile boolean initialized;

  ConfiguratorImpl(APersistentMap configuration) {
    ssl = (boolean) configuration.get(SSL);
    port = ((Number) configuration.get(PORT)).intValue();
    acceptorPoolSize = getIntValue(configuration, ACCEPTOR_POOL_SIZE);
    workerPoolSize = getIntValue(configuration, WORKER_POOL_SIZE);
    backlog = getIntValue(configuration, BACKLOG);
    readTimeout = getIntValue(configuration, READ_TIMEOUT);
    writeTimeout = getIntValue(configuration, WRITE_TIMEOUT);
    maxInitialLineLength = getIntValue(configuration, MAX_INITIAL_LINE_LENGHT);
    maxHeaderSize = getIntValue(configuration, MAX_HEADER_SIZE);
    maxChunkSize = getIntValue(configuration, MAX_CHUNK_SIZE);
    
    this.contextFactory = new ContextFactoryImpl();
  }

  public synchronized void initalize() throws Exception {
    if (initialized) return;
    initialized = true;
    
    // Configure SSL.s
    if (ssl) {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        sslContext = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
    } else {
        sslContext = null;
    }
  }

  @Override
  public ContextFactory contextFactory() {
    return contextFactory;
  }

  @Override
  public RequestHandlingContext requestHandlingContext() {
    return this;
  }

  @Override
  public Optional<SslContext> sslContext() {
    return Optional.ofNullable(sslContext);
  }

  @Override
  public IFn router() {
    return router;
  }

  @Override
  public void setRouter(IFn router) {
    this.router = router;
  }

  public boolean ssl() {
    return ssl;
  }

  public int port() {
    return port;
  }

  public int acceptorPoolSize() {
    return acceptorPoolSize;
  }

  public int workerPoolSize() {
    return workerPoolSize;
  }

  public int backlog() {
    return backlog;
  }

  public int readTimeout() {
    return readTimeout;
  }

  public int writeTimeout() {
    return writeTimeout;
  }

  public int maxInitialLineLength() {
    return maxInitialLineLength;
  }

  public int maxHeaderSize() {
    return maxHeaderSize;
  }

  public int maxChunkSize() {
    return maxChunkSize;
  }

}
