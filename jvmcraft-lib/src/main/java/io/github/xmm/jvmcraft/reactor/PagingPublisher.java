package io.github.xmm.jvmcraft.reactor;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.reactivestreams.Subscription;
import reactor.core.CorePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;

/**
 * A PagingPublisher is a reactive publisher designed to generate a sequence of items based on an
 * initial value and a generator function. This publisher is intended for asynchronous paging,
 * taking into account the last ID of the previous page. The generator function will be invoked
 * repeatedly until the Flux produced by the generator returns {@code Flux.empty()} on the first
 * request.
 *
 * @param <T> the type of items emitted by this publisher
 */
public class PagingPublisher<T> extends Flux<T> implements CorePublisher<T> {

  private final Callable<T> initSupplier;
  private final Function<T, Flux<T>> generator;

  /**
   * Constructs a PagingPublisher with the specified initial supplier and generator function.
   *
   * @param initSupplier a callable that supplies the initial value
   * @param generator    a function that generates a {@link Flux} based on the last emitted value
   */
  public PagingPublisher(
      Callable<T> initSupplier,
      Function<T, Flux<T>> generator) {
    this.initSupplier = Objects.requireNonNull(initSupplier, "initSupplier");
    this.generator = Objects.requireNonNull(generator, "generator");
  }

  /**
   * Creates a new instance of PagingPublisher. The generator function will be invoked repeatedly
   * until the Flux produced by the generator returns {@code Flux.empty()} on the first request. The
   * {@code initSupplier} may return {@literal null} and the generator should account for this
   * possibility.
   *
   * <p>Example usage:</p>
   * <pre>
   *   class Filter {
   *     public int max;
   *     public Long lastId;
   *   }
   *
   *   PagingPublisher.create(
   *         () -> null,
   *         (lastRecord) -> someRepository.findByFilters(
   *             filter.withLastId(lastRecord != null ? lastRecord.id() : null))
   *     );
   * </pre>
   *
   * @param initSupplier a callable that supplies the initial value for the generator function
   * @param generator    a function that generates a {@link Flux} based on the last emitted value
   * @param <T>          the type of items emitted by this publisher
   * @return a {@link Flux} that emits items generated by the PagingPublisher
   */
  public static <T> Flux<T> create(
      Callable<T> initSupplier,
      Function<T, Flux<T>> generator
  ) {
    return onAssembly(new PagingPublisher<>(initSupplier, generator));
  }

  @Override
  public void subscribe(CoreSubscriber<? super T> actual) {
    T initValue;

    try {
      initValue = initSupplier.call();
    } catch (Throwable ex) {
      Operators.error(actual, Operators.onOperatorError(ex, actual.currentContext()));
      return;
    }

    PagingProcessor<T> parent = new PagingProcessor<>(actual, generator, initValue);
    actual.onSubscribe(parent);
  }

  private static class PagingProcessor<T> implements CoreSubscriber<T>, Subscription {

    final CoreSubscriber<? super T> actual;
    final Function<T, Flux<T>> generator;

    AtomicLong requested = new AtomicLong();
    AtomicBoolean stopped = new AtomicBoolean();
    AtomicBoolean currentCompleted = new AtomicBoolean(true);
    volatile boolean newSubscription = true;
    volatile Subscription s;
    volatile T lastValue;

    public PagingProcessor(CoreSubscriber<? super T> actual, Function<T, Flux<T>> generator,
        T initialValue) {
      this.actual = actual;
      this.generator = generator;
      this.lastValue = initialValue;
    }

    @Override
    public void request(long n) {
//      log.debug("PagingProcessor.request n={}", n); // fixme: add log4j or remove all debug lines
      if (!Operators.validate(n)) {
        return;
      }

      if (stopped.get()) {
        return;
      }

      requested.set(n);

      if (currentCompleted.get()) {
        try {
          doSubscribe();
        } catch (final Throwable ex) {
          error(ex);
        }
      }

      if (s != null) {
        s.request(n);
      }
    }

    @Override
    public void cancel() {
      if (!stopped.get()) {
        stopped.set(true);
        if (s != null) {
          s.cancel();
        }
      }
    }

    public void doSubscribe() {
      final Flux<T> currentPublisher = this.generator.apply(lastValue);
//      log.debug("PagingProcessor.doSubscribe");

      if (currentPublisher == null) {
//        log.debug("PagingProcessor.doSubscribe subscription is empty");
        complete();
        return;
      }

      newSubscription = true;
      currentCompleted.set(false);
      currentPublisher.subscribe(this);
    }

    @Override
    public void onSubscribe(Subscription s) {
      this.s = s;
    }

    @Override
    public void onNext(T t) {
//      log.debug("PagingProcessor.onNext value = {}", t);
      if (t == null) {
        error(new NullPointerException("The generator produced a null value"));
        return;
      }

      newSubscription = false;
      lastValue = t;
      requested.decrementAndGet();

      actual.onNext(t);
    }

    @Override
    public void onComplete() {
//      log.debug("PagingProcessor.onComplete");
      if (newSubscription) {
//        log.debug("PagingProcessor.onComplete newSubscription={}", newSubscription);
        complete();
      }

      currentCompleted.set(true);

      if (requested.get() > 0) {
        request(requested.get());
      }
    }

    @Override
    public void onError(Throwable t) {
      error(t);
    }

    private void error(Throwable t) {
//      log.debug("PagingProcessor.error");
      if (!stopped.get()) {
        stopped.set(true);
        actual.onError(t);
      }
    }

    private void complete() {
//      log.debug("PagingProcessor.complete");
      stopped.set(true);
      actual.onComplete();
    }

  }

}
