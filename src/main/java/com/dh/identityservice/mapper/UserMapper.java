package com.dh.identityservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.dh.identityservice.dto.request.UserCreationRequest;
import com.dh.identityservice.dto.request.UserUpdateRequest;
import com.dh.identityservice.dto.response.UserResponse;
import com.dh.identityservice.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
