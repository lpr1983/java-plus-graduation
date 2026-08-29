package ewm.userservice.mapper;

import ewm.userservice.dto.UserShortDto;
import ewm.userservice.entity.User;

public class UserMapper {
    public static UserShortDto toUserShortDto(User user) {
        return new UserShortDto(user.getId(), user.getEmail());
    }
}
