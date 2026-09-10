package ewm.stats.analyzer.repository;

import ewm.stats.analyzer.model.EventSimilarity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Repository
public class EventSimilarityRepository {
    private static final String INSERT_SQL = """
            INSERT INTO event_similarities (event_a, event_b, score, calculation_time)
            VALUES (:eventA, :eventB, :score, :calculationTime)
            """;
    private static final String UPDATE_SQL = """
            UPDATE event_similarities
            SET score = CASE WHEN calculation_time <= :calculationTime THEN :score ELSE score END,
                calculation_time = CASE
                    WHEN calculation_time <= :calculationTime THEN :calculationTime
                    ELSE calculation_time
                END
            WHERE event_a = :eventA AND event_b = :eventB
            """;
    private static final String FIND_BY_EVENT_ID_SQL = """
            SELECT event_a, event_b, score, calculation_time
            FROM event_similarities
            WHERE event_a = :eventId OR event_b = :eventId
            ORDER BY score DESC, event_a, event_b
            LIMIT :limit
            """;
    private static final String FIND_BY_EVENT_IDS_SQL = """
            SELECT event_a, event_b, score, calculation_time
            FROM event_similarities
            WHERE event_a IN (:eventIds) OR event_b IN (:eventIds)
            ORDER BY score DESC, event_a, event_b
            LIMIT :limit
            """;
    private static final RowMapper<EventSimilarity> ROW_MAPPER = new EventSimilarityRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EventSimilarityRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(EventSimilarity similarity) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventA", similarity.getEventA())
                .addValue("eventB", similarity.getEventB())
                .addValue("score", similarity.getScore())
                .addValue("calculationTime", Timestamp.from(similarity.getTimestamp()));
        if (jdbcTemplate.update(UPDATE_SQL, parameters) == 0) {
            insertOrRetryUpdate(parameters);
        }
    }

    public List<EventSimilarity> findByEventId(long eventId, int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("limit", limit);
        return jdbcTemplate.query(FIND_BY_EVENT_ID_SQL, parameters, ROW_MAPPER);
    }

    public List<EventSimilarity> findByEventIds(Collection<Long> eventIds, int limit) {
        if (eventIds.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventIds", eventIds)
                .addValue("limit", limit);
        return jdbcTemplate.query(FIND_BY_EVENT_IDS_SQL, parameters, ROW_MAPPER);
    }

    private void insertOrRetryUpdate(MapSqlParameterSource parameters) {
        try {
            jdbcTemplate.update(INSERT_SQL, parameters);
        } catch (DuplicateKeyException exception) {
            jdbcTemplate.update(UPDATE_SQL, parameters);
        }
    }

    private static class EventSimilarityRowMapper implements RowMapper<EventSimilarity> {

        @Override
        public EventSimilarity mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
            return EventSimilarity.of(
                    resultSet.getLong("event_a"),
                    resultSet.getLong("event_b"),
                    resultSet.getDouble("score"),
                    resultSet.getTimestamp("calculation_time").toInstant()
            );
        }
    }
}
