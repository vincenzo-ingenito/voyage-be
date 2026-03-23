package it.voyage.ms.repository.entity;

/**
 * Ruolo di un partecipante in un viaggio di gruppo.
 * - VIEWER: può solo visualizzare il viaggio
 * - EDITOR: può modificare il viaggio
 */
public enum ParticipantRole {
    VIEWER,
    EDITOR
}