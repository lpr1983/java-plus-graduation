package ewm.stats.collector.service;

import ru.practicum.ewm.stats.proto.UserActionProto;

public interface UserActionService {

    void collect(UserActionProto action);
}
