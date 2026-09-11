package ewm.stats.aggregator.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventPairTest {

    @Test
    void shouldNormalizeEventIdsAndProduceEqualKeys() {
        EventPair direct = EventPair.of(3, 10);
        EventPair reversed = EventPair.of(10, 3);

        assertThat(reversed).isEqualTo(direct).hasSameHashCodeAs(direct);
        assertThat(reversed.getEventA()).isEqualTo(3);
        assertThat(reversed.getEventB()).isEqualTo(10);
    }

    @Test
    void shouldRejectPairOfTheSameEvent() {
        assertThatThrownBy(() -> EventPair.of(3, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
