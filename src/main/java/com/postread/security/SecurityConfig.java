package com.postread.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private UserService userService;
    private TokenFilter tokenFilter;

    public SecurityConfig() {
    }

    @Autowired
    public void setTokenFilter(TokenFilter tokenFilter) {
        this.tokenFilter = tokenFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Статические ресурсы - публичные
                        .requestMatchers(
                                "/",
                                "/home",
                                "/index",
                                "/favicon.ico",
                                "/error",
                                "/uploads/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/static/**"
                        ).permitAll()

                        // Аутентификация - публичные
                        .requestMatchers(
                                "/auth/**",
                                "/login",
                                "/register",
                                "/signup"
                        ).permitAll()

                        // Статьи - публичные (чтение)
                        .requestMatchers(
                                "/articles",
                                "/articles/**",
                                "/articles/search",
                                "/articles/search-advanced",
                                "/articles/api/**"
                        ).permitAll()

                        // API endpoints - детальная настройка
                        .requestMatchers(
                                "/api/articles/**",
                                "/api/tags/**",
                                "/api/comments/**",
                                "/api/search/**",
                                "/api/upload/**"
                        ).permitAll()

                        // Bookmark status check - публичный
                        .requestMatchers("/bookmarks/status/**").permitAll()

                        // Bookmark management - требует аутентификации
                        .requestMatchers("/bookmarks/**").authenticated()

                        // Reactions - требуют аутентификации
                        .requestMatchers("/api/reactions/**").authenticated()

                        // Создание и редактирование статей - требует аутентификации
                        .requestMatchers(
                                "/articles/editor/**",
                                "/articles/create/**",
                                "/articles/edit/**",
                                "/articles/delete/**",
                                "/articles/publish/**",
                                "/articles/unpublish/**"
                        ).authenticated()

                        // Пользовательские endpoints - требуют аутентификации
                        .requestMatchers(
                                "/user/**",
                                "/profile/**",
                                "/my/**"
                        ).authenticated()

                        // Административные endpoints
                        .requestMatchers(
                                "/admin/**",
                                "/management/**"
                        ).hasRole("ADMIN")

                        // Все остальные запросы - публичные
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/articles", true)
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecret")
                        .tokenValiditySeconds(86400) // 24 hours
                        .rememberMeParameter("remember-me")
                )
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}