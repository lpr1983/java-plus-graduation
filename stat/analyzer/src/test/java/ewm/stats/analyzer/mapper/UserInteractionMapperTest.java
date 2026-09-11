package ewm.stats.analyzer.mapper;

import ewm.stats.analyzer.model.UserInteraction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UserInteractionMapperTest {
    private static final Instant TIMESTAMP = Instant.parse("2026-09-10T12:00:00Z");
    private final UserInteractionMapper mapper = new UserInteractionMapper();

    @ParameterizedTest
    @MethodSource("actions")
    void shouldMapActionWeight(ActionTypeAvro actionType, double expectedWeight) {
        UserActionAvro action = UserActionAvro.newBuilder()
                .setUserId(10)
                .setEventId(20)
                .setActionType(actionType)
                .setTimestamp(TIMESTAMP)
                .build();

        UserInteraction interaction = mapper.toModel(action);

        assertThat(interaction.getUserId()).isEqualTo(10);
        assertThat(interaction.getEventId()).isEqualTo(20);
        assertThat(interaction.getWeight()).isEqualTo(expectedWeight);
        assertThat(interaction.getTimestamp()).isEqualTo(TIMESTAMP);
    }

    private static Stream<Arguments> actions() {
        return Stream.of(
                Arguments.of(ActionTypeAvro.VIEW, 0.4),
                Arguments.of(ActionTypeAvro.REGISTER, 0.8),
                Arguments.of(ActionTypeAvro.LIKE, 1.0)
        );
    }
}
