package it.voyage.ms.srv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.voyage.ms.enums.BlockActionEnum;
import it.voyage.ms.enums.FriendRelationshipStatusEnum;
import it.voyage.ms.exceptions.ConflictException;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.mapper.FriendrelationshipMapper;
import it.voyage.ms.mapper.UserMapper;
import it.voyage.ms.repository.entity.FriendRelationshipEty;
import it.voyage.ms.repository.entity.FriendRelationshipEty.Status;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.IFriendRelationshipRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.INotificationService;
import it.voyage.ms.service.impl.FriendshipService;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendshipService — modello follow asimmetrico")
class FriendshipServiceTest {

    @Mock UserRepository                userRepository;
    @Mock IFriendRelationshipRepository friendRelationshipRepository;
    @Mock FriendrelationshipMapper      friendrelationShipMapper;
    @Mock UserMapper                    userMapper;
    @Mock INotificationService          notificationService;

    @InjectMocks FriendshipService service;

    // ─── Costanti ────────────────────────────────────────────────────────────

    static final String A = "user-A";
    static final String B = "user-B";

    // ─── Factory helpers ─────────────────────────────────────────────────────

    static UserEty privateUser(String id) {
        UserEty u = new UserEty();
        u.setId(id);
        u.setName("Name-" + id);
        u.setPrivate(true);
        return u;
    }

    static UserEty publicUser(String id) {
        UserEty u = new UserEty();
        u.setId(id);
        u.setName("Name-" + id);
        u.setPrivate(false);
        return u;
    }

    static FriendRelationshipEty rel(String from, String to, Status status) {
        FriendRelationshipEty r = new FriendRelationshipEty();
        r.setRequesterId(from);
        r.setReceiverId(to);
        r.setStatus(status);
        return r;
    }

    // =========================================================================
    // sendFriendRequest
    // =========================================================================

    @Nested
    @DisplayName("sendFriendRequest")
    class SendFriendRequest {

        @BeforeEach
        void noExistingRelation() {
            // Default: nessun record in nessuna direzione
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(anyString(), anyString()))
                    .thenReturn(Optional.empty());
        }

        // ── I quattro test case del requisito ─────────────────────────────────

