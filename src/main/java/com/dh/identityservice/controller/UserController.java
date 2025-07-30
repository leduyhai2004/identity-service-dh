package com.dh.identityservice.controller;

import java.util.List;

import com.dh.identityservice.dto.PageResponseDTO;
import com.dh.identityservice.service.UserRedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.dh.identityservice.dto.request.ApiResponse;
import com.dh.identityservice.dto.request.UserCreationRequest;
import com.dh.identityservice.dto.request.UserUpdateRequest;
import com.dh.identityservice.dto.response.UserResponse;
import com.dh.identityservice.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;
    UserRedisService userRedisService;

    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        var authentication  = SecurityContextHolder.getContext().getAuthentication();
        log.info("User {} is accessing user info for userId: {}", authentication.getName(), userId);
        authentication.getAuthorities().forEach(grantedAuthority
                -> log.info("Authority: {}", grantedAuthority.getAuthority()));

        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/my-info")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @DeleteMapping("/{userId}")
    ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponseDTO<UserResponse>> searchUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) throws JsonProcessingException {
        PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.by("id").ascending());
        List<UserResponse> userResponse = userRedisService.getAllUsers(keyword, pageRequest);

        PageResponseDTO<UserResponse> pageResponseDTO = new PageResponseDTO<>();

        if (userResponse!=null && !userResponse.isEmpty()) {
            log.info("Get users from redis");
             pageResponseDTO = PageResponseDTO.<UserResponse>builder()
                    .content(userResponse)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .totalElements(userResponse.size())
                    .totalPages((int) Math.ceil((double) userResponse.size() / pageSize))
                    .build();
        }
        if(userResponse == null){
            pageResponseDTO = userService.searchUsers(keyword, pageNo, pageSize, sortBy, sortDir);
            userRedisService.saveAllUsers(pageResponseDTO.getContent(),keyword,pageRequest);
            log.info("Get users from database");
        }
        return ApiResponse.<PageResponseDTO<UserResponse>>builder()
                .result(pageResponseDTO)
                .build();
    }
}
