package ewm.stats.analyzer.repository;

import ewm.stats.analyzer.model.EventSimilarity;
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
    private static final String FIND_NOT_INTERACTED_BY_EVENT_ID_SQL = """
            SELECT event_a, event_b, score, calculation_time
            FROM event_similarities es
            WHERE (event_a = :eventId OR event_b = :eventId)
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_interactions ui
                  WHERE ui.user_id = :userId
                    AND ui.event_id = CASE WHEN es.event_a = :eventId THEN es.event_b ELSE es.event_a END
              )
            ORDER BY score DESC, event_a, event_b
            LIMIT :limit
            """;
    private static final String FIND_RECOMMENDATION_CANDIDATES_SQL = """
            WITH candidates AS (
                SELECT event_b AS event_id, score
                FROM event_similarities
                WHERE event_a IN (:eventIds) AND event_b NOT IN (:eventIds)
                UNION ALL
                SELECT event_a AS event_id, score
                FROM event_similarities
                WHERE event_b IN (:eventIds) AND event_a NOT IN (:eventIds)
            )
            SELECT candidate.event_id
            FROM candidates candidate
            WHERE NOT EXISTS (
                SELECT 1
                FROM user_interactions ui
                WHERE ui.user_id = :userId AND ui.event_id = candidate.event_id
            )
            GROUP BY candidate.event_id
            ORDER BY MAX(candidate.score) DESC, candidate.event_id
            LIMIT :limit
            """;
    private static final String FIND_INTERACTED_BY_EVENT_ID_SQL = """
            SELECT event_a, event_b, score, calculation_time
            FROM event_similarities es
            WHERE (event_a = :eventId OR event_b = :eventId)
              AND EXISTS (
                  SELECT 1
                  FROM user_interactions ui
                  WHERE ui.user_id = :userId
                    AND ui.event_id = CASE WHEN es.event_a = :eventId THEN es.event_b ELSE es.event_a END
              )
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
            jdbcTemplate.update(INSERT_SQL, parameters);
        }
    }

    public List<EventSimilarity> findSimilaritiesForEventNotInteractedByUser(long eventId, long userId, int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("userId", userId)
                .addValue("limit", limit);

        return jdbcTemplate.query(FIND_NOT_INTERACTED_BY_EVENT_ID_SQL, parameters, ROW_MAPPER);
    }

    public List<Long> findRecommendationCandidateIds(Collection<Long> eventIds, long userId, int limit) {
        if (eventIds.isEmpty()) {
            return List.of();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventIds", eventIds)
                .addValue("userId", userId)
                .addValue("limit", limit);

        return jdbcTemplate.queryForList(FIND_RECOMMENDATION_CANDIDATES_SQL, parameters, Long.class);
    }

    public List<EventSimilarity> findSimilaritiesForEventInteractedByUser(long eventId, long userId, int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("userId", userId)
                .addValue("limit", limit);

        return jdbcTemplate.query(FIND_INTERACTED_BY_EVENT_ID_SQL, parameters, ROW_MAPPER);
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
