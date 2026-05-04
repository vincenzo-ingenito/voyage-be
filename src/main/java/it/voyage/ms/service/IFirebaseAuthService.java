package it.voyage.ms.service;

public interface IFirebaseAuthService {

    /**
     * Elimina utente da Firebase Auth 
     */
    void deleteUser(String userId);
}