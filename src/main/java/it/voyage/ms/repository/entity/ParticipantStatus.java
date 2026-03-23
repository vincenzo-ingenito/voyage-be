package it.voyage.ms.repository.entity;

/**
 * Stato di un invito a partecipare a un viaggio di gruppo.
 * - PENDING: invito inviato ma non ancora accettato
 * - ACCEPTED: invito accettato
 * - DECLINED: invito rifiutato
 */
public enum ParticipantStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}