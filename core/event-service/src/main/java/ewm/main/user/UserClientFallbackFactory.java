package ewm.main.user;

import ewm.main.client.ClientFallbackExceptionMapper;
import ewm.main.dto.UserShortDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public UserShortDto getUser(long userId) {
                throw ClientFallbackExceptionMapper.map("user-service", cause);
            }

            @Override
            public List<UserShortDto> getUsers(List<Long> ids) {
                throw ClientFallbackExceptionMapper.map("user-service", cause);
            }
        };
    }
}
