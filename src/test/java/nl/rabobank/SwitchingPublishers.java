package nl.rabobank;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static reactor.core.publisher.Flux.defer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Basics of learning how to work with empty publishers or
 * unexpected values. Does not explain how to deal with Error
 * mono of flux. For that, see {@link ErrorPublishers}
 *
 * If a publisher ends before any value is published, but you
 * expect your subscriber to act upon it, your application
 * might get stuck since no fallback is implemented. For this
 * reason it is always good practise to consider what should
 * happen if your publisher returns an empty value
 */
@ExtendWith(MockitoExtension.class)
class SwitchingPublishers
{
    /**
     * Return another mono if the initial mono is empty.
     * Mono.switchIfEmpty only support Mono, but
     * Flux.switchIfEmpty supports Flux and Mono
     */
    @Test
    @DisplayName("Switch to another publisher on empty")
    void switchOnEmpty()
    {
        final var monoWithSwitch = emptyMono().switchIfEmpty(Mono.just(1));

        StepVerifier.create(monoWithSwitch)
                .expectNext(1)
                .verifyComplete();

        final var fluxWithSwitch = emptyFlux().switchIfEmpty(Mono.just(1));

        StepVerifier.create(fluxWithSwitch)
                .expectNext(1)
                .verifyComplete();
    }

    /**
     * Almost the same as switchIfEmpty, but only specifying a default value, instead
     * of switching the publisher itself.
     * Flux default will always just have one value. Use switchIfEmpty to switch to a
     * Flux with multiple values
     */
    @Test
    @DisplayName("Publisher with a default value")
    void defaultIfEmpty()
    {
        final var monoWithDefault = emptyMono().defaultIfEmpty(1);

        StepVerifier.create(monoWithDefault)
                .expectNext(1)
                .verifyComplete();

        final var fluxWithDefault = emptyFlux().defaultIfEmpty(1);

        StepVerifier.create(fluxWithDefault)
                .expectNext(1)
                .verifyComplete();
    }

    /**
     * Switch on first value of a flux
     * Uses a {@link java.util.function.BiFunction} with the first
     * {@link reactor.core.publisher.Signal}.
     *
     * Can switch to a completely different Flux depending on given
     * state of that Signal
     */
    @Test
    @DisplayName("Switching publisher on first")
    void switchOnFirst()
    {
        final var switchOnFirstFlux = Flux.just(1, 2, 3, 4, 5)
                .switchOnFirst((signal, flux) -> signal.get() == 2 ? flux : switchFlux(flux));

        StepVerifier.create(switchOnFirstFlux)
                .expectNext(2)
                .expectNext(4)
                .expectNext(6)
                .expectNext(8)
                .verifyComplete();

        final var noSwitchFlux = Flux.just(2, 3, 4, 5)
                .switchOnFirst((signal, flux) -> signal.get() == 2 ? flux : switchFlux(flux));

        StepVerifier.create(noSwitchFlux)
                .expectNext(2)
                .expectNext(3)
                .expectNext(4)
                .expectNext(5)
                .verifyComplete();
    }

    /**
     * Because any publisher is constructed ahead of time as early as possible,
     * if that behaviour is unwanted, we can hold the construction of the publisher
     * until the switch is called with defer()
     */
    @Test
    @DisplayName("Using defer to only create a publisher when the initial flux is empty")
    void switchIfEmptyDefer()
    {
        final var mockService = mock(MockService.class);
        final var flux = Flux.just(1, 2, 3, 4);
        final var switchWithDefer = flux.switchIfEmpty(defer(() -> mockService.getValues()));
        StepVerifier.create(switchWithDefer)
                .expectNextCount(4)
                .verifyComplete();
        verifyNoInteractions(mockService);
    }

    /**
     * If returning another publisher is not acceptable because an empty value is
     * not an acceptable state, consider throwing an error
     *
     * See {@link ErrorPublishers} on how to handle errors
     */
    @Test
    @DisplayName("Return an error if the publisher is empty")
    void switchIfEmptyReturnError()
    {
        final var errorOnEmptyMono = emptyMono().switchIfEmpty(Mono.error(new IllegalStateException("Publisher returned empty")));

        StepVerifier.create(errorOnEmptyMono)
                .verifyError(IllegalStateException.class);
    }

    private Mono<Integer> emptyMono()
    {
        return Mono.empty();
    }

    private Flux<Integer> emptyFlux() { return Flux.empty(); }

    private Flux<Integer> switchFlux(final Flux<Integer> flux)
    {
        return flux.map(i -> i * 2).filter(i -> i < 10);
    }
}
