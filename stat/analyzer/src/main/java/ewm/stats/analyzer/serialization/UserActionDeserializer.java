package ewm.stats.analyzer.serialization;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.IOException;

public class UserActionDeserializer implements Deserializer<UserActionAvro> {

    @Override
    public UserActionAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.getClassSchema());
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (IOException exception) {
            throw new SerializationException("Failed to deserialize user action from topic " + topic, exception);
        }
    }
}
