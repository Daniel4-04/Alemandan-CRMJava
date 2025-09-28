package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Listar usuarios
    @GetMapping
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.getAllUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "usuarios/lista"; // Vista Thymeleaf
    }

    // Mostrar formulario para nuevo usuario
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoUsuario(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "usuarios/nuevo"; // Vista Thymeleaf
    }

    // Guardar usuario (con verificación de email único)
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, Model model) {
        if (usuarioService.existeUsuarioPorEmail(usuario.getEmail())) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "Ya existe un usuario con ese correo electrónico.");
            return "usuarios/nuevo"; // muestra el formulario de nuevo usuario con error
        }
        usuarioService.saveUsuario(usuario);
        return "redirect:/usuarios";
    }

    // Eliminar (INACTIVAR) usuario
    @GetMapping("/eliminar/{id}")
    public String inactivarUsuario(@PathVariable Long id) {
        usuarioService.inactivarUsuario(id);
        return "redirect:/usuarios";
    }
}