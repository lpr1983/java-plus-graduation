package ewm.userservice.controller;

import ewm.userservice.dto.UserShortDto;
import ewm.userservice.mapper.UserMapper;
import ewm.userservice.service.UserService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public UserShortDto getUser(@PathVariable @Positive Long userId) {
        return UserMapper.toUserShortDto(userService.findUserById(userId));
    }

    @GetMapping
    public List<UserShortDto> getUsers(@RequestParam List<@Positive Long> ids) {
        return userService.findUsersByIds(ids).stream()
                .map(UserMapper::toUserShortDto)
                .toList();
    }
}
