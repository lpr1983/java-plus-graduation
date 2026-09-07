package ewm.stats.collector.mapper;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserActionMapperTest {

    @Test
    void shouldMapUserAction() {
        UserActionProto proto = UserActionProto.newBuilder()
                .setUserId(12L)
                .setEventId(34L)
                .setActionType(ActionTypeProto.ACTION_REGISTER)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(1_700_000_000L)
                        .setNanos(123_000_000)
                        .build())
                .build();

        UserActionAvro avro = UserActionMapper.toAvro(proto);

        assertThat(avro.getUserId()).isEqualTo(12L);
        assertThat(avro.getEventId()).isEqualTo(34L);
        assertThat(avro.getActionType()).isEqualTo(ActionTypeAvro.REGISTER);
        assertThat(avro.getTimestamp()).isEqualTo(Instant.ofEpochSecond(1_700_000_000L, 123_000_000));
    }
}
