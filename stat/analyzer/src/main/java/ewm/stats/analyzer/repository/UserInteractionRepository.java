package ewm.stats.analyzer.repository;

import ewm.stats.analyzer.model.UserInteraction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class UserInteractionRepository {
    private static final String INSERT_SQL = """
            INSERT INTO user_interactions (user_id, event_id, weight, interaction_time)
            VALUES (:userId, :eventId, :weight, :interactionTime)
            """;
    private static final String UPDATE_SQL = """
            UPDATE user_interactions
            SET weight = CASE WHEN weight < :weight THEN :weight ELSE weight END,
                interaction_time = CASE
                    WHEN interaction_time < :interactionTime THEN :interactionTime
                    ELSE interaction_time
                END
            WHERE user_id = :userId AND event_id = :eventId
            """;
    private static final String FIND_RECENT_SQL = """
            SELECT user_id, event_id, weight, interaction_time
            FROM user_interactions
            WHERE user_id = :userId
            ORDER BY interaction_time DESC, event_id
            LIMIT :limit
            """;
    private static final String FIND_BY_EVENT_IDS_SQL = """
            SELECT user_id, event_id, weight, interaction_time
            FROM user_interactions
            WHERE user_id = :userId AND event_id IN (:eventIds)
            """;
    private static final String SUM_WEIGHTS_SQL = """
            SELECT event_id, SUM(weight) AS weight_sum
            FROM user_interactions
            WHERE event_id IN (:eventIds)
            GROUP BY event_id
            """;
    private static final RowMapper<UserInteraction> ROW_MAPPER = new UserInteractionRowMapper();
    private static final RowMapper<Map.Entry<Long, Double>> WEIGHT_SUM_ROW_MAPPER = new WeightSumRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserInteractionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(UserInteraction interaction) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", interaction.getUserId())
                .addValue("eventId", interaction.getEventId())
                .addValue("weight", interaction.getWeight())
                .addValue("interactionTime", Timestamp.from(interaction.getTimestamp()));

        if (jdbcTemplate.update(UPDATE_SQL, parameters) == 0) {
            jdbcTemplate.update(INSERT_SQL, parameters);
        }
    }

    public List<UserInteraction> findRecentByUserId(long userId, int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("limit", limit);

        return jdbcTemplate.query(FIND_RECENT_SQL, parameters, ROW_MAPPER);
    }

    public List<UserInteraction> findByUserIdAndEventIds(long userId, Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return List.of();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("eventIds", eventIds);

        return jdbcTemplate.query(FIND_BY_EVENT_IDS_SQL, parameters, ROW_MAPPER);
    }

    public Map<Long, Double> sumWeightsByEventIds(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource("eventIds", eventIds);
        Map<Long, Double> weightSums = new HashMap<>();
        List<Map.Entry<Long, Double>> rows = jdbcTemplate.query(SUM_WEIGHTS_SQL, parameters, WEIGHT_SUM_ROW_MAPPER);

        for (Map.Entry<Long, Double> row : rows) {
            weightSums.put(row.getKey(), row.getValue());
        }

        return weightSums;
    }

    private static class UserInteractionRowMapper implements RowMapper<UserInteraction> {

        @Override
        public UserInteraction mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
            return new UserInteraction(
                    resultSet.getLong("user_id"),
                    resultSet.getLong("event_id"),
                    resultSet.getDouble("weight"),
                    resultSet.getTimestamp("interaction_time").toInstant()
            );
        }
    }

    private static class WeightSumRowMapper implements RowMapper<Map.Entry<Long, Double>> {

        @Override
        public Map.Entry<Long, Double> mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
            return Map.entry(resultSet.getLong("event_id"), resultSet.getDouble("weight_sum"));
        }
    }
}
