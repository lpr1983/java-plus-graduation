package ewm.requestservice.user;

import ewm.requestservice.dto.UserShortDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "user-service",
        path = "/internal/users",
        fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {

    @GetMapping("/{userId}")
    UserShortDto getUser(@PathVariable("userId") long userId);

    @GetMapping
    List<UserShortDto> getUsers(@RequestParam("ids") List<Long> ids);
}
