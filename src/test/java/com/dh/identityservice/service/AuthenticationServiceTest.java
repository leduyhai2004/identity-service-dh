package com.dh.identityservice.service;

import com.dh.identityservice.dto.request.AuthenticationRequest;
import com.dh.identityservice.dto.response.AuthenticationResponse;
import com.dh.identityservice.entity.User;
import com.dh.identityservice.exception.AppException;
import com.dh.identityservice.repository.InvalidatedTokenRepository;
import com.dh.identityservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class AuthenticationServiceTest {
    @Autowired
    private AuthenticationService authenticationService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private InvalidatedTokenRepository invalidatedTokenRepository;


    private AuthenticationRequest authenticationRequest;
    private AuthenticationResponse authenticationResponse;
    private User user;
    private LocalDate dob;


    @BeforeEach
    void initData() {
        dob = LocalDate.of(1990, 1, 1);

        authenticationRequest = AuthenticationRequest
                .builder()
                .username("john")
                .password("12345678")
                .build();
        authenticationResponse = AuthenticationResponse
                .builder()
                .authenticated(true)
                .token("hellotoken")
                .build();

        user = User.builder()
                .id(156443L)
                .username("john")
                .firstName("Haild")
                .lastName("Haild")
                .dob(dob)
                .build();
    }

    @Test
    void authenticate_success() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        String encodedPassword = new BCryptPasswordEncoder(10).encode("12345678");
        user.setPassword(encodedPassword);
        var response = authenticationService.authenticate(authenticationRequest);

        Assertions.assertThat(response.isAuthenticated()).isTrue();
        Assertions.assertThat(response.getToken()).isNotNull();
        verify(userRepository).findByUsername(anyString());
    }

    @Test
    void test_generate_token_success(){
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        String encodedPassword = new BCryptPasswordEncoder(10).encode("12345678");
        user.setPassword(encodedPassword);
        AuthenticationResponse response = authenticationService.authenticate(authenticationRequest);
        Assertions.assertThat(response.getToken()).isNotNull();

        String[] tokenParts = response.getToken().split("\\.");

        Assertions.assertThat(tokenParts).hasSize(3);
    }
    @Test
    void test_generate_token_fail(){
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        String encodedPassword = new BCryptPasswordEncoder(10).encode("12345w678");
        user.setPassword(encodedPassword);
        Assertions.assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> authenticationService.authenticate(authenticationRequest))
                .withMessage("Unauthenticated");
    }

    @Test
    void authenticate_fail() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        String encodedPassword = new BCryptPasswordEncoder(10).encode("12345688");
        user.setPassword(encodedPassword);
//        var error = org.junit.jupiter.api.Assertions.assertThrows(
//                AppException.class,
//                () -> authenticationService.authenticate(authenticationRequest)
//        );
        Assertions.assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> authenticationService.authenticate(authenticationRequest))
                .withMessage("Unauthenticated");
    }
}
