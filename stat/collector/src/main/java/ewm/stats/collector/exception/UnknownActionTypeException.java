package ewm.stats.collector.exception;

import ru.practicum.ewm.stats.proto.ActionTypeProto;

public class UnknownActionTypeException extends IllegalArgumentException {

    public UnknownActionTypeException(ActionTypeProto actionType) {
        super("Unsupported user action type: " + actionType);
    }
}
