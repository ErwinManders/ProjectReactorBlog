package nl.rabobank;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.publisher.Mono.fromRunnable;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.test.StepVerifier;

/**
 * Since publishers are only constructed when the subscriber has need for the data stream,
 * the publisher might be constructed once for each subscriber active upon it. To prevent
 * this, we can cache the publisher itself, or cache the value the publisher's data in an
 * external cache to prevent it from being requested once per subscriber
 */
@ExtendWith(MockitoExtension.class)
class CachePublishers
{
    /**
     * A Flux with three subscribers, will have it's data stream constructed once per subscriber
     */
    @Test
    @DisplayName("A flux that is uncached")
    void uncachedFlux()
    {
        final var count = new AtomicInteger();

        final var uncachedMono = Mono.just(1).doOnNext(i -> count.incrementAndGet());

        StepVerifier.create(uncachedMono)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(uncachedMono)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(uncachedMono)
                .expectNextCount(1)
                .verifyComplete();

        assertThat(count.get()).isEqualTo(3);

    }

    /**
     * If we cache the publisher, the data stream will be constructed only for the first subscriber
     * and will be re-usable after that
     */
    @Test
    @DisplayName("Using mono.cache to prevent operations from running again")
    void cacheFlux()
    {
        final var count = new AtomicInteger();

        final var cachedMono = Mono.just(1).doOnNext(i -> count.incrementAndGet()).cache();

        StepVerifier.create(cachedMono)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(cachedMono)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(cachedMono)
                .expectNextCount(1)
                .verifyComplete();

        assertThat(count.get()).isEqualTo(1);
    }

    /**
     * Using an external cache, we can use Spring Cache with the CacheMono or FluxMono
     * from the reactor addons to create a data stream that does a lookup, performs a
     * function on cache miss and writes to cache after the function is done
     */
    @Test
    @DisplayName("Using CacheMono to fetch from external cache")
    void cacheMono()
    {
        final var cacheManager = new ConcurrentMapCacheManager();
        final var cache = cacheManager.getCache("local-cache");
        final var cacheKey = "cacheKey";

        final var count = new AtomicInteger();

        final var lookUpAndCacheMono = CacheMono
                .lookup(key -> Mono.justOrEmpty(cache.get(key, CacheValue.class)).map(Signal::next), cacheKey)
                .onCacheMissResume(() -> Mono.just(new CacheValue("value")).doOnNext(value -> count.incrementAndGet()))
                .andWriteWith((key, signal) -> fromRunnable(
                        () -> ofNullable(signal.get())
                                .ifPresent(value -> cache.put(key, value))));

        StepVerifier.create(lookUpAndCacheMono)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(lookUpAndCacheMono)
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(lookUpAndCacheMono)
                .expectNextCount(1)
                .verifyComplete();

        assertThat(count.get()).isEqualTo(1);
    }

    private static final class CacheValue
    {
        private final String value;

        public CacheValue(final String value)
        {
            this.value = value;
        }
    }


}
