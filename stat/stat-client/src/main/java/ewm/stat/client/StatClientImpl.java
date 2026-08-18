package ewm.stat.client;

import ewm.stat.client.exception.StatClientException;
import ewm.stat.client.model.GetStatsParams;
import ewm.stat.client.model.HitParams;
import ewm.stat.dto.HitDto;
import ewm.stat.client.mapper.HitMapper;
import ewm.stat.dto.StatDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StatClientImpl implements StatClient {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String app;
    private final RestClient restClient;

    public StatClientImpl(String app, String baseUrl, int timeoutMs) {
        this.app = app;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (timeoutMs > 0) {
            requestFactory.setConnectTimeout(timeoutMs);
            requestFactory.setReadTimeout(timeoutMs);
        }

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void saveHit(HitParams params) {
        HitDto dto = HitMapper.fromHitParams(params, app);

        try {
            restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw toStatClientException(e);
        }
    }

    @Override
    public List<StatDto> getStats(GetStatsParams params) {
        String path = buildStatsPath(params);

        try {
            return restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StatDto>>() {
                    });
        } catch (RestClientException e) {
            throw toStatClientException(e);
        }
    }

    private StatClientException toStatClientException(RestClientException e) {
        if (e instanceof RestClientResponseException) {
            RestClientResponseException restClientResponseException = (RestClientResponseException) e;

            return new StatClientException(String.format("HTTP code: %s, body: %s",
                    restClientResponseException.getStatusCode(),
                    restClientResponseException.getResponseBodyAsString()), e);
        }

        return new StatClientException("Ошибка вызова сервиса статистики: " + e.getMessage(), e);
    }

    private String buildStatsPath(GetStatsParams params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats");
        LocalDateTime start = params.getStart();
        LocalDateTime end = params.getEnd();

        if (start != null) {
            builder.queryParam("start", start.format(DATE_TIME_FORMATTER));
        }
        if (end != null) {
            builder.queryParam("end", end.format(DATE_TIME_FORMATTER));
        }

        Boolean unique = params.getUnique();
        if (unique != null) {
            builder.queryParam("unique", unique);
        }

        List<String> uris = params.getUris();
        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", uris.toArray());
        }

        return builder.build().toUriString();
    }
}
