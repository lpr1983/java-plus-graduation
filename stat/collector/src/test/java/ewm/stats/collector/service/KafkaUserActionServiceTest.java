package ewm.stats.collector.service;

import com.google.protobuf.Timestamp;
import ewm.stats.collector.exception.UserActionPublishingException;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaUserActionServiceTest {
    private static final String TOPIC = "stats.user-actions.v1";

    @Mock
    private Producer<Long, SpecificRecordBase> producer;

    @Test
    void shouldWaitForKafkaAndPublishMappedAction() {
        when(producer.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        KafkaUserActionService service = new KafkaUserActionService(producer, TOPIC, 1000L);

        service.collect(action());

        ArgumentCaptor<ProducerRecord<Long, SpecificRecordBase>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(captor.capture());
        ProducerRecord<Long, SpecificRecordBase> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(TOPIC);
        assertThat(record.key()).isEqualTo(12L);
        assertThat(record.value()).isInstanceOf(UserActionAvro.class);
    }

    @Test
    void shouldWrapKafkaFailure() {
        Future<RecordMetadata> failed = CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable"));
        when(producer.send(any())).thenReturn(failed);
        KafkaUserActionService service = new KafkaUserActionService(producer, TOPIC, 1000L);

        assertThatThrownBy(() -> service.collect(action()))
                .isInstanceOf(UserActionPublishingException.class)
                .hasCauseInstanceOf(ExecutionException.class);
    }

    private UserActionProto action() {
        return UserActionProto.newBuilder()
                .setUserId(12L)
                .setEventId(34L)
                .setActionType(ActionTypeProto.ACTION_VIEW)
                .setTimestamp(Timestamp.newBuilder().setSeconds(1_700_000_000L).build())
                .build();
    }
}
