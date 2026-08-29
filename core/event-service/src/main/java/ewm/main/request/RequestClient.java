package ewm.main.request;

import ewm.main.dto.ConfirmedRequestsCountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestClient {

    @GetMapping("/confirmed-counts")
    List<ConfirmedRequestsCountDto> getConfirmedRequestsCounts(
            @RequestParam("eventIds") List<Long> eventIds);
}
