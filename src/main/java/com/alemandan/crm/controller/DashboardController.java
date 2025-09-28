package com.alemandan.crm.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        if(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "dashboardadmin";
        } else if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLEADO"))) {
            return "dashboardempleado";
        }
        return "redirect:/login?error";
    }
}