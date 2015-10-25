package meerkat.java.pipeline;

import clojure.lang.AFn;
import clojure.lang.IFn;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Step implements Function<Object, CompletableFuture<Object>> {

  private final IFn f;

  public Step(IFn f) {
    this.f = f;
  }

  @Override
  public CompletableFuture<Object> apply(Object context) {
    final CompletableFuture<Object> c = new CompletableFuture<>();
    final IFn complete = new AFn() {
      @Override
      public Object invoke(Object context) {
        c.complete(context);
        return context;
      }
    };
    f.invoke(context, complete);
    return c;
  }

}
