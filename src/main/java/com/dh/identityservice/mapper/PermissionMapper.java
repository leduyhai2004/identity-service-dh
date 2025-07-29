package com.dh.identityservice.mapper;

import org.mapstruct.Mapper;

import com.dh.identityservice.dto.request.PermissionRequest;
import com.dh.identityservice.dto.response.PermissionResponse;
import com.dh.identityservice.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
