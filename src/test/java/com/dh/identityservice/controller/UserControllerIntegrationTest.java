package com.dh.identityservice.controller;

import com.dh.identityservice.dto.request.UserCreationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.time.LocalDate;

//@Slf4j
//@SpringBootTest
//@AutoConfigureMockMvc
//@Testcontainers


@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(locations = "classpath:test.properties")
class UserControllerIntegrationTest {

//    @Container
//    static final MySQLContainer<?> mySQLContainer = (MySQLContainer<?>) new MySQLContainer("mysql:8.0")
//            .withDatabaseName("testdb")
//            .withUsername("testuser")
//            .withPassword("testpass")
//            .withEnv("MYSQL_ROOT_PASSWORD", "rootpass")
//            .withStartupTimeout(Duration.ofMinutes(3))
//            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 1));
//    //==>Sau khi chạy dòng này, Testcontainers sẽ dùng Docker để khởi động 1 container MySQL thật trong nền.
//
//    @DynamicPropertySource
//    static void configureDataSource(DynamicPropertyRegistry registry){
//        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
//        registry.add("spring.datasource.username", mySQLContainer::getUsername);
//        registry.add("spring.datasource.password", mySQLContainer::getPassword);
//        registry.add("spring.datasource.driverClassName", () -> "com.mysql.cj.jdbc.Driver");
//        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
//        registry.add("spring.jpa.show-sql", () -> "true");
//    }
//    // ==> Gán cấu hình container vào Spring Boot bằng @DynamicPropertySource


    @Autowired
    private MockMvc mockMvc;

    private UserCreationRequest request;

    @BeforeEach
    void initData() {
        LocalDate dob = LocalDate.of(1990, 1, 1);

        request = UserCreationRequest.builder()
                .username("john3")
                .firstName("John3")
                .lastName("Doe3")
                .password("12345678")
                .dob(dob)
                .build();
    }

    @Test
    void createUser_validRequest_success() throws Exception {
        // GIVEN
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String content = objectMapper.writeValueAsString(request);

        // WHEN, THEN
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("result.username").value("john3"));
        log.info("result", response.andReturn().getResponse().getContentAsString() );
    }
}
