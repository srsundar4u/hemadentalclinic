package com.dentalclinic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(access -> access
                .requestMatchers("/login", "/css/**", "/error").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login")
                .defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService staffAccounts(PasswordEncoder encoder,
            @Value("${CLINIC_DENTIST_PASSWORD}") String dentistPassword,
            @Value("${CLINIC_RECEPTIONIST_PASSWORD}") String receptionistPassword) {
        if (dentistPassword.isBlank() || receptionistPassword.isBlank()) {
            throw new IllegalArgumentException("Both staff account passwords must be configured.");
        }
        return new InMemoryUserDetailsManager(
            User.withUsername("dentist").password(encoder.encode(dentistPassword))
                .roles("DENTIST").build(),
            User.withUsername("receptionist").password(encoder.encode(receptionistPassword))
                .roles("RECEPTIONIST").build());
    }
}
