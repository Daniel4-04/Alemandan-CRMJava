package com.alemandan.crm.service;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Obtener todos los usuarios
    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findAll();
    }

    // Guardar un usuario
    public Usuario saveUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    // Buscar usuario por ID
    public Optional<Usuario> getUsuarioById(Long id) {
        return usuarioRepository.findById(id);
    }

    // Eliminar usuario por ID
    public void deleteUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }

    // Buscar usuario por email
    public Usuario getUsuarioByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
}