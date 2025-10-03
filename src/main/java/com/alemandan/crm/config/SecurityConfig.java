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
                        // Acceso público
                        .requestMatchers(
                                "/",               // <-- La raíz ahora también es pública
                                "/index",          // <-- Permite acceder a /index y / directamente
                                "/login",
                                "/usuarios/nuevo",
                                "/usuarios/guardar",
                                "/registro",
                                "/recuperar",
                                "/reset-password",
                                "/css/**",
                                "/js/**",
                                "/assets/**",
                                "/images/**"
                        ).permitAll()
                        // Dashboard general (redirigido según rol)
                        .requestMatchers("/dashboard").authenticated()
                        // Módulos solo para ADMIN
                        .requestMatchers("/usuarios/**", "/productos/**", "/proveedores/**", "/dashboard-admin", "/dashboard-admin/**").hasRole("ADMIN")
                        // Módulos solo para EMPLEADO
                        .requestMatchers("/ventas/caja", "/ventas/caja/**", "/ventas/registrar", "/ventas/mis-ventas", "/dashboard-empleado", "/dashboard-empleado/**").hasRole("EMPLEADO")
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                // Desactiva CSRF para evitar bloqueos con fetch POST desde JS
                .csrf(csrf -> csrf.ignoringRequestMatchers("/admin/ventas/exportar-grafico-pdf"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout.permitAll());
        return http.build();
    }
}