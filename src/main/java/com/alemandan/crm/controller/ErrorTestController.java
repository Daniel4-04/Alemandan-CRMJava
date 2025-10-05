package com.alemandan.crm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorTestController {

    @GetMapping("/provocar-error")
    public String provocarError() {
        throw new RuntimeException("Error de prueba 500");
    }
}