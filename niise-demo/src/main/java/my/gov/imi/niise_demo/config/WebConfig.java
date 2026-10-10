package my.gov.imi.niise_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/**") // Applies configuration to all api endpoints
                        .allowedOriginPatterns("*") // Safely allows both http://localhost and file:// (Origin: null)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Explicitly allow all CRUD operations
                        .allowedHeaders("*") // Allows Authorization, Content-Type, etc.
                        .exposedHeaders("Authorization") // Allows frontend JS to read your Bearer token if needed
                        .allowCredentials(true); // Needed if you handle session cookies alongside tokens
            }
        };
    }
}
