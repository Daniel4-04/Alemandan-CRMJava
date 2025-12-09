package com.alemandan.crm.config;

import com.alemandan.crm.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Solo para pruebas, NO USAR EN PRODUCCIÓN
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exponer AuthenticationManager como bean para poder inyectarlo (necesario para re-autenticación).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // Permite POST al endpoint de exportar gráfica a PDF
                        .requestMatchers(HttpMethod.POST, "/admin/ventas/exportar-grafico-pdf").permitAll()
                        // Acceso público (páginas legales y ayuda) + recursos públicos y uploads
                        .requestMatchers(
                                "/",
                                "/index",
                                "/login",
                                "/usuarios/nuevo",
                                "/usuarios/guardar",
                                "/registro",
                                "/recuperar",
                                "/reset-password",
                                "/password-reset.html",
                                "/api/password-reset/**",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/images/**",
                                "/uploads/**",
                                "/terminos",
                                "/privacidad",
                                "/faq",
                                "/pqr",
                                "/mapa"
                        ).permitAll()
                        // El Web Service resumen puede ser consumido por cualquier usuario autenticado
                        .requestMatchers("/api/ventas/resumen").authenticated()
                        // Dashboard general (redirigido según rol)
                        .requestMatchers("/dashboard").authenticated()
                        // Módulos solo para ADMIN (usar hasAuthority si tus authorities son "ADMIN" / "EMPLEADO")
                        .requestMatchers("/usuarios/**", "/productos/**", "/proveedores/**", "/dashboard-admin", "/dashboard-admin/**").hasAuthority("ADMIN")
                        // Módulos solo para EMPLEADO
                        .requestMatchers(
                                "/ventas/caja", "/ventas/caja/**",
                                "/ventas/registrar", "/ventas/mis-ventas",
                                "/dashboard-empleado", "/dashboard-empleado/**",
                                "/api/ventas/**"
                        ).hasAuthority("EMPLEADO")
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                // Ignora CSRF para endpoints AJAX de ventas y exportar PDF
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/admin/ventas/exportar-grafico-pdf", "/api/ventas/**", "/api/password-reset/**", "/logout")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout.permitAll());
        return http.build();
    }
}