        @Test
        @DisplayName("TC1 — A(priv)→B(priv): crea PENDING, A vedrà B solo dopo accept, notifica inviata")
        void tc1_privatoVersoPravato_creaPending() {
            when(userRepository.findById(A)).thenReturn(Optional.of(privateUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(privateUser(B)));

            String result = service.sendFriendRequest(A, B);

            assertThat(result).contains("inviata con successo");

            ArgumentCaptor<FriendRelationshipEty> cap = ArgumentCaptor.forClass(FriendRelationshipEty.class);
            verify(friendRelationshipRepository).save(cap.capture());
            assertThat(cap.getValue().getRequesterId()).isEqualTo(A);
            assertThat(cap.getValue().getReceiverId()).isEqualTo(B);
            assertThat(cap.getValue().getStatus()).isEqualTo(Status.PENDING);

            // Un solo record creato: nessun B→A automatico
            verify(friendRelationshipRepository, times(1)).save(any());
            verify(notificationService).sendFriendRequestNotification(eq(B), anyString(), eq(A), any());
        }

        @Test
        @DisplayName("TC2 — A(priv)→B(pub): ACCEPTED immediato, B NON vede A (nessun record inverso)")
        void tc2_privatoVersoPublico_acceptedImmediato() {
            when(userRepository.findById(A)).thenReturn(Optional.of(privateUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(publicUser(B)));

            String result = service.sendFriendRequest(A, B);

            assertThat(result).contains("segui questo utente");

            ArgumentCaptor<FriendRelationshipEty> cap = ArgumentCaptor.forClass(FriendRelationshipEty.class);
            verify(friendRelationshipRepository).save(cap.capture());
            assertThat(cap.getValue().getStatus()).isEqualTo(Status.ACCEPTED);
            // Esattamente un record: A→B. Mai B→A.
            verify(friendRelationshipRepository, times(1)).save(any());
            verify(notificationService).sendNewFollowerNotification(eq(B), anyString(), eq(A), any());
        }

        @Test
        @DisplayName("TC3 — A(pub)→B(priv): crea PENDING, A vedrà B solo dopo accept")
        void tc3_pubblicoVersoPrivato_creaPending() {
            when(userRepository.findById(A)).thenReturn(Optional.of(publicUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(privateUser(B)));

            String result = service.sendFriendRequest(A, B);

            assertThat(result).contains("inviata con successo");

            ArgumentCaptor<FriendRelationshipEty> cap = ArgumentCaptor.forClass(FriendRelationshipEty.class);
            verify(friendRelationshipRepository).save(cap.capture());
            assertThat(cap.getValue().getStatus()).isEqualTo(Status.PENDING);
            verify(friendRelationshipRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("TC4 — A(pub)→B(pub): ACCEPTED immediato, B NON vede A")
        void tc4_pubblicoVersoPublico_acceptedImmediato() {
            when(userRepository.findById(A)).thenReturn(Optional.of(publicUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(publicUser(B)));

            String result = service.sendFriendRequest(A, B);

            assertThat(result).contains("segui questo utente");

            ArgumentCaptor<FriendRelationshipEty> cap = ArgumentCaptor.forClass(FriendRelationshipEty.class);
            verify(friendRelationshipRepository).save(cap.capture());
            assertThat(cap.getValue().getStatus()).isEqualTo(Status.ACCEPTED);
            verify(friendRelationshipRepository, times(1)).save(any());
        }

        // ── Idempotenza ───────────────────────────────────────────────────────

        @Test
        @DisplayName("Idempotenza — A già segue B (ACCEPTED) → messaggio senza nuovi record")
        void idempotenza_giaAccepted() {
            when(userRepository.findById(A)).thenReturn(Optional.of(publicUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(publicUser(B)));
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(rel(A, B, Status.ACCEPTED)));

            String result = service.sendFriendRequest(A, B);

            assertThat(result).contains("già seguendo");
            verify(friendRelationshipRepository, never()).save(any());
        }

        @Test
        @DisplayName("Idempotenza — A ha già PENDING verso B → messaggio senza nuovi record")
        void idempotenza_giaPending() {
            when(userRepository.findById(A)).thenReturn(Optional.of(privateUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(privateUser(B)));
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(rel(A, B, Status.PENDING)));

            String result = service.sendFriendRequest(A, B);

            assertThat(result).contains("già inviato");
            verify(friendRelationshipRepository, never()).save(any());
        }

        @Test
        @DisplayName("Idempotenza — A ha bloccato B → ConflictException")
        void idempotenza_blocked_lanceConflict() {
            when(userRepository.findById(A)).thenReturn(Optional.of(privateUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(privateUser(B)));
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(rel(A, B, Status.BLOCKED)));

            assertThatThrownBy(() -> service.sendFriendRequest(A, B))
                    .isInstanceOf(ConflictException.class);
            verify(friendRelationshipRepository, never()).save(any());
        }

        // ── Auto-accept pending inverso ───────────────────────────────────────

        @Test
        @DisplayName("Auto-accept — B aveva PENDING verso A, A invia → entrambi ACCEPTED (2 record separati)")
        void autoAccept_pendingInverso_dueRecordSeparati() {
            when(userRepository.findById(A)).thenReturn(Optional.of(privateUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(privateUser(B)));

            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.empty());
            FriendRelationshipEty bToA = rel(B, A, Status.PENDING);
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(B, A))
                    .thenReturn(Optional.of(bToA));

            String result = service.sendFriendRequest(A, B);

            assertThat(result).contains("automaticamente");

            // Il record B→A deve essere passato ad ACCEPTED
            assertThat(bToA.getStatus()).isEqualTo(Status.ACCEPTED);

            // Deve essere salvato anche un nuovo record A→B ACCEPTED
            ArgumentCaptor<FriendRelationshipEty> cap = ArgumentCaptor.forClass(FriendRelationshipEty.class);
            verify(friendRelationshipRepository, times(2)).save(cap.capture());

            boolean hasAtoB = cap.getAllValues().stream().anyMatch(r ->
                    A.equals(r.getRequesterId()) && B.equals(r.getReceiverId())
                    && r.getStatus() == Status.ACCEPTED);
            assertThat(hasAtoB).isTrue();
        }

        // ── Utenti non trovati ────────────────────────────────────────────────

        @Test
        @DisplayName("Requester non trovato → NotFoundException")
        void requesterNonTrovato() {
            when(userRepository.findById(A)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.sendFriendRequest(A, B))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Receiver non trovato → NotFoundException")
        void receiverNonTrovato() {
            when(userRepository.findById(A)).thenReturn(Optional.of(publicUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.sendFriendRequest(A, B))
                    .isInstanceOf(NotFoundException.class);
        }

        // ── Resilienza notifiche ──────────────────────────────────────────────

        @Test
        @DisplayName("Errore FCM non propaga eccezione al caller (privato→privato)")
        void erroreNotificaPrivato_nonPropaga() {
            when(userRepository.findById(A)).thenReturn(Optional.of(privateUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(privateUser(B)));
            doThrow(new RuntimeException("FCM down"))
                    .when(notificationService)
                    .sendFriendRequestNotification(any(), any(), any(), any());

            assertThatCode(() -> service.sendFriendRequest(A, B)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Errore FCM non propaga eccezione al caller (pubblico→pubblico)")
        void erroreNotificaPublico_nonPropaga() {
            when(userRepository.findById(A)).thenReturn(Optional.of(publicUser(A)));
            when(userRepository.findById(B)).thenReturn(Optional.of(publicUser(B)));
            doThrow(new RuntimeException("FCM down"))
                    .when(notificationService)
                    .sendNewFollowerNotification(any(), any(), any(), any());

            assertThatCode(() -> service.sendFriendRequest(A, B)).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // handleFriendRequest (accept / decline)
    // =========================================================================

    @Nested
    @DisplayName("handleFriendRequest")
    class HandleFriendRequest {

        @Test
        @DisplayName("Accept TC1 — B accetta A→B: record ACCEPTED, NESSUN record B→A creato automaticamente")
        void accept_tc1_nessunRecordInverso() {
            FriendRelationshipEty aToB = rel(A, B, Status.PENDING);
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(aToB));

            String result = service.handleFriendRequest(A, B, "accept");

            assertThat(result).contains("accettata");
            assertThat(aToB.getStatus()).isEqualTo(Status.ACCEPTED);

            // Esattamente 1 save: il record A→B aggiornato. MAI B→A.
            ArgumentCaptor<FriendRelationshipEty> cap = ArgumentCaptor.forClass(FriendRelationshipEty.class);
            verify(friendRelationshipRepository, times(1)).save(cap.capture());
            assertThat(cap.getValue().getRequesterId()).isEqualTo(A);
            assertThat(cap.getValue().getReceiverId()).isEqualTo(B);
        }

        @Test
        @DisplayName("Accept TC3 — A(pub)→B(priv) accept: solo A→B ACCEPTED, B non vede A")
        void accept_tc3_pubblicoVersoPrivato() {
            FriendRelationshipEty aToB = rel(A, B, Status.PENDING);
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(aToB));

            service.handleFriendRequest(A, B, "accept");

            assertThat(aToB.getStatus()).isEqualTo(Status.ACCEPTED);
            verify(friendRelationshipRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Accept — relazione non trovata → NotFoundException")
        void accept_nonTrovata() {
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.handleFriendRequest(A, B, "accept"))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Accept — relazione già ACCEPTED (non PENDING) → NotFoundException")
        void accept_giàAccettata() {
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(rel(A, B, Status.ACCEPTED)));

            assertThatThrownBy(() -> service.handleFriendRequest(A, B, "accept"))
                    .isInstanceOf(NotFoundException.class);
        }

//        @Test
//        @DisplayName("Decline — record eliminato, A torna AVAILABLE")
//        void decline_eliminaRecord() {
//            when(friendRelationshipRepository.deleteFriendship(B, A)).thenReturn(1L);
//
//            String result = service.handleFriendRequest(A, B, "decline");
//
//            assertThat(result).contains("rifiutata");
//            verify(friendRelationshipRepository).deleteFriendship(B, A);
//        }
//
//        @Test
//        @DisplayName("Decline — record non trovato → NotFoundException")
//        void decline_nonTrovato() {
//            when(friendRelationshipRepository.deleteFriendship(B, A)).thenReturn(0L);
//
//            assertThatThrownBy(() -> service.handleFriendRequest(A, B, "decline"))
//                    .isInstanceOf(NotFoundException.class);
//        }

        @Test
        @DisplayName("Azione non valida → IllegalArgumentException")
        void azioneNonValida() {
            assertThatThrownBy(() -> service.handleFriendRequest(A, B, "foobar"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Accept case-insensitive — 'ACCEPT' funziona come 'accept'")
        void accept_caseInsensitive() {
            FriendRelationshipEty aToB = rel(A, B, Status.PENDING);
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(aToB));

            assertThatCode(() -> service.handleFriendRequest(A, B, "ACCEPT"))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // deleteFriendship — sempre unilaterale
    // =========================================================================

    @Nested
    @DisplayName("deleteFriendship — cancellazione unilaterale")
    class DeleteFriendship {

        @Test
        @DisplayName("Smetti di seguire — elimina A→B, il record B→A non viene mai toccato")
        void unfollow_soloRecordDiretto() {
            FriendRelationshipEty aToB = rel(A, B, Status.ACCEPTED);
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(aToB));

            service.deleteFriendship(A, B);

            verify(friendRelationshipRepository).delete(aToB);
            // B→A non viene mai cercato
            verify(friendRelationshipRepository, never()).findByRequesterIdAndReceiverId(B, A);
        }

        @Test
        @DisplayName("Annulla pending — A cancella la propria richiesta PENDING verso B")
        void annullaPending() {
            FriendRelationshipEty aToB = rel(A, B, Status.PENDING);
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(aToB));

            service.deleteFriendship(A, B);

            verify(friendRelationshipRepository).delete(aToB);
        }

        @Test
        @DisplayName("Rimuovi follower — non esiste A→B, esiste B→A: si elimina B→A")
        void rimuoviFollower_eliminaInverso() {
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.empty());
            FriendRelationshipEty bToA = rel(B, A, Status.ACCEPTED);
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(B, A))
                    .thenReturn(Optional.of(bToA));

            service.deleteFriendship(A, B);

            verify(friendRelationshipRepository).delete(bToA);
        }

        @Test
        @DisplayName("Nessun record → NotFoundException")
        void nessunRecord_notFound() {
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteFriendship(A, B))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Asimmetria garantita — delete A→B non tocca mai B→A")
        void asimmetriaGarantita() {
            FriendRelationshipEty aToB = rel(A, B, Status.ACCEPTED);
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(aToB));

            service.deleteFriendship(A, B);

            // Il delete non deve mai essere chiamato con un record B→A
            verify(friendRelationshipRepository, never()).delete(argThat(r ->
                    B.equals(r.getRequesterId()) && A.equals(r.getReceiverId())));
        }
    }

    // =========================================================================
    // checkIfUserAreFriends
    // =========================================================================

    @Nested
    @DisplayName("checkIfUserAreFriends")
    class CheckIfUserAreFriends {

        @Test
        @DisplayName("A→B ACCEPTED → true")
        void aSegueB_true() {
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(rel(A, B, Status.ACCEPTED)));
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(B, A))
                    .thenReturn(Optional.empty());

            assertThat(service.checkIfUserAreFriends(A, B)).isTrue();
        }

        @Test
        @DisplayName("Solo B→A ACCEPTED → true (direzione inversa è sufficiente)")
        void bSegueA_true() {
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.empty());
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(B, A))
                    .thenReturn(Optional.of(rel(B, A, Status.ACCEPTED)));

            assertThat(service.checkIfUserAreFriends(A, B)).isTrue();
        }

        @Test
        @DisplayName("Nessun record → false")
        void nessunRecord_false() {
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThat(service.checkIfUserAreFriends(A, B)).isFalse();
        }

        @Test
        @DisplayName("A→B PENDING (non ACCEPTED) → false")
        void pending_false() {
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(A, B))
                    .thenReturn(Optional.of(rel(A, B, Status.PENDING)));
            when(friendRelationshipRepository.findByRequesterIdAndReceiverId(B, A))
                    .thenReturn(Optional.empty());

            assertThat(service.checkIfUserAreFriends(A, B)).isFalse();
        }
    }

    // =========================================================================
    // executeBlockAction
    // =========================================================================

    @Nested
    @DisplayName("executeBlockAction")
    class ExecuteBlockAction {

        @Test
        @DisplayName("BLOCK → chiama updateRelationshipStatus con Status.BLOCKED")
        void block() {
            service.executeBlockAction(A, B, BlockActionEnum.BLOCK);
            verify(friendRelationshipRepository).updateRelationshipStatus(A, B, Status.BLOCKED);
        }

        @Test
        @DisplayName("UNBLOCK → chiama deleteRelationship")
        void unblock() {
            service.executeBlockAction(A, B, BlockActionEnum.UNBLOCK);
            verify(friendRelationshipRepository).deleteRelationship(A, B);
        }
    }

    // =========================================================================
    // searchUsersAndDetermineStatus — mapToSearchResult
    // =========================================================================

    @Nested
    @DisplayName("searchUsersAndDetermineStatus — stati UI")
    class SearchStatus {

        private UserEty userB;

        @BeforeEach
        void setup() {
            userB = publicUser(B);
            when(friendRelationshipRepository.findByReceiverIdAndStatus(A, Status.BLOCKED))
                    .thenReturn(Collections.emptyList());
            when(friendRelationshipRepository.findByRequesterIdAndStatus(A, Status.BLOCKED))
                    .thenReturn(Collections.emptyList());
            when(userRepository.findByNameRegex(anyString()))
                    .thenReturn(List.of(userB));
        }

        @Test
        @DisplayName("Nessuna relazione → AVAILABLE")
        void nessunRelazione_available() {
            when(friendRelationshipRepository.findAllRelevantRelationships(eq(A), any()))
                    .thenReturn(Collections.emptyList());

            service.searchUsersAndDetermineStatus("Name", A);

            verify(userMapper).mapToSearchResult(userB, FriendRelationshipStatusEnum.AVAILABLE);
        }

        @Test
        @DisplayName("A→B ACCEPTED → ALREADY_FRIENDS")
        void aSegueB_alreadyFriends() {
            when(friendRelationshipRepository.findAllRelevantRelationships(eq(A), any()))
                    .thenReturn(List.of(rel(A, B, Status.ACCEPTED)));

            service.searchUsersAndDetermineStatus("Name", A);

            verify(userMapper).mapToSearchResult(userB, FriendRelationshipStatusEnum.ALREADY_FRIENDS);
        }

        @Test
        @DisplayName("A→B PENDING → PENDING_REQUEST_SENT")
        void aToB_pending_sent() {
            when(friendRelationshipRepository.findAllRelevantRelationships(eq(A), any()))
                    .thenReturn(List.of(rel(A, B, Status.PENDING)));

            service.searchUsersAndDetermineStatus("Name", A);

            verify(userMapper).mapToSearchResult(userB, FriendRelationshipStatusEnum.PENDING_REQUEST_SENT);
        }

        @Test
        @DisplayName("B→A PENDING → PENDING_REQUEST_RECEIVED")
        void bToA_pending_received() {
            when(friendRelationshipRepository.findAllRelevantRelationships(eq(A), any()))
                    .thenReturn(List.of(rel(B, A, Status.PENDING)));

            service.searchUsersAndDetermineStatus("Name", A);

            verify(userMapper).mapToSearchResult(userB, FriendRelationshipStatusEnum.PENDING_REQUEST_RECEIVED);
        }

        @Test
        @DisplayName("A→B BLOCKED → BLOCKED")
        void aBloccaB_blocked() {
            when(friendRelationshipRepository.findAllRelevantRelationships(eq(A), any()))
                    .thenReturn(List.of(rel(A, B, Status.BLOCKED)));

            service.searchUsersAndDetermineStatus("Name", A);

            verify(userMapper).mapToSearchResult(userB, FriendRelationshipStatusEnum.BLOCKED);
        }

        @Test
        @DisplayName("B→A ACCEPTED (B segue A) ma A non segue B → AVAILABLE per A (può ancora seguire B)")
        void bSegueA_aVedeStillAvailable() {
            when(friendRelationshipRepository.findAllRelevantRelationships(eq(A), any()))
                    .thenReturn(List.of(rel(B, A, Status.ACCEPTED)));

            service.searchUsersAndDetermineStatus("Name", A);

            // A deve ancora poter inviare la propria richiesta → AVAILABLE
            verify(userMapper).mapToSearchResult(userB, FriendRelationshipStatusEnum.AVAILABLE);
        }

        @Test
        @DisplayName("Chi mi ha bloccato non appare nei risultati di ricerca")
        void chiMiHaBloccato_nonAppare() {
            FriendRelationshipEty blockRel = rel(B, A, Status.BLOCKED);
            when(friendRelationshipRepository.findByReceiverIdAndStatus(A, Status.BLOCKED))
                    .thenReturn(List.of(blockRel));

            List<?> results = service.searchUsersAndDetermineStatus("Name", A);

            assertThat(results).isEmpty();
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("Utente che ho bloccato non appare nei risultati di ricerca")
        void chioHoBloccato_nonAppare() {
            FriendRelationshipEty blockRel = rel(A, B, Status.BLOCKED);
            when(friendRelationshipRepository.findByRequesterIdAndStatus(A, Status.BLOCKED))
                    .thenReturn(List.of(blockRel));

            List<?> results = service.searchUsersAndDetermineStatus("Name", A);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Se la query non trova utenti → lista vuota senza chiamate al mapper")
        void nessunMatch_listaVuota() {
            when(userRepository.findByNameRegex(anyString())).thenReturn(Collections.emptyList());

            List<?> results = service.searchUsersAndDetermineStatus("xyz", A);

            assertThat(results).isEmpty();
            verifyNoInteractions(userMapper);
        }
    }

    // =========================================================================
    // getPendingRequests
    // =========================================================================

    @Nested
    @DisplayName("getPendingRequests")
    class GetPendingRequests {

        @Test
        @DisplayName("Filtra relazioni con requester null — mappa solo quelle valide")
        void filtraRequesterNull() {
            FriendRelationshipEty valida = rel(A, B, Status.PENDING);
            valida.setRequester(privateUser(A));

            FriendRelationshipEty senzaRequester = rel("ghost", B, Status.PENDING);
            senzaRequester.setRequester(null);

            when(friendRelationshipRepository.findPendingRequestsWithRequester(B, Status.PENDING))
                    .thenReturn(List.of(valida, senzaRequester));

            service.getPendingRequests(B);

            // Solo la relazione con requester valorizzato deve essere mappata
            verify(friendrelationShipMapper, times(1))
                    .mapToFriendRelationshipDto(any(), any(), any());
        }
    }
}
