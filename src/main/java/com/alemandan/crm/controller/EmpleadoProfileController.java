package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.UsuarioService;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/empleado/profile")
public class EmpleadoProfileController {

    private final String UPLOAD_DIR = "uploads/users";

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/update")
    public String updateProfile(@RequestParam String nombre,
                                @RequestParam String email,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                Authentication auth,
                                HttpServletRequest request) {
        String usernameOld = ((User) auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(usernameOld);
        if (usuario == null) {
            return "redirect:/dashboard-empleado?error=usuario_no_encontrado";
        }

        boolean changingEmail = !usernameOld.equals(email);
        boolean changingPassword = newPassword != null && !newPassword.trim().isEmpty();

        // If changing email or password, validate current password
        if (changingEmail || changingPassword) {
            if (currentPassword == null || currentPassword.isBlank()) {
                return "redirect:/dashboard-empleado?error=" + "Se requiere la contraseña actual para realizar cambios";
            }
            try {
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(usernameOld, currentPassword));
            } catch (Exception ex) {
                return "redirect:/dashboard-empleado?error=" + "Contraseña actual incorrecta";
            }
        }

        usuario.setNombre(nombre);
        usuario.setEmail(email);

        if (changingPassword) {
            usuario.setPassword(passwordEncoder.encode(newPassword));
        }

        usuarioRepository.save(usuario);

        // Re-authenticate so session principal is updated
        try {
            String passwordForAuth = changingPassword ? newPassword : currentPassword;
            if (passwordForAuth == null) {
                Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                if (currentAuth != null) {
                    Object credentials = currentAuth.getCredentials() != null ? currentAuth.getCredentials() : "";
                    User newPrincipal = new User(usuario.getEmail(), credentials.toString(), currentAuth.getAuthorities());
                    UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(newPrincipal, credentials, currentAuth.getAuthorities());
                    newAuth.setDetails(currentAuth.getDetails());
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                    if (request.getSession(false) != null) {
                        request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                    }
                }
            } else {
                Authentication newAuth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(usuario.getEmail(), passwordForAuth)
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
                if (request.getSession(false) != null) {
                    request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                }
            }
        } catch (Exception ex) {
            return "redirect:/dashboard-empleado?updated=1&warning=reauth_failed";
        }

        return "redirect:/dashboard-empleado?updated=1";
    }

    @PostMapping("/photo")
    public String uploadPhoto(@RequestParam("photo") MultipartFile photo,
                              Authentication auth) throws IOException {
        String username = ((User) auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(username);
        if (usuario == null) {
            return "redirect:/dashboard-empleado?error=usuario_no_encontrado";
        }
        if (photo != null && !photo.isEmpty()) {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String original = photo.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String filename = "user_" + usuario.getId() + "_" + System.currentTimeMillis() + ext;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(photo.getInputStream(), filePath);

            String publicPath = "/uploads/users/" + filename;

            try {
                BeanWrapperImpl wrapper = new BeanWrapperImpl(usuario);
                if (wrapper.isWritableProperty("imagePath")) {
                    wrapper.setPropertyValue("imagePath", publicPath);
                } else if (wrapper.isWritableProperty("image")) {
                    wrapper.setPropertyValue("image", publicPath);
                }
            } catch (Exception ex) {
                // ignore
            }

            usuarioRepository.save(usuario);
        }
        return "redirect:/dashboard-empleado?photoUpdated=1";
    }
}