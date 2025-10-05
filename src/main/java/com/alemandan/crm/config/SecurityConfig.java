package com.alemandan.crm.config;

import com.alemandan.crm.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // Permite POST al endpoint de exportar gráfica a PDF
                        .requestMatchers(HttpMethod.POST, "/admin/ventas/exportar-grafico-pdf").permitAll()
                        // Acceso público (¡aquí están las páginas legales y ayuda!)
                        .requestMatchers(
                                "/",
                                "/index",
                                "/login",
                                "/usuarios/nuevo",
                                "/usuarios/guardar",
                                "/registro",
                                "/recuperar",
                                "/reset-password",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/images/**",
                                "/terminos",
                                "/privacidad",
                                "/faq",
                                "/pqr",
                                "/mapa"
                        ).permitAll()
                        // Dashboard general (redirigido según rol)
                        .requestMatchers("/dashboard").authenticated()
                        // Módulos solo para ADMIN
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
                        .ignoringRequestMatchers("/admin/ventas/exportar-grafico-pdf", "/api/ventas/**", "/logout")
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