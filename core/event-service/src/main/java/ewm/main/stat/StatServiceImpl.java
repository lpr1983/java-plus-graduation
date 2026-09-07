package ewm.main.stat;

import ewm.main.exception.ServiceUnavailableException;
import ewm.main.stat.dto.HitDto;
import ewm.main.stat.dto.StatDto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatServiceImpl implements StatService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatClient statClient;
    private final String applicationName;

    public StatServiceImpl(StatClient statClient,
                           @Value("${spring.application.name}") String applicationName) {
        this.statClient = statClient;
        this.applicationName = applicationName;
    }

    @Override
    public void saveHit(String uri, String ip) {
        HitDto hitDto = HitDto.builder()
                .app(applicationName)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            statClient.saveHit(hitDto);
        } catch (FeignException | ServiceUnavailableException exception) {
            log.error("Ошибка работы statClient.saveHit: {}", exception.getMessage());
        }
    }

    @Override
    public Map<String, Long> getViews(LocalDateTime start,
                                      LocalDateTime end,
                                      List<String> uris,
                                      boolean unique) {
        try {
            List<StatDto> statResult = statClient.getStats(
                    format(start),
                    format(end),
                    uris,
                    unique
            );
            return toViewsByUri(statResult);
        } catch (FeignException | ServiceUnavailableException exception) {
            log.error("Ошибка работы statClient.getStats: {}", exception.getMessage());
            return null;
        }
    }

    private Map<String, Long> toViewsByUri(List<StatDto> statResult) {
        Map<String, Long> viewsByUri = new HashMap<>();

        for (StatDto statDto : statResult) {
            viewsByUri.merge(statDto.getUri(), statDto.getHits(), Long::sum);
        }

        return viewsByUri;
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }
}
