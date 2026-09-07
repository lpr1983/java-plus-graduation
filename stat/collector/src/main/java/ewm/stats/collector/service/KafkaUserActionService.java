package ewm.stats.collector.service;

import ewm.stats.collector.exception.UserActionPublishingException;
import ewm.stats.collector.mapper.UserActionMapper;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class KafkaUserActionService implements UserActionService {
    private final Producer<Long, SpecificRecordBase> producer;
    private final String userActionsTopic;
    private final long sendTimeoutMs;

    public KafkaUserActionService(Producer<Long, SpecificRecordBase> producer,
                                  @Value("${kafka.topic.user-actions}") String userActionsTopic,
                                  @Value("${kafka.producer.send-timeout-ms}") long sendTimeoutMs) {
        this.producer = producer;
        this.userActionsTopic = userActionsTopic;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Override
    public void collect(UserActionProto action) {
        UserActionAvro avro = UserActionMapper.toAvro(action);
        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(userActionsTopic, avro.getUserId(), avro);

        try {
            producer.send(record).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UserActionPublishingException("User action publishing was interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new UserActionPublishingException("Failed to publish user action", exception);
        }
    }
}
