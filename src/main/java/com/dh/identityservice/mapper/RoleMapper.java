package com.dh.identityservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.dh.identityservice.dto.request.RoleRequest;
import com.dh.identityservice.dto.response.RoleResponse;
import com.dh.identityservice.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
