package ewm.main.stat;

import ewm.main.stat.dto.HitDto;
import ewm.main.stat.dto.StatDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "${stat-service.name:stats-server}")
public interface StatClient {

    @PostMapping("/hit")
    void saveHit(@RequestBody HitDto hitDto);

    @GetMapping("/stats")
    List<StatDto> getStats(@RequestParam("start") String start,
                            @RequestParam("end") String end,
                            @RequestParam(value = "uris", required = false) List<String> uris,
                            @RequestParam(value = "unique", required = false) Boolean unique);
}
