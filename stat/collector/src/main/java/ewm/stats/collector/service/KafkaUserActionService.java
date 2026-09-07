package ewm.stats.collector.service;

import ewm.stats.collector.mapper.UserActionMapper;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Service
public class KafkaUserActionService implements UserActionService {
    private final Producer<Long, SpecificRecordBase> producer;
    private final String userActionsTopic;

    public KafkaUserActionService(Producer<Long, SpecificRecordBase> producer,
                                  @Value("${kafka.topic.user-actions}") String userActionsTopic) {
        this.producer = producer;
        this.userActionsTopic = userActionsTopic;
    }

    @Override
    public void collect(UserActionProto action) {
        UserActionAvro avro = UserActionMapper.toAvro(action);
        producer.send(new ProducerRecord<>(userActionsTopic, avro.getUserId(), avro));
    }
}
