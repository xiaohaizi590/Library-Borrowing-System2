package net.togogo.mapper;

import net.togogo.dto.UserDTO;
import net.togogo.entity.User;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        if (user == null) return null;
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhone())
                .createTime(user.getCreateTime())
                .role(user.getRole())
                .build();
    }
}
