package com.alemandan.crm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarBienvenida(String to, String nombre) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(to);
        mensaje.setSubject("Bienvenido a AlemandanCRM");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Gracias por solicitar acceso a nuestra plataforma AlemandanCRM.\n"
                + "Tu registro fue recibido y está pendiente de aprobación por el administrador.\n"
                + "Recibirás otro correo cuando tu acceso sea aprobado.\n\n"
                + "Saludos,\nEquipo AlemandanCRM");
        mailSender.send(mensaje);
    }

    public void enviarAprobacion(String to, String nombre, String usuario) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(to);
        mensaje.setSubject("Acceso aprobado - AlemandanCRM");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Tu acceso ha sido aprobado. Puedes ingresar al sistema con tu usuario: " + usuario + "\n"
                + "Recuerda cambiar tu contraseña en el primer ingreso.\n\n"
                + "Saludos,\nEquipo AlemandanCRM");
        mailSender.send(mensaje);
    }

    public void enviarRechazo(String to, String nombre) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(to);
        mensaje.setSubject("Solicitud rechazada - AlemandanCRM");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Tu solicitud de acceso fue rechazada por el administrador.\n"
                + "Si crees que esto es un error, comunícate con la empresa.\n\n"
                + "Saludos,\nEquipo AlemandanCRM");
        mailSender.send(mensaje);
    }

    public void enviarCorreoRecuperarPassword(String to, String nombre, String link) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(to);
        mensaje.setSubject("Recuperar contraseña - AlemandanCRM");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Haz clic en el siguiente enlace para restablecer tu contraseña:\n"
                + link + "\n\n"
                + "Si no solicitaste este cambio, ignora este mensaje.\n\n"
                + "Saludos,\nEquipo AlemandanCRM");
        mailSender.send(mensaje);
    }


    public void enviarCorreoGenerico(String to, String asunto, String mensajeTexto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(to);
        mensaje.setSubject(asunto);
        mensaje.setText(mensajeTexto);
        mailSender.send(mensaje);
    }
}