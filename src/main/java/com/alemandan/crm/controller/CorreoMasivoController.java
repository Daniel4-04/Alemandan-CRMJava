package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/correos-masivos")
public class CorreoMasivoController {

    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private MailService mailService;

    @GetMapping
    public String mostrarFormulario(Model model) {
        List<Usuario> usuarios = usuarioRepo.findAll();
        model.addAttribute("usuarios", usuarios);
        return "correos_masivos";
    }

    @PostMapping
    public String enviarCorreos(@RequestParam(required = false) List<Long> destinatarios,
                                @RequestParam String asunto,
                                @RequestParam String mensaje,
                                Model model) {
        if (destinatarios != null && !destinatarios.isEmpty()) {
            List<Usuario> usuarios = usuarioRepo.findAllById(destinatarios);
            for (Usuario usuario : usuarios) {
                mailService.enviarCorreoGenerico(usuario.getEmail(), asunto, mensaje);
            }
            model.addAttribute("mensaje", "Correos enviados correctamente.");
        } else {
            model.addAttribute("error", "Debes seleccionar al menos un destinatario.");
        }
        model.addAttribute("usuarios", usuarioRepo.findAll());
        return "correos_masivos";
    }
}