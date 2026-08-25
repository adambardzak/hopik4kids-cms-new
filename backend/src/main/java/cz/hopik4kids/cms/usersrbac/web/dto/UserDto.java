package cz.hopik4kids.cms.usersrbac.web.dto;

import cz.hopik4kids.cms.usersrbac.domain.User;

import java.time.Instant;

public record UserDto(
        String id,
        String name,
        String email,
        String role,
        String status,
        String phone,
        String color,
        Instant lastLoginAt
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole().name().toLowerCase(),
                u.getStatus().name().toLowerCase(),
                u.getPhone(),
                u.getColor(),
                u.getLastLoginAt()
        );
    }
}
