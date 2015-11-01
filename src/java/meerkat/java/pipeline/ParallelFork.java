package meerkat.java.pipeline;

import clojure.lang.IFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * This class represents a fork step which completes when all steps have completed.
 * Steps are run in parallel flow.
 */
public class ParallelFork implements Function<Object, CompletableFuture<Object>> {

  private final Logger logger = LoggerFactory.getLogger(SerialFork.class);

  private final List steps;
  private final IFn reducer;
  private final Executor executor;

  public ParallelFork(List steps, IFn reducer, Executor executor) {
    this.steps = steps;
    this.reducer = reducer;
    this.executor = executor;
  }

  public ParallelFork(List steps, IFn reducer) {
    this(steps, reducer, null);
  }

  @Override
  public CompletableFuture<Object> apply(Object context) {
    final CompletableFuture<Object> result = new CompletableFuture<>();
    final AtomicInteger completeSteps = new AtomicInteger(steps.size());
    final AtomicReference<Object> resultContext = new AtomicReference<>();
    for (Object o: steps) {
      Step step = (Step) o;
      Runnable r = () -> {
        try {
          step.apply(context).thenAccept((c) -> {
            resultContext.accumulateAndGet(c, reducer::invoke);
            if (completeSteps.decrementAndGet() == 0) {
              result.complete(resultContext.get());
            }
          });
        }
        catch (Throwable e) {
          logger.error("Critical error. Step function must never throws an exception.", e);
          if (completeSteps.decrementAndGet() == 0) {
            result.complete(resultContext.get());
          }
        }
      };
      if (executor != null) {
        CompletableFuture.runAsync(r, executor);
      }
      else {
        CompletableFuture.runAsync(r);
      }
    }
    return result;
  }

}
