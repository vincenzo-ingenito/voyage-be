package it.voyage.ms.repository.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entità che rappresenta un partecipante a un viaggio di gruppo.
 * Gestisce inviti, ruoli (VIEWER/EDITOR) e stato dell'invito.
 */
@Entity
@Table(
		name = "travel_participants",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_travel_user",
						columnNames = {"travel_id", "user_id"}
						)
		},
		indexes = {
				@Index(name = "idx_participant_travel_id", columnList = "travel_id"),
				@Index(name = "idx_participant_user_id", columnList = "user_id"),
				@Index(name = "idx_participant_status", columnList = "status")
		}
		)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelParticipantEty {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * Viaggio a cui il partecipante è invitato
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "travel_id",
			nullable = false,
			foreignKey = @ForeignKey(name = "fk_participant_travel")
			)
	private TravelEty travel;

	/**
	 * ID utente partecipante (Firebase UID)
	 */
	@Column(name = "user_id", nullable = false, length = 255)
	private String userId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * Utente partecipante (relazione opzionale per fetch)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "user_id",
			insertable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_participant_user")
			)
	private UserEty user;

	/**
	 * Ruolo del partecipante nel viaggio
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 50)
	private ParticipantRole role;

	/**
	 * Stato dell'invito
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 50)
	private ParticipantStatus status = ParticipantStatus.PENDING;

	/**
	 * ID utente che ha inviato l'invito (Firebase UID)
	 * Tipicamente è l'owner del viaggio
	 */
	@Column(name = "invited_by", nullable = false, length = 255)
	private String invitedBy;

	/**
	 * Timestamp dell'invito
	 */
	@Column(name = "invited_at", nullable = false)
	private LocalDateTime invitedAt;

	/**
	 * Timestamp della risposta all'invito (accettato/rifiutato)
	 */
	@Column(name = "responded_at")
	private LocalDateTime respondedAt;

	/**
	 * Flag che indica se l'utente può modificare il viaggio
	 * Calcolato automaticamente dal role:
	 * - EDITOR -> can_edit = true
	 * - VIEWER -> can_edit = false
	 */
//	@Column(name = "can_edit", nullable = false)
//	private Boolean canEdit;

	/**
	 * Flag che indica se l'utente può invitare altri partecipanti
	 * Solo l'owner del viaggio può invitare, quindi per i partecipanti è sempre false
	 */
	@Column(name = "can_invite", nullable = false)
	private Boolean canInvite = false;

	@PrePersist
	protected void onCreate() {

		if (createdAt == null) {
			createdAt = LocalDateTime.now();   // <-- aggiunto
		}

		if (invitedAt == null) {
			invitedAt = LocalDateTime.now();
		}
		if (status == null) {
			status = ParticipantStatus.PENDING;
		}
		// Calcola can_edit dal role
//		updateCanEdit();
	}

	/**
	 * Aggiorna can_edit quando cambia il role
	 */
//	@jakarta.persistence.PreUpdate
//	protected void onUpdate() {
//		updateCanEdit();
//	}

	/**
	 * Metodo helper per calcolare can_edit dal role
	 */
//	private void updateCanEdit() {
//		if (role != null) {
//			canEdit = (role == ParticipantRole.EDITOR);
//		}
//	}
	
	@Transient
	public boolean isCanEdit() {
	    return role == ParticipantRole.EDITOR;
	}
}
