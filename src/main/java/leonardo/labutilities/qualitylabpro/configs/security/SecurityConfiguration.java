package leonardo.labutilities.qualitylabpro.configs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import leonardo.labutilities.qualitylabpro.configs.constants.ApiEndpoints;
import leonardo.labutilities.qualitylabpro.domains.users.enums.UserRoles;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfiguration {

      private final SecurityFilter securityFilter;
      private static final String[] PUBLIC_PATHS =
                  {"/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/actuator/**"};


      @Bean
      protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable).cors(Customizer.withDefaults())
                        .sessionManagement(
                                    sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(req -> {
                              // Public endpoints
                              req.requestMatchers(PUBLIC_PATHS).permitAll();
                              req.requestMatchers(HttpMethod.POST, ApiEndpoints.PUBLIC_POST_PATHS)
                                          .permitAll();
                              req.requestMatchers(HttpMethod.PATCH, ApiEndpoints.PASSWORD_PATH)
                                          .permitAll();

                              // Admin endpoints
                              req.requestMatchers(HttpMethod.DELETE,
                                          ApiEndpoints.ADMIN_MODIFY_PATHS)
                                          .hasRole(UserRoles.ADMIN.name());
                              req.requestMatchers(HttpMethod.PUT, ApiEndpoints.ADMIN_MODIFY_PATHS)
                                          .hasRole(UserRoles.ADMIN.name());
                              req.requestMatchers(HttpMethod.PATCH, ApiEndpoints.ADMIN_MODIFY_PATHS)
                                          .hasRole(UserRoles.ADMIN.name());

                              // User management (admin only)
                              req.requestMatchers(HttpMethod.DELETE, ApiEndpoints.USERS_PATH)
                                          .hasRole(UserRoles.ADMIN.name());
                              req.requestMatchers(HttpMethod.PUT, ApiEndpoints.USERS_PATH)
                                          .hasRole(UserRoles.ADMIN.name());

                              // Require authentication for all other requests
                              req.anyRequest().authenticated();
                        })
                        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                        .build();
      }

      @Bean
      AuthenticationManager authMenager(AuthenticationConfiguration configuration)
                  throws Exception {
            return configuration.getAuthenticationManager();
      }

      @Bean
      PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
      }
}
