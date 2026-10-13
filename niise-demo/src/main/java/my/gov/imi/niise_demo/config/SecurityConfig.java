package my.gov.imi.niise_demo.config;

import my.gov.imi.niise_demo.LoginService;
import my.gov.imi.niise_demo.filters.JwtRequestFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Added for Swagger OpenAPI security configuration setup
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final LoginService loginService;
    private final JwtRequestFilter jwtFilter;
    private final PasswordEncoder passwordEncoder; // Injected from AppConfig cleanly

    /**
     * Swagger OpenAPI Configuration
     * Enables the global "Authorize" paddock button to pass Bearer tokens to secure routes.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("NIISE Demo API Console")
                        .version("1.0.0")
                        .description("Administrative backend systems configuration documentation."))
                // Automatically flags all endpoints documented in Swagger to require the authorization padlocks
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // Declares the structure as an HTTP Bearer header injection scheme
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }

    /**
     * Explicit authentication provider registration.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(loginService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager Bean.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Component-based Security Filter Chain definition.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/login").permitAll()
                .requestMatchers("/api/v1/pengguna/register").permitAll()
                // ADDED: Permits public GET/search access to standard user listings without token validation
                .requestMatchers("/api/v1/pengguna").permitAll()
                .requestMatchers("/api/v1/pengguna/search").permitAll()
                
                .requestMatchers(
                    "/v3/api-docs",
                    "/v3/api-docs/**", 
                    "/v3/api-docs.yaml", 
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/swagger-resources",
                    "/swagger-resources/**",
                    "/configuration/ui",
                    "/configuration/security",
                    "/webjars/**"
                ).permitAll()
                
                // Matches endpoints cleanly to catch any route mutations
                .requestMatchers("/api/v1/passports/admin", "/api/v1/passports/admin/**").authenticated()
                .requestMatchers("/api/v1/pengguna/admin", "/api/v1/pengguna/admin/**").authenticated()
                
                .anyRequest().authenticated()
            );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
