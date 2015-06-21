package meerkat.java.httpservice.netty;

import clojure.lang.IFn;

public interface RequestHandlingContext {

  IFn router();

  void setRouter(IFn router);
}
