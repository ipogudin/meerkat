package meerkat.java.httpservice.netty;

import io.netty.handler.codec.http.FullHttpRequest;
import clojure.lang.IPersistentMap;

public interface RequestFactory {

  IPersistentMap build(FullHttpRequest httpRequest);

}