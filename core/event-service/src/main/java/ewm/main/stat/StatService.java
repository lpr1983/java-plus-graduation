package ewm.main.stat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StatService {

    void saveHit(String uri, String ip);

    Map<String, Long> getViews(LocalDateTime start,
                               LocalDateTime end,
                               List<String> uris,
                               boolean unique);
}
