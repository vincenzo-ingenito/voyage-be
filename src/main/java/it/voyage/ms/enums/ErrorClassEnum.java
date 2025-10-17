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
	GENERIC("/errors", "Generic Error", "Generic Error", "/generic", HttpStatus.INTERNAL_SERVER_ERROR),
	VALIDATION("/errors/validation", "Validation", "Validation failure", "/validation", HttpStatus.BAD_REQUEST),
	NOT_FOUND("/errors/not-found", "Missing", "Record not present", "/not-found", HttpStatus.NOT_FOUND),
	ACCESS_DENIED("/errors/access-denied", "Access denied", "Access denied", "/access-denied", HttpStatus.FORBIDDEN);

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