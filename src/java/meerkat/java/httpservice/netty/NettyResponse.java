package meerkat.java.httpservice.netty;

import clojure.lang.APersistentMap;

public interface NettyResponse {

  void write(APersistentMap response);

  void flush();

  void complete();

  void close();

}
