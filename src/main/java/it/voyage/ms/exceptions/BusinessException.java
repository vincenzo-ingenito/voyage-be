package it.voyage.ms.exceptions;

import java.io.Serial;

import it.voyage.ms.response.BaseResponse;
import lombok.Getter;

/**
 * Generic business exception.
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    

	@Getter
	private BaseResponse error;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }
    

	public BusinessException(BaseResponse inError) {
		error = inError;
	}

}