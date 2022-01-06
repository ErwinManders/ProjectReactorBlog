package nl.rabobank;


import static java.lang.String.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Basics of how to handle exceptions in reactor
 * When an error occurs, we can decide to handle it, map it
 * to another exception or perform an action and continue
 *
 * We can specify a {@link Throwable} on which to act upon
 * or a {@link java.util.function.Predicate} to act upon if
 * true is returned for most of these operations
 */
@Slf4j
class ErrorPublishers
{
    /**
     * Return a default value if an error occurs
     *
     */
    @Test
    @DisplayName("Return another value on error")
    void onErrorReturn()
    {
        final var onErrorReturnFlux = operationWithError().onErrorReturn(1);

        StepVerifier.create(onErrorReturnFlux)
                .expectNext(1)
                .verifyComplete();

        final var onErrorReturnForClass = operationWithError().onErrorReturn(PublisherException.class, 1);

        StepVerifier.create(onErrorReturnForClass)
                .expectNext(1)
                .verifyComplete();

        final var onErrorReturnForPredicate = operationWithError().onErrorReturn(throwable -> throwable.getMessage().contains("Error"), 1);

        StepVerifier.create(onErrorReturnForPredicate)
                .expectNext(1)
                .verifyComplete();
    }

    /**
     * Return an alternate publisher if an error is returned from the initial publisher
     */
    @Test
    @DisplayName("Resume with another publisher on error")
    void onErrorResume()
    {
        final var alternate = Flux.just(1);

        final var onErrorResumeFlux = operationWithError().onErrorResume(throwable -> alternate);

        StepVerifier.create(onErrorResumeFlux)
                .expectNext(1)
                .verifyComplete();

        final var onErrorResumeFluxForClass = operationWithError().onErrorResume(PublisherException.class, throwable -> alternate);

        StepVerifier.create(onErrorResumeFluxForClass)
                .expectNext(1)
                .verifyComplete();

        final var onErrorResumeForPredicate = operationWithError().onErrorResume(throwable -> throwable.getMessage().contains("Error"), throwable -> alternate);

        StepVerifier.create(onErrorResumeForPredicate)
                .expectNext(1)
                .verifyComplete();
    }

    /**
     * Map the given exception and throw another one back to the subscriber
     * The publisher can be tested by verifying the error in the step verifier
     * Use this if code that is not yours throws unwanted exceptions that
     * you need to map to exceptions that your subscriber can work with
     */
    @Test
    @DisplayName("Mapping an error without handling it")
    void onErrorMap()
    {
        final var onErrorMapFlux = operationWithError().onErrorMap(throwable -> new IllegalStateException());

        StepVerifier.create(onErrorMapFlux)
                .verifyError(IllegalStateException.class);

        final var onErrorMapForClass = operationWithError().onErrorMap(NullPointerException.class, throwable -> new IllegalStateException());

        StepVerifier.create(onErrorMapForClass)
                .verifyError(PublisherException.class);

        final var onErrorMapPredicate = operationWithError().onErrorMap(throwable -> throwable.getMessage().contains("Error"), throwable -> new IllegalStateException());

        StepVerifier.create(onErrorMapPredicate)
                .verifyError(IllegalStateException.class);
    }

    /**
     * Perform an action when an exception occurs but do not map or catch it
     * Returns the initial exception to the subscriber after performing the
     * {@link java.util.function.Consumer} function
     */
    @Test
    @DisplayName("Continue on error without handling it (but performing an action)")
    void onErrorContinue()
    {
        final var onErrorContinueFlux = operationWithError().onErrorContinue((throwable, o) -> log.error(format("Object: %s, Exception: %s", o, throwable)));

        StepVerifier.create(onErrorContinueFlux)
                .verifyError(PublisherException.class);
    }

    @Test
    @DisplayName("When calling non-reactive code within your reactor that throws an exception")
    void externalExceptions()
    {
        final var exceptionMono = Mono.just(1).doOnNext(i -> someExternalMethod());

        StepVerifier.create(exceptionMono)
                .verifyError(NullPointerException.class);
    }



    /**
     * When throwing errors in reactor code, use Flux.error() or Mono.error()
     * Throwing exceptions in Reactor does not require the return type of the
     * method to be Mono or Flux of Error
     * @return Flux
     */
    private Flux<Integer> operationWithError()
    {
        return Flux.error(new PublisherException("Error"));
    }

    private void someExternalMethod()
    {
        throw new NullPointerException("External method threw exception");
    }

    private static class PublisherException extends RuntimeException
    {
        static final long serialVersionUID = 1L;

        public PublisherException(final String message)
        {
            super(message);
        }

    }

}
