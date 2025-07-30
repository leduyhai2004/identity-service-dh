package com.dh.identityservice.service;

import com.dh.identityservice.dto.response.UserResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    @Value("${spring.data.redis.use-redis-cache}")
    private boolean useRedisCache;

    private String getKeyFrom(String keyword,
                              PageRequest pageRequest) {
        int pageNumber = pageRequest.getPageNumber();
        int pageSize = pageRequest.getPageSize();
        Sort sort = pageRequest.getSort();
        String sortDirection = sort.getOrderFor("id")
                .getDirection() == Sort.Direction.ASC ? "asc": "desc";
        String key = String.format("all_users:%s:%d:%d:%s",
                keyword, pageNumber, pageSize, sortDirection);
        return key;
    }

    public List<UserResponse> getAllUsers(String keyword,
                                             PageRequest pageRequest) throws JsonProcessingException {
        if (!useRedisCache) {
            log.debug("Redis cache is disabled, returning null");
            return null;
        }


            String key = this.getKeyFrom(keyword, pageRequest);
            String json = (String) redisTemplate.opsForValue().get(key);
            List<UserResponse> userResponses =
                    json != null ?
                            redisObjectMapper.readValue(json, new TypeReference<List<UserResponse>>() {})
                            : null;
            return userResponses;
    }

    public void clear(){
        if (!useRedisCache) {
            log.debug("Redis cache is disabled, skipping clear operation");
            return;
        }

        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            log.warn("Failed to clear Redis cache: {}", e.getMessage());
        }
    }

    //save to Redis
    public void saveAllUsers(List<UserResponse> userResponses,
                                String keyword,
                                PageRequest pageRequest) throws JsonProcessingException {
        if (!useRedisCache) {
            log.debug("Redis cache is disabled, skipping save operation");
            return;
        }
            String key = this.getKeyFrom(keyword, pageRequest);
            String json = redisObjectMapper.writeValueAsString(userResponses);
            redisTemplate.opsForValue().set(key, json);
    }
}
