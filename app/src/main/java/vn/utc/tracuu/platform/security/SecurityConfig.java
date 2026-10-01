package vn.utc.tracuu.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.web.ApiError;
import vn.utc.tracuu.platform.web.ErrorCode;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final TracuuProperties props;
    private final ObjectMapper json;

    public SecurityConfig(TracuuProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(props.security().bcryptStrength());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Giao diện và máy chủ dựng cùng một ảnh nên cookie phiên là cách xác thực đúng ở đây;
        // không có bên thứ ba nào gọi vào, và không có phiên bản hoá giao diện lập trình
        // (hợp đồng Mục 9) — thêm tầng thẻ mang là giải một bài toán không tồn tại.
        CsrfTokenRequestAttributeHandler csrf = new CsrfTokenRequestAttributeHandler();
        csrf.setCsrfRequestAttributeName(null);

        http
            .csrf(c -> c
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrf)
                // Bộ đo chạy bằng LỆNH, không qua đường mạng (hợp đồng Mục 8) — nên không có
                // ngoại lệ nào ở đây cho nó.
                .ignoringRequestMatchers(new AntPathRequestMatcher("/api/auth/login", "POST")))

            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(f -> f.changeSessionId())
                // NFR-system-010: 50 phiên mở đồng thời.
                .maximumSessions(3))

            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // Tệp tĩnh của giao diện: app phục vụ chúng, giao diện không có tiến trình riêng.
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/*.svg").permitAll()
                // Khu quản trị: quyền trên CHỨC NĂNG, lộ sự tồn tại ở đây là vô hại.
                .requestMatchers("/api/admin/**").hasRole("QUAN_TRI")
                .anyRequest().authenticated())

            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(res, ErrorCode.CHUA_DANG_NHAP))
                .accessDeniedHandler((req, res, ex) -> write(res, ErrorCode.KHONG_DU_QUYEN)))

            .logout(l -> l
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout", "POST"))
                .invalidateHttpSession(true)
                .deleteCookies("SESSION", "XSRF-TOKEN")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));

        return http.build();
    }

    /** Lỗi xác thực cũng phải mang đúng hình dạng chung của mọi lỗi. */
    private void write(jakarta.servlet.http.HttpServletResponse res, ErrorCode code)
            throws java.io.IOException {
        res.setStatus(code.status().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getWriter(), ApiError.of(code));
    }
}
