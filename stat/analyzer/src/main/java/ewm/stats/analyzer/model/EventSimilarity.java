package ewm.stats.analyzer.model;

import java.time.Instant;
import java.util.Objects;

public class EventSimilarity {
    private final long eventA;
    private final long eventB;
    private final double score;
    private final Instant timestamp;

    private EventSimilarity(long eventA, long eventB, double score, Instant timestamp) {
        if (!Double.isFinite(score) || score < 0) {
            throw new IllegalArgumentException("Similarity score must be a finite non-negative number");
        }
        this.eventA = eventA;
        this.eventB = eventB;
        this.score = score;
        this.timestamp = Objects.requireNonNull(timestamp, "Similarity timestamp must not be null");
    }

    public static EventSimilarity of(long firstEventId, long secondEventId, double score, Instant timestamp) {
        if (firstEventId == secondEventId) {
            throw new IllegalArgumentException("Similarity requires two different events");
        }
        long eventA = Math.min(firstEventId, secondEventId);
        long eventB = Math.max(firstEventId, secondEventId);
        return new EventSimilarity(eventA, eventB, score, timestamp);
    }

    public long getEventA() {
        return eventA;
    }

    public long getEventB() {
        return eventB;
    }

    public double getScore() {
        return score;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getOtherEventId(long eventId) {
        if (eventId == eventA) {
            return eventB;
        }
        if (eventId == eventB) {
            return eventA;
        }
        throw new IllegalArgumentException("Event " + eventId + " is not part of the similarity pair");
    }
}
