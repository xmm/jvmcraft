package io.github.xmm.jvmcraft.reactor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class PagingPublisherTest extends PublisherVerification<Integer> {

  public PagingPublisherTest() {
    super(new TestEnvironment());
  }

  /** fixme
   * Emulate data fetching
   * @param last
   * @param max
   * @param batchSize
   * @return
   */
  private static Flux<Integer> createPageGenerator(Integer last, int max, int batchSize) {
    printf("PageGenerator generator last=%d max=%d batch=%d\n", last, max, batchSize);
    int start = last != null ? last + 1 : 0;
    println("start = " + start);

    if (start >= max) {
      println("PageGenerator STOP last=" + last);
      return Flux.empty();
    }

    int count = Math.min(batchSize, max - start + 1);

    if (count <= 0) {
      println("PagingPublisherTest.createPublisher STOP count=" + count);
      return Flux.empty();
    }
    println("PageGenerator generate start=" + start + " count=" + count);
    return Flux.range(start, count);
  }

  private static void println(String s) {
    System.out.println(s);
  }

  private static void printf(String f, Object... args) {
    System.out.printf(f, args);
  }

  @Test
  public void testBase() {
    CountDownLatch latch = new CountDownLatch(1);
    ArrayList<String> signals = new ArrayList<>();

    PagingPublisher<Long> publisher = new PagingPublisher<>(
        () -> 0L,
        (s) -> {
          println("PagingPublisherTest.test generator");
          return s == null || s == 0
              ? Flux.fromArray(new Long[]{1L, 2L, 3L})
              : null;
        });

    publisher.log().subscribe(new Subscriber<>() {

      @Override
      public void onSubscribe(Subscription s) {
        println("PagingPublisherTest.onSubscribe s=" + s);
        signals.add("onSubscribe");
        println("PagingPublisherTest.onSubscribe lets request(3) elements");
        s.request(3);
      }

      @Override
      public void onNext(Long v) {
        println("PagingPublisherTest.onNext v=" + v);
        signals.add("onNext " + v);
      }

      @Override
      public void onError(Throwable t) {
        println("PagingPublisherTest.onError");
        signals.add("onError");
      }

      @Override
      public void onComplete() {
        println("PagingPublisherTest.onComplete");
        signals.add("onComplete");
        latch.countDown();
      }
    });

    Assertions.assertEquals(
        Arrays.asList(
            "onSubscribe",
            "onNext 1",
            "onNext 2",
            "onNext 3"
        ),
        signals
    );
  }

  @Test
  public void testReSubscription() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    ArrayList<String> signals = new ArrayList<>();
    int init = 2;
    int max = 8;

    PagingPublisher<Integer> publisher = new PagingPublisher<>(
        () -> init,
        (last) -> createPageGenerator(last, init + max, 3));

    publisher.subscribe(new Subscriber<>() {

      @Override
      public void onSubscribe(Subscription s) {
        println("PagingPublisherTest.onSubscribe s=" + s);
        signals.add("onSubscribe");
        println("PagingPublisherTest.onSubscribe lets request(8) elements");
        s.request(8);
      }

      @Override
      public void onNext(Integer v) {
        println("PagingPublisherTest.onNext v=" + v);
        signals.add("onNext " + v);
      }

      @Override
      public void onError(Throwable t) {
        println("PagingPublisherTest.onError");
        signals.add("onError");
      }

      @Override
      public void onComplete() {
        println("PagingPublisherTest.onComplete");
        signals.add("onComplete");
        latch.countDown();
      }
    });

    Assertions.assertEquals(
        Arrays.asList(
            "onSubscribe",
            "onNext 3",
            "onNext 4",
            "onNext 5",
            "onNext 6",
            "onNext 7",
            "onNext 8",
            "onNext 9",
            "onNext 10"
        ),
        signals
    );
  }

  @Test
  void testZero() { // fixme: remove
    StepVerifier.create(Flux.range(0, 0))
        .expectNextCount(0)
        .verifyComplete();
  }

  private Integer initSupplierThrowable() {
    return 1/0;
  }

  @Test
  public void testWhenInitSupplierThrowError() {
    CountDownLatch latch = new CountDownLatch(1);
    ArrayList<String> signals = new ArrayList<>();
    final int init = 0;
    final int max = 2;

    PagingPublisher<Integer> publisher = new PagingPublisher<>(
        () -> initSupplierThrowable(),
        (last) -> createPageGenerator(last, init + max, 1));

    publisher.subscribe(new Subscriber<>() {

      @Override
      public void onSubscribe(Subscription s) {
        println("PagingPublisherTest.onSubscribe s=" + s);
        signals.add("onSubscribe");
        println("PagingPublisherTest.onSubscribe lets request(8) elements");
        s.request(8);
      }

      @Override
      public void onNext(Integer v) {
        println("PagingPublisherTest.onNext v=" + v);
        signals.add("onNext " + v);
      }

      @Override
      public void onError(Throwable t) {
        println("PagingPublisherTest.onError");
        signals.add("onError");
      }

      @Override
      public void onComplete() {
        println("PagingPublisherTest.onComplete");
        signals.add("onComplete");
        latch.countDown();
      }
    });

    Assertions.assertEquals(
        Arrays.asList(
            "onSubscribe",
            "onError"
        ),
        signals
    );
  }

  @Override
  public long maxElementsFromPublisher() {
    return Integer.MAX_VALUE;
  }

  @Override
  public Publisher<Integer> createPublisher(long l) {
    final int max = (int) Math.min(Integer.MAX_VALUE, l);
    final int batchSize = (int) Math.min(5L, l);
    printf("\nPagingPublisherTest.createPublisher l = %d max = %d batchSize = %d\n", l, max,
        batchSize);

    return new PagingPublisher<>(
        () -> null,
        (last) -> createPageGenerator(last, max, batchSize));
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    return Flux.error(new RuntimeException("Some error"));
  }
}