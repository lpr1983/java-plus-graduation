package ewm.stats.analyzer.model;

import java.time.Instant;
import java.util.Objects;

public class UserInteraction {
    private final long userId;
    private final long eventId;
    private final double weight;
    private final Instant timestamp;

    public UserInteraction(long userId, long eventId, double weight, Instant timestamp) {
        if (!Double.isFinite(weight) || weight < 0) {
            throw new IllegalArgumentException("Interaction weight must be a finite non-negative number");
        }

        this.userId = userId;
        this.eventId = eventId;
        this.weight = weight;
        this.timestamp = Objects.requireNonNull(timestamp, "Interaction timestamp must not be null");
    }

    public long getUserId() {
        return userId;
    }

    public long getEventId() {
        return eventId;
    }

    public double getWeight() {
        return weight;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
