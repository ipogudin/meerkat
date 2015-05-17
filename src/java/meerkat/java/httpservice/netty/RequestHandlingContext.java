package meerkat.java.httpservice.netty;

import clojure.lang.IFn;

public interface RequestHandlingContext {

  public IFn router();

}
