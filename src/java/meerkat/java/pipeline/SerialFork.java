package meerkat.java.pipeline;

import clojure.lang.IFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * This class represents a fork step which completes when all steps have completed.
 * Steps are combined into serial flow.
 */
public class SerialFork implements Function<Object, CompletableFuture<Object>> {

  private final Logger logger = LoggerFactory.getLogger(SerialFork.class);

  private final List steps;
  private final IFn reducer;

  public SerialFork(List steps, IFn reducer) {
    this.steps = steps;
    this.reducer = reducer;
  }

  @Override
  public CompletableFuture<Object> apply(Object context) {
    CompletableFuture<Object> result = null;
    try {
      for (Object o: steps) {
        Step step = (Step) o;
        if (result == null) {
          result = step.apply(context);
        }
        else {
          result = result.thenCombine(step.apply(context), reducer::invoke);
        }
      }
    }
    catch (Throwable e) {
      logger.error("Critical error. Step function must never throws an exception.", e);
      // Don't use complete exceptionally.
      // It is responsibility of clojure code to handle exceptions in pipeline's steps.
      return CompletableFuture.completedFuture(context);
    }
    return result;
  }

}
