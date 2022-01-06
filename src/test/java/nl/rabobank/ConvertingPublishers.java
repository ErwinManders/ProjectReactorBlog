package nl.rabobank;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * How to combine instances of Mono and Flux or
 * how to convert them between each other.
 *
 * To convert a Flux to a Mono use next() or last(),
 * see {@link #singleOrLast()}
 * To repeat the value of a Mono as a Flux use repat,
 * see {@link #repeatMono()}
 * To zip a publisher with another, see:
 * {@link #monoZipMonoTuple()}
 * {@link #monoZipMonoCombinator()}
 * {@link #fluxZipTuple()}
 * {@link #fluxZipMonoWithRepeat()}
 * {@link #fluxZipTupleCombinator()}
 * {@link #zipFluxWithMono()}
 * {@link #zipMoreThanTwo()}
 *
 */
class ConvertingPublishers
{
    /**
     * Convert Flux to Mono. Only one value of the flux will be published
     * Otherwise, use collectList() if the entire data stream should be
     * published as a list
     */
    @Test
    @DisplayName("First or last value of a flux")
    void singleOrLast()
    {
        final var flux = Flux.range(1, 3);

        final var next = flux.next();
        final var last = flux.last(2);
        // final var single = flux.single();

        StepVerifier.create(next)
                .expectNext(1)
                .verifyComplete();
        StepVerifier.create(last)
                .expectNext(3)
                .verifyComplete();
    }

    /**
     *  Repeat a mono an x amount of times as a flux
     *  If no value is given, will repeat until subscriber closes
     *  See {@link #fluxZipMonoWithRepeat()} for example
     *  of zipping a Flux with a Mono.repeat
     */
    @Test
    @DisplayName("Repeating a value - keeping the original value and repeating x times")
    void repeatMono()
    {
        final var flux = Mono.just(1).repeat(3);

        StepVerifier.create(flux)
                .expectNextCount(4)
                .verifyComplete();
    }

    /**
     * Zip two mono into a single one
     * Result is a mono of {@link reactor.util.function.Tuple2}
     * with the left and right value as the respective
     * mono values
     */
    @Test
    @DisplayName("Zipping a mono with a mono")
    void monoZipMonoTuple()
    {
        final var mono = Mono.just(1);
        final var mono2 = Mono.just(2);

        final var zippedMono = mono.zipWith(mono2);

        StepVerifier.create(zippedMono)
                .assertNext(tuple -> {
                    assertThat(tuple.getT1()).isEqualTo(1);
                    assertThat(tuple.getT2()).isEqualTo(2);
                })
                .verifyComplete();
    }

    /**
     * Using a combinator to zip two mono
     * Result is a mono of the {@link java.util.function.Function}
     * returned value
     */
    @Test
    @DisplayName("Zipping a mono with a mono with a combinator")
    void monoZipMonoCombinator()
    {
        final var mono = Mono.just(1);
        final var mono2 = Mono.just(2);

        final var zippedMono = mono.zipWith(mono2, (a,b) -> a + b);

        StepVerifier.create(zippedMono)
                .expectNext(3)
                .verifyComplete();
    }

    /**
     * Zipping a flux with another flux
     * Results in a flux of {@link reactor.util.function.Tuple2}
     * The resulting flux ends when one of the two original
     * flux ends
     */
    @Test
    @DisplayName("Zipping a flux with a flux")
    void fluxZipTuple()
    {
        final var flux = Flux.just(1, 2);
        final var flux2 = Flux.just(3, 4, 5);

        final var zippedFlux = flux.zipWith(flux2);

        StepVerifier.create(zippedFlux)
                .expectNextCount(2)
                .verifyComplete();
    }

    /**
     * Zip a flux with a mono, using repeat
     * Results in a flux of {@link reactor.util.function.Tuple2}
     * with the right value repeating
     * The resulting flux ends when the original flux ends
     */
    @Test
    @DisplayName("Zip a flux with a mono using repeat")
    void fluxZipMonoWithRepeat()
    {
        final var flux = Flux.just(1, 2, 3, 4);
        final var mono = Mono.just(1);

        final var zippedFlux = flux.zipWith(mono);

        StepVerifier.create(zippedFlux)
                .expectNextCount(1)
                .verifyComplete();

        final var zippedFluxRepeatMono = flux.zipWith(mono.repeat());

        StepVerifier.create(zippedFluxRepeatMono)
                .expectNextCount(4)
                .verifyComplete();
    }

    /**
     * Zip a flux with another flux using a combinator
     * Results in a flux of the {@link java.util.function.Function}
     * returned value
     * The resulting flux ends when one of the two original flux ends
     */
    @Test
    @DisplayName("Zipping a flux with a flux with a combinator")
    void fluxZipTupleCombinator()
    {
        final var flux = Flux.just(1, 2);
        final var flux2 = Flux.just(3, 4, 5);

        final var zippedFlux = flux.zipWith(flux2, (a,b) -> a + b);

        StepVerifier.create(zippedFlux)
                .expectNext(4)
                .expectNext(6)
                .verifyComplete();
    }

    /**
     * Zip a mono with a flux. Requires converting the flux
     * into a mono with next(), last(), etc.
     */
    @Test
    @DisplayName("Zipping a flux with a mono (or mono with a flux)")
    void zipFluxWithMono()
    {
        final var flux = Flux.just(1, 2, 3);
        final var mono = Mono.just("1");

        StepVerifier.create(mono.zipWith(flux.next(), (a, b) -> Integer.parseInt(a) + b))
                .expectNext(2)
                .verifyComplete();
    }

    /**
     * Zipping more than one publisher into one
     * All of above rules apply
     * If one publisher ends, the resulting publisher ends
     * Results in a Tuple with the amount of publishers
     */
    @Test
    @DisplayName("Zip more than 2 publishers into one")
    void zipMoreThanTwo()
    {
        final var flux1 = Flux.just(1, 2, 3);
        final var flux2 = Flux.just(4, 5);
        final var mono = Mono.just("1");

        StepVerifier.create(Flux.zip(flux1, flux2, mono))
                .assertNext(tuple -> {
                    assertThat(tuple.getT1()).isEqualTo(1);
                    assertThat(tuple.getT2()).isEqualTo(4);
                    assertThat(tuple.getT3()).isEqualTo("1");
                })
                .verifyComplete();

        StepVerifier.create(Flux.zip(flux1, flux2, mono.repeat()))
                .expectNextCount(2)
                .verifyComplete();
    }


}
