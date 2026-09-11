package ewm.stats.aggregator.model;

import java.util.Objects;

public final class EventPair {
    private final long eventA;
    private final long eventB;

    private EventPair(long eventA, long eventB) {
        this.eventA = eventA;
        this.eventB = eventB;
    }

    public static EventPair of(long firstEventId, long secondEventId) {
        if (firstEventId == secondEventId) {
            throw new IllegalArgumentException("An event cannot be paired with itself");
        }
        return firstEventId < secondEventId
                ? new EventPair(firstEventId, secondEventId)
                : new EventPair(secondEventId, firstEventId);
    }

    public long getEventA() {
        return eventA;
    }

    public long getEventB() {
        return eventB;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof EventPair)) {
            return false;
        }
        EventPair other = (EventPair) object;
        return eventA == other.eventA && eventB == other.eventB;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventA, eventB);
    }
}
