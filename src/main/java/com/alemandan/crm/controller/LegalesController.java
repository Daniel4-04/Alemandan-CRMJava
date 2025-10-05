package com.alemandan.crm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalesController {

    @GetMapping("/terminos")
    public String terminos() {
        return "terminos";
    }

    @GetMapping("/privacidad")
    public String privacidad() {
        return "privacidad";
    }

    @GetMapping("/faq")
    public String faq() {
        return "faq";
    }

    @GetMapping("/mapa")
    public String mapa() {
        return "mapa";
    }

    @GetMapping("/pqr")
    public String pqr() {
        return "pqr";
    }

}