package my.gov.imi.niise_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    /**
     * Initializes the isolated BCrypt Password Hashing Engine.
     * Keeps security logic separated from HTTP filters to prevent circular loops.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // REMOVED the duplicate modelMapper() bean declaration block.
    // Spring Boot will now seamlessly pick up your original bean from ModelMapperConfig.java instead!
}
