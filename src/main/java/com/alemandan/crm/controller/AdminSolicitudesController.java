package com.alemandan.crm.controller;

import com.alemandan.crm.model.SolicitudRegistro;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.SolicitudRegistroRepository;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/solicitudes")
public class AdminSolicitudesController {

    @Autowired
    private SolicitudRegistroRepository solicitudRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MailService mailService;

    @GetMapping
    public String verSolicitudesPendientes(Model model) {
        model.addAttribute("solicitudes", solicitudRepo.findByAprobadaFalseAndRechazadaFalse());
        return "admin_solicitudes";
    }

    @PostMapping("/aprobar/{id}")
    public String aprobar(@PathVariable Long id) {
        SolicitudRegistro solicitud = solicitudRepo.findById(id).orElseThrow();
        solicitud.setAprobada(true);
        solicitudRepo.save(solicitud);

        // Crear usuario real
        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.getNombre());
        usuario.setEmail(solicitud.getEmail());
        usuario.setPassword(passwordEncoder.encode(solicitud.getPassword()));
        usuario.setActivo(true);
        usuario.setRol("EMPLEADO");
        usuarioRepo.save(usuario);

        // Enviar correo de aprobación
        mailService.enviarAprobacion(usuario.getEmail(), usuario.getNombre(), usuario.getEmail());

        return "redirect:/admin/solicitudes";
    }

    @PostMapping("/rechazar/{id}")
    public String rechazar(@PathVariable Long id) {
        SolicitudRegistro solicitud = solicitudRepo.findById(id).orElseThrow();
        solicitud.setRechazada(true);
        solicitudRepo.save(solicitud);

        // Enviar correo de rechazo
        mailService.enviarRechazo(solicitud.getEmail(), solicitud.getNombre());

        return "redirect:/admin/solicitudes";
    }
}