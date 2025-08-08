package com.dh.identityservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.dh.identityservice.dto.request.UserUpdateRequest;
import com.dh.identityservice.repository.RoleRepository;
import org.assertj.core.api.Assertions;
import org.h2.util.MathUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import com.dh.identityservice.dto.request.UserCreationRequest;
import com.dh.identityservice.dto.response.UserResponse;
import com.dh.identityservice.entity.User;
import com.dh.identityservice.exception.AppException;
import com.dh.identityservice.repository.UserRepository;

@SpringBootTest
@TestPropertySource("/test.properties")
public class UserServiceTest {
    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    private UserCreationRequest request;
    private UserUpdateRequest updateRequest;
    private UserResponse userResponse;
    private User user;
    private LocalDate dob;

    @BeforeEach
    void initData() {
        dob = LocalDate.of(1990, 1, 1);

        request = UserCreationRequest.builder()
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .password("12345678")
                .dob(dob)
                .build();

        updateRequest = UserUpdateRequest.builder()
                .password("1213212")
                .firstName("Haild")
                .lastName("Haild")
                .build();

        userResponse = UserResponse.builder()
                .id(156443L)
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .dob(dob)
                .build();

        user = User.builder()
                .id(156443L)
                .username("Haild")
                .firstName("Haild")
                .lastName("Haild")
                .dob(dob)
                .build();
    }

    @Test
    void createUser_validRequest_success() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        //set role user to test
        when(roleRepository.findById(anyString())).thenReturn(Optional.empty());

        when(userRepository.save(any())).thenReturn(user);


        // WHEN
        var response = userService.createUser(request);
        // THEN

        Assertions.assertThat(response.getId()).isEqualTo(156443L);
        Assertions.assertThat(response.getUsername()).isEqualTo("Haild");
    }

    @Test
    void createUser_userExisted_fail() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));

//        MockedStatic<MathUtils> mockedStatic = Mockito.mockStatic(MathUtils.class);
//        mockedStatic.when(() -> MathUtils.randomInt(10)).thenReturn(2);

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }

    //org.springframework.security
    @Test
    @WithMockUser(username = "john")
    void getMyInfo_valid_success() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        var response = userService.getMyInfo();

        Assertions.assertThat(response.getUsername()).isEqualTo("Haild");
        Assertions.assertThat(response.getId()).isEqualTo(156443L);
    }

    //org.springframework.security
    @Test
    @WithMockUser(username = "john")
    void getMyInfo_userNotFound_error() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.ofNullable(null));

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.getMyInfo());

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1005);
    }

    @Test
    @WithMockUser(username = "Haild")
    void updateUser_validRequest_success() {
        // GIVEN
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(roleRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(user);

        // WHEN
        when(roleRepository.findAllById(any())).thenReturn(List.of());
        var response = userService.updateUser(156443L,updateRequest);

        // THEN
        Assertions.assertThat(response.getId()).isEqualTo(156443L);
        Assertions.assertThat(response.getUsername()).isEqualTo("Haild");

    }

    @Test
    void updateUser_userNotFound_fail() {
        // GIVEN
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.updateUser(156443L,updateRequest));
        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1005);
    }
    @Test
    @WithMockUser(username = "Haild")
    void updateUser_notAuthorized_fail(){
        User otherUser = User.builder()
                .id(156443L)
                .username("otherUser")
                .firstName("otherUser")
                .lastName("otherUser")
                .dob(dob)
                .build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(otherUser));
        when(roleRepository.findAllById(any())).thenReturn(List.of());
        when(userRepository.save(any())).thenReturn(otherUser);
        // WHEN & THEN
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> userService.updateUser(999L, updateRequest));
    }

    @Test
    void checkPowerMockito(){
        try (MockedStatic<MathUtils> mockedStatic = Mockito.mockStatic(MathUtils.class)) {
            mockedStatic.when(() -> MathUtils.randomInt(5)).thenReturn(2);

            int result = MathUtils.randomInt(5);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            org.junit.jupiter.api.Assertions.assertEquals(2,result);
            Assertions.assertThat(result).isEqualTo(2);
        }

        //test with role
//        @Test
//        @WithMockUser(username = "user", roles = {"USER"}) // Người dùng có role không phù hợp
//        void getUsers_userWithoutAdminRole_accessDenied() {
//            // WHEN & THEN
//            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> userService.getUsers());
//        }

    }
}
