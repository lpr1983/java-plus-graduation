package ewm.stat.server.storage;

import ewm.stat.server.dto.HitDto;
import ewm.stat.server.dto.StatDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatStorage {

    void save(HitDto dto);

    List<StatDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique, List<String> uris);

}
