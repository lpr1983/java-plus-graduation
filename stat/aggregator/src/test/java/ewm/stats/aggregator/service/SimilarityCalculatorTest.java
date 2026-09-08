package ewm.stats.aggregator.service;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimilarityCalculatorTest {
    private final SimilarityCalculator calculator = new SimilarityCalculator();

    @Test
    void shouldCalculateSimilaritySequentiallyUsingPartialSums() {
        assertThat(calculator.update(action(1, 10, ActionTypeAvro.VIEW))).isEmpty();

        List<EventSimilarityAvro> afterFirstCommonUser = calculator.update(action(1, 20, ActionTypeAvro.VIEW));
        assertSimilarity(afterFirstCommonUser, 10, 20, 1.0);

        List<EventSimilarityAvro> afterOnlyFirstEventChanged = calculator.update(action(2, 10, ActionTypeAvro.VIEW));
        assertThat(afterOnlyFirstEventChanged).isEmpty();

        List<EventSimilarityAvro> afterSecondCommonUser = calculator.update(action(2, 20, ActionTypeAvro.REGISTER));
        assertSimilarity(afterSecondCommonUser, 10, 20, 0.8 / Math.sqrt(0.8 * 1.2));

        List<EventSimilarityAvro> afterWeightIncrease = calculator.update(action(1, 10, ActionTypeAvro.REGISTER));
        assertSimilarity(afterWeightIncrease, 10, 20, 0.8 / Math.sqrt(1.2 * 1.2));
    }

    @Test
    void shouldIgnoreActionThatDoesNotIncreaseMaximumWeight() {
        calculator.update(action(1, 10, ActionTypeAvro.REGISTER));
        calculator.update(action(1, 20, ActionTypeAvro.VIEW));

        assertThat(calculator.update(action(1, 10, ActionTypeAvro.VIEW))).isEmpty();
    }

    @Test
    void shouldNotCalculateSimilarityForEventsOfDifferentUsers() {
        calculator.update(action(1, 10, ActionTypeAvro.VIEW));

        assertThat(calculator.update(action(2, 20, ActionTypeAvro.VIEW))).isEmpty();
    }

    private UserActionAvro action(long userId, long eventId, ActionTypeAvro actionType) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Instant.parse("2026-09-08T10:00:00Z"))
                .build();
    }

    private void assertSimilarity(List<EventSimilarityAvro> similarities, long eventA, long eventB, double score) {
        assertThat(similarities).singleElement().satisfies(similarity -> {
            assertThat(similarity.getEventA()).isEqualTo(eventA);
            assertThat(similarity.getEventB()).isEqualTo(eventB);
            assertThat(similarity.getScore()).isCloseTo(score, within(0.000001));
        });
    }

    private org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
