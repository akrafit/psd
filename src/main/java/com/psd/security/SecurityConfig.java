package com.psd.security;

import com.psd.repo.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return new CustomUserDetailsService(userRepository);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider
    ) throws Exception {

        http
                .authenticationProvider(authenticationProvider)

                // CSRF оставляем для обычных страниц,
                // но отключаем для OnlyOffice callback/internal/api,
                // потому что они ходят не как обычная html-форма.
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(authz -> authz
                        // Страница входа и тестовые публичные ручки
                        .requestMatchers("/", "/login", "/test/**").permitAll()

                        // Статика + error
                        .requestMatchers(
                                "/error",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/webjars/**",
                                "/images/**"
                        ).permitAll()

                        // OnlyOffice
                        .requestMatchers("/onlyoffice/config/**", "/onlyoffice/callback/**").permitAll()
                        .requestMatchers("/api/files/**").permitAll()

                        // Внутренние ручки
                        .requestMatchers("/internal/**").permitAll()

                        // Админка
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Остальное — только после входа
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/projects", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                // Для OnlyOffice иногда важно не запрещать iframe.
                // Если редактор и портал на одном origin — достаточно sameOrigin.
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}