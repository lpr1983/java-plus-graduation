package ewm.stats.aggregator.model;

public record EventPair(long eventA, long eventB) {

    public EventPair {
        if (eventA >= eventB) {
            throw new IllegalArgumentException("eventA must be less than eventB");
        }
    }

    public static EventPair of(long firstEventId, long secondEventId) {
        if (firstEventId == secondEventId) {
            throw new IllegalArgumentException("An event cannot be paired with itself");
        }
        return firstEventId < secondEventId
                ? new EventPair(firstEventId, secondEventId)
                : new EventPair(secondEventId, firstEventId);
    }
}
