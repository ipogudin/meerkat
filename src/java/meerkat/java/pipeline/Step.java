package meerkat.java.pipeline;

import clojure.lang.AFn;
import clojure.lang.IFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This class represents a smallest step in processing pipeline.
 */
public class Step implements Function<Object, CompletableFuture<Object>> {

  private final Logger logger = LoggerFactory.getLogger(Step.class);

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
    try {
      f.invoke(context, complete);
    }
    catch (Throwable e) {
      logger.error("Critical error. Step function must never throws an exception.", e);
      // Don't use complete exceptionally.
      // It is responsibility of clojure code to handle exceptions in pipeline's steps.
      c.complete(context);
    }
    return c;
  }

}

