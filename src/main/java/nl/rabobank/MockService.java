package nl.rabobank;

import reactor.core.publisher.Flux;

public class MockService
{
    public Flux<Integer> getValues()
    {
        return Flux.just(1, 2, 3);
    }
}
