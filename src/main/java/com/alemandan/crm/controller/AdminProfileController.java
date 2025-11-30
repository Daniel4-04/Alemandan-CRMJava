package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.UsuarioService;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final String UPLOAD_DIR = "uploads/users";

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Update profile with re-authentication using AuthenticationManager.
     * If email or password is changed, currentPassword is required.
     * newPassword is optional (if provided it will replace the old password).
     */
    @PostMapping("/update")
    public String updateProfile(@RequestParam String nombre,
                                @RequestParam String email,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                Authentication auth,
                                HttpServletRequest request) {
        String usernameOld = ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(usernameOld);
        if (usuario == null) {
            return "redirect:/dashboard?error=usuario_no_encontrado";
        }

        boolean changingEmail = !usernameOld.equals(email);
        boolean changingPassword = newPassword != null && !newPassword.trim().isEmpty();

        // If changing email or password, validate current password by authenticating
        if (changingEmail || changingPassword) {
            if (currentPassword == null || currentPassword.isBlank()) {
                return "redirect:/dashboard?error=" + "Se requiere la contraseña actual para realizar cambios críticos";
            }
            try {
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(usernameOld, currentPassword));
            } catch (Exception ex) {
                return "redirect:/dashboard?error=" + "Contraseña actual incorrecta";
            }
        }

        // Apply updates
        usuario.setNombre(nombre);
        usuario.setEmail(email);

        if (changingPassword) {
            String encoded = passwordEncoder.encode(newPassword);
            usuario.setPassword(encoded);
        }

        usuarioRepository.save(usuario);

        // Now re-authenticate/set new Authentication in SecurityContext with new credentials
        try {
            String passwordForAuth = changingPassword ? newPassword : currentPassword;
            if (passwordForAuth == null) {
                // No password provided and not changing password: reuse current authentication
                // Update principal name if email changed by creating a new UserDetails with same authorities
                Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                if (currentAuth != null) {
                    Object credentials = currentAuth.getCredentials() != null ? currentAuth.getCredentials() : "";
                    Object principalObj = currentAuth.getPrincipal();
                    // Build new UserDetails (using Spring's User) with updated username and same authorities
                    UserDetails newPrincipal = User.withUsername(usuario.getEmail())
                            .password(credentials.toString())
                            .authorities(currentAuth.getAuthorities())
                            .build();
                    UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(newPrincipal, credentials, currentAuth.getAuthorities());
                    newAuth.setDetails(currentAuth.getDetails());
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                    if (request.getSession(false) != null) {
                        request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                    }
                }
            } else {
                // Authenticate against the updated user record (if password changed we saved encoded, authenticationManager will compare)
                Authentication newAuth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(usuario.getEmail(), passwordForAuth)
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
                if (request.getSession(false) != null) {
                    request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                }
            }
        } catch (Exception ex) {
            // If re-authentication fails, still return but show a warning (user will have to login again)
            return "redirect:/dashboard?updated=1&warning=reauth_failed";
        }

        return "redirect:/dashboard?updated=1";
    }

    // Upload photo handler remains same as before
    @PostMapping("/photo")
    public String uploadPhoto(@RequestParam("photo") MultipartFile photo,
                              Authentication auth) throws IOException {
        String username = ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername();
        Usuario usuario = usuarioService.findByEmail(username);
        if (usuario == null) {
            return "redirect:/dashboard?error=usuario_no_encontrado";
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
                // ignore binding errors
            }

            usuarioRepository.save(usuario);
        }
        return "redirect:/dashboard?photoUpdated=1";
    }
}