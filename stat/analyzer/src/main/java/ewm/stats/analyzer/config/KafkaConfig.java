package ewm.stats.analyzer.config;

import ewm.stats.analyzer.serialization.EventSimilarityDeserializer;
import ewm.stats.analyzer.serialization.UserActionDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;

@Configuration
public class KafkaConfig {

    @Bean
    public Consumer<Long, UserActionAvro> userActionConsumer(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.consumer.user-actions.client-id}") String clientId,
            @Value("${kafka.consumer.user-actions.group-id}") String groupId) {
        Properties properties = consumerProperties(bootstrapServers, clientId, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    @Bean
    public Consumer<String, EventSimilarityAvro> eventSimilarityConsumer(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${kafka.consumer.event-similarities.client-id}") String clientId,
            @Value("${kafka.consumer.event-similarities.group-id}") String groupId) {
        Properties properties = consumerProperties(bootstrapServers, clientId, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private Properties consumerProperties(String bootstrapServers, String clientId, String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }
}
