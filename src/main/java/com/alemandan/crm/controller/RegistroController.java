package com.alemandan.crm.controller;

import com.alemandan.crm.model.SolicitudRegistro;
import com.alemandan.crm.repository.SolicitudRegistroRepository;
import com.alemandan.crm.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Controller
public class RegistroController {

    private static final Logger logger = LoggerFactory.getLogger(RegistroController.class);

    @Autowired
    private SolicitudRegistroRepository solicitudRepo;

    @Autowired
    private MailService mailService;

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("solicitud", new SolicitudRegistro());
        return "login";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute SolicitudRegistro solicitud, Model model) {
        // Validar email único
        if (solicitudRepo.existsByEmail(solicitud.getEmail())) {
            model.addAttribute("registroError", "Ya existe una solicitud con ese correo.");
            model.addAttribute("solicitud", solicitud);
            return "login";
        }
        // Validar contraseñas iguales
        if (!solicitud.getPassword().equals(solicitud.getConfirmPassword())) {
            model.addAttribute("registroError", "Las contraseñas no coinciden.");
            model.addAttribute("solicitud", solicitud);
            return "login";
        }
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setAprobada(false);
        solicitud.setRechazada(false);
        solicitudRepo.save(solicitud);

        // Enviar correo de bienvenida - wrapped in try-catch to prevent SMTP failures from blocking registration
        try {
            mailService.enviarBienvenida(solicitud.getEmail(), solicitud.getNombre());
        } catch (Exception e) {
            // Log error but continue - user is already registered
            logger.error("Failed to send welcome email to {} after registration, but registration succeeded", 
                solicitud.getEmail(), e);
        }

        model.addAttribute("mensaje", "Tu solicitud fue enviada y está pendiente de aprobación.");
        model.addAttribute("solicitud", new SolicitudRegistro());
        return "login";
    }
}