package ewm.main.stat;

import ewm.main.client.ClientFallbackExceptionMapper;
import ewm.main.stat.dto.HitDto;
import ewm.main.stat.dto.StatDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StatClientFallbackFactory implements FallbackFactory<StatClient> {

    @Override
    public StatClient create(Throwable cause) {
        return new StatClient() {
            @Override
            public void saveHit(HitDto hitDto) {
                throw ClientFallbackExceptionMapper.map("stats-server", cause);
            }

            @Override
            public List<StatDto> getStats(
                    String start,
                    String end,
                    List<String> uris,
                    Boolean unique) {
                throw ClientFallbackExceptionMapper.map("stats-server", cause);
            }
        };
    }
}
