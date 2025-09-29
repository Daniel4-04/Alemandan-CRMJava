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

    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    // Listar todos los usuarios activos
    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findByActivoTrue();
    }

    // Inactivar usuario (eliminación lógica)
    public void inactivarUsuario(Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        }
    }

    // Verificar existencia por email
    public boolean existeUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email) != null;
    }

    // Listar empleados (para filtros de ventas admin)
    public List<Usuario> listarEmpleados() {
        return usuarioRepository.findByRolAndActivoTrue("EMPLEADO");
    }
}