package ewm.stats.analyzer.model;

public class RecommendedEvent {
    private final long eventId;
    private final double score;

    public RecommendedEvent(long eventId, double score) {
        if (!Double.isFinite(score) || score < 0) {
            throw new IllegalArgumentException("Recommendation score must be a finite non-negative number");
        }
        this.eventId = eventId;
        this.score = score;
    }

    public long getEventId() {
        return eventId;
    }

    public double getScore() {
        return score;
    }
}
