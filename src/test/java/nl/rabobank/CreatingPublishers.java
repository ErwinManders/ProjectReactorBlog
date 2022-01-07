package nl.rabobank;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Basics of how to construct {@link Mono} or {@link Flux} publishers
 * Always use the most appropriate way to create a publisher from your
 * data depending on what that data looks like.
 *
 * Do not use just if the object is nullable
 * Use justOrEmpty for nullable or {@link Optional} objects,
 * see {@link #justOrEmpty()}
 * Use fromIterable or fromStream to create Flux from collections,
 * see {@link #fromIterable()} and {@link #fromStream()}
 * Use map, flatMap and flatMapMany to create Mono or Flux
 * from more complex objects, see {@link #mappingPublishers()}
 *
 */
class CreatingPublishers
{
    private static final String VALUE = "test";

    /**
     * Most default construction of a Mono.
     * Do not use for nullable objects
     * Publishers cannot publish null values
     */
    @Test
    @DisplayName("Creating a new mono")
    void createMono()
    {
        final var mono = Mono.just(VALUE);

        StepVerifier.create(mono)
                .expectNext(VALUE)
                .verifyComplete();
    }

    /**
     * Most default construction of a Flux.
     * To convert collections into a Flux
     * use fromIterable
     */
    @Test
    @DisplayName("Creating a new flux - combine expect next / assert next")
    void createFlux()
    {
        final var flux = Flux.just(VALUE, VALUE, VALUE);

        StepVerifier.create(flux)
                .expectNextCount(1)
                .expectNext(VALUE)
                .assertNext(value -> assertThat(value).isEqualTo(VALUE))
                .verifyComplete();
    }

    /**
     * Use justOrEmpty to create publishers from nullable
     * or {@link Optional} objects
     * See {@link SwitchingPublishers} to learn how to deal
     * with empty publishers
     */
    @Test
    @DisplayName("Just or empty with optional or null values")
    void justOrEmpty()
    {
        final var nonNullValue = Mono.justOrEmpty(VALUE);

        StepVerifier.create(nonNullValue)
                .expectNext(VALUE)
                .verifyComplete();

        final var optional = Optional.of(VALUE);
        final var optionalWithValue = Mono.justOrEmpty(optional);

        StepVerifier.create(optionalWithValue)
                .expectNext(VALUE)
                .verifyComplete();

        final var emptyOptional = Mono.justOrEmpty(Optional.empty());

        StepVerifier.create(emptyOptional)
                .verifyComplete();

        final var nullValue = Mono.justOrEmpty(null);

        StepVerifier.create(nullValue)
                .verifyComplete();
    }

    /**
     * Use fromIterable to construct a Flux from any {@link java.util.Collection}
     * If a Mono of {@link List} is desired, use collectList()
     */
    @Test
    @DisplayName("From iterable to construct a Flux from a collection")
    void fromIterable()
    {
        final var list = List.of(VALUE, VALUE, VALUE);

        final var fluxFromList = Flux.fromIterable(list);

        StepVerifier.create(fluxFromList)
                .expectNext(VALUE)
                .expectNext(VALUE)
                .expectNext(VALUE)
                .verifyComplete();

        final var monoOfList = fluxFromList.collectList();

        StepVerifier.create(monoOfList)
                .assertNext(l -> assertThat(l).hasSize(3));
    }

    /**
     * Use fromStream to convert a {@link Stream} to Flux
     */
    @Test
    @DisplayName("From stream to construct a Flux from a stream")
    void fromStream()
    {
        final var stream = Stream.of(VALUE, VALUE, VALUE);

        final var fluxFromStream = Flux.fromStream(stream);

        StepVerifier.create(fluxFromStream)
                .expectNext(VALUE)
                .expectNext(VALUE)
                .expectNext(VALUE)
                .verifyComplete();
    }

    /**
     * Combine previous constructors with maps to construct Mono or Flux
     * from complex objects
     *
     * For example:
     * Mapping into null objects creates empty Mono
     * flatMapMany into a Flux will flatmap it into one Flux
     */
    @Test
    @DisplayName("Combine justOrEmpty and fromIterable with map and flat map many")
    void mappingPublishers()
    {
        final var nullIterable = createFluxFromObject(new FlatMapIterableObject(null));

        StepVerifier.create(nullIterable)
                .verifyComplete();

        final var emptyIterable = createFluxFromObject(new FlatMapIterableObject(new FlatMapIterableObject.IterableObject(Collections.emptyList())));

        StepVerifier.create(emptyIterable)
                .verifyComplete();

        final var valueWithNull = createFluxFromObject(new FlatMapIterableObject(new FlatMapIterableObject.IterableObject(List.of(new FlatMapIterableObject.ValueObject("value"), new FlatMapIterableObject.ValueObject(null)))));

        StepVerifier.create(valueWithNull)
                .expectNextCount(1)
                .verifyComplete();
    }

    private Flux<String> createFluxFromObject(final FlatMapIterableObject flatMapIterableObject)
    {
        return Mono.justOrEmpty(flatMapIterableObject.getIterableObject())
                .flatMapMany(iterableObject -> Flux.fromIterable(iterableObject.getValueObjects()))
                .map(FlatMapIterableObject.ValueObject::getValue);
    }

}

