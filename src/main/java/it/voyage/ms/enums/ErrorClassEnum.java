package it.voyage.ms.enums;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorClassEnum {

	/**
	 * Generic class error.
	 */
	GENERIC("/errors", "Generic", "Errore generico", "/generic", HttpStatus.INTERNAL_SERVER_ERROR),
	VALIDATION("/errors/validation", "Validation", "Validazione fallita", "/validation", HttpStatus.BAD_REQUEST),
	NOT_FOUND("/errors/not-found", "Missing", "Record non presente", "/not-found", HttpStatus.NOT_FOUND);

	/**
	 * Error type.
	 */
	private final String type;

	/**
	 * Error title, user friendly description.
	 */
	private final String title;

	/**
	 * Error detail, developer friendly description.
	 */
	private final String defaultDetail;

	/**
	 * Error instance, URI that identifies the specific occurrence of the problem.
	 */
	private final String instance;

	private final HttpStatus status;

}