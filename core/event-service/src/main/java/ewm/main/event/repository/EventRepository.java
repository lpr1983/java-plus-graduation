package ewm.main.event.repository;

import ewm.main.event.model.Event;
import ewm.main.event.model.EventState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    @EntityGraph(attributePaths = {"category"})
    Page<Event> findAll(Specification<Event> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    List<Event> findByInitiatorIdOrderByEventDateAsc(long userId, Pageable pageable);

    Optional<Event> findOneByInitiatorIdAndId(long userId, long eventId);

    Optional<Event> findOneByIdAndState(long id, EventState state);

    boolean existsByCategory_Id(long categoryId);
}
