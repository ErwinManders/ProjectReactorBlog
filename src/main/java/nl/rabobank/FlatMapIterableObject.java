package nl.rabobank;

import java.util.List;
import lombok.Value;
import reactor.core.publisher.Mono;

@Value
public class FlatMapIterableObject
{
    IterableObject iterableObject;

    @Value
    public static class IterableObject
    {
        List<ValueObject> valueObjects;
    }

    @Value
    public static class ValueObject
    {
        String value;
    }
}
