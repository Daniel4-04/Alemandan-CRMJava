package com.alemandan.crm.events;

/**
 * Event published when a user is successfully registered (after save).
 * Listeners can handle this event asynchronously after transaction commit.
 */
public class UsuarioRegistradoEvent {

    private final Long usuarioId;

    public UsuarioRegistradoEvent(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }
}
