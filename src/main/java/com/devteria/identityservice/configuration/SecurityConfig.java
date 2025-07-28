package com.devteria.identityservice.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // phải có cái này để sử dụng @PreAuthorize, @PostAuthorize, @Secured, @RolesAllowed
public class SecurityConfig {
    private final String[] PUBLIC_ENDPOINTS = {
        "/users", "/auth/token", "/auth/introspect", "/auth/logout", "/auth/refresh"
    };

    @Autowired
    private CustomJwtDecoder customJwtDecoder;

    //các request của client đầu tiên sẽ được gửi đến đây
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request
                -> request.requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS)
                .permitAll()

                //C1 : để phân quyền thì
                //mặc định jwt authentication manager sẽ map các role thành SCOPE_* để Spring Security có thể sử dụng
                //.requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINTS).hasAuthority("SCOPE_ADMIN")
                //trong jwt thì token sẽ có dạng "scope": "ADMIN USER" (nếu có 2 role là ADMIN và USER)

                //C2 : để phân quyền thì
                //Ta có thể sử dụng jwtAuthenticationConverter để convert prefix SCOPE_ thành ROLE_
                //.requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINTS).hasAuthority("ROLE_ADMIN")
                // hoặc hasRole("ADMIN") : vì nó sẽ tự động tìm trong các authorities có prefix là ROLE_ADMIN
                //trong jwt thì token sẽ có dạng "scope": "ADMIN USER" (nếu có 2 role là ADMIN và USER)

                .anyRequest()
                .authenticated());

        //để xác thực token, nếu token hợp lệ thì sẽ được chuyển đến các controller
        //để xử lý các request tiếp theo
        //nếu token không hợp lệ thì sẽ trả về lỗi 401 Unauthorized
        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(customJwtDecoder)

                //jwtAuthenticationConverter là gì?
                //để chuyển đổi JWT thành Authentication object
                //để có thể sử dụng các phương thức của Authentication object
                //ví dụ như getAuthorities() để lấy các quyền của người dùng
                //để có thể sử dụng các quyền này trong các phương thức của controller
                //ở đây sử dụng để chỉnh lại tên prefix của quyền từ mặc định là SCOPE_ thành tên ta muốn
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

        //CSRF là một kiểu tấn công mà kẻ xấu lợi dụng sự tin tưởng của trình duyệt
        // và trang web đích. Kẻ xấu lừa trình duyệt của bạn (hoặc bạn) gửi một yêu cầu
        // không mong muốn đến một trang web mà bạn đã đăng nhập và đang có phiên làm việc hợp lệ.
        // Trang web đích sau đó sẽ thực hiện hành động trái phép đó, tin rằng yêu cầu này là từ bạn
        // vì nó đi kèm với "chứng minh thư" (cookie) của bạn.
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsFilter(urlBasedCorsConfigurationSource);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        //Dùng để trích xuất quyền (roles/authorities) từ JWT claims, thường là từ scope hoặc authorities.
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        //Mặc định Spring thêm "ROLE_" vào trước mỗi quyền, như ROLE_ADMIN.
        // bỏ "ROLE_" để giữ nguyên (ví dụ: "ADMIN" hoặc "user.read"),
        // tránh bug khi sử dụng @PreAuthorize("hasAuthority('ADMIN')").
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        //đây là converter thực sự dùng để tạo ra Authentication object mà Spring Security sử dụng.
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }


        //Customize prefix lại SCOPE thanh ROLE
        //c2
//    @Bean
//    JwtAuthenticationConverter jwtAuthenticationConverter() {
//        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
//        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
//        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
//        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
//
//        return jwtAuthenticationConverter;
//    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
