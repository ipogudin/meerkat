package meerkat.java.httpservice.netty;

import clojure.lang.APersistentMap;

public interface NettyResponseProcessor {

  void write(APersistentMap response);

  void flush();

  void complete();

  void close();

}
