package it.voyage.ms.exceptions;

import java.io.Serial;

import it.voyage.ms.response.BaseResponse;
import lombok.Getter;

/**
 * Conflict exception.
 */
public class ConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    

	@Getter
	private BaseResponse error;

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictException(Throwable cause) {
        super(cause);
    }
    

	public ConflictException(BaseResponse inError) {
		error = inError;
	}

}