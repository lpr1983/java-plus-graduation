package ewm.stats.collector.mapper;

import com.google.protobuf.Timestamp;
import ewm.stats.collector.exception.UnknownActionTypeException;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

public final class UserActionMapper {

    private UserActionMapper() {
    }

    public static UserActionAvro toAvro(UserActionProto action) {
        Timestamp timestamp = action.getTimestamp();

        return UserActionAvro.newBuilder()
                .setUserId(action.getUserId())
                .setEventId(action.getEventId())
                .setActionType(toAvro(action.getActionType()))
                .setTimestamp(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()))
                .build();
    }

    private static ActionTypeAvro toAvro(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new UnknownActionTypeException(actionType);
        };
    }
}
