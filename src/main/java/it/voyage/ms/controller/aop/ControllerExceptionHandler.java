package it.voyage.ms.controller.aop;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.voyage.ms.enums.ErrorClassEnum;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.exceptions.ValidationException;
import it.voyage.ms.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;

/**
 *	Exceptions Handler.
 */
@Slf4j
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

	private ResponseEntity<BaseResponse> getErrorResponse(ErrorClassEnum errorEnum, final Exception e) {
		BaseResponse br = new BaseResponse();

		if(e instanceof BusinessException) {
			BusinessException b = (BusinessException)e;
			if(b.getError() != null) {
				br = b.getError();
				return new ResponseEntity<>(br, errorEnum.getStatus());

			}
		} 

		//Setting RFC7807 info
		br.setType(errorEnum.getType());
		br.setTitle(errorEnum.getTitle());
		br.setInstance(errorEnum.getInstance());

		String detail = e.getMessage();
		if (StringUtils.isAllBlank(detail)) {
			detail = errorEnum.getDefaultDetail();
		}

		br.setDetail(detail);

		return new ResponseEntity<>(br, errorEnum.getStatus());
	} 
 
	@ExceptionHandler(value = {BusinessException.class, Exception.class})
	protected ResponseEntity<BaseResponse> handleGenericException(final Exception ex, final WebRequest request) {
		log.error("handleGenericException", ex);
		return getErrorResponse(ErrorClassEnum.GENERIC, ex);//500 SERVER ERROR/ISR
	}
	
	@ExceptionHandler(value = {ValidationException.class})
	protected ResponseEntity<BaseResponse> handleValidationException(final ValidationException ex, final WebRequest request) {
		log.error("handleValidationException", ex);
		return getErrorResponse(ErrorClassEnum.VALIDATION, ex);//400 VALIDATION
	}
	
	@ExceptionHandler(value = {NotFoundException.class})
	protected ResponseEntity<BaseResponse> handleValidationException(final NotFoundException ex, final WebRequest request) {
		log.error("handleNotFOundException", ex);
		return getErrorResponse(ErrorClassEnum.NOT_FOUND, ex);//404 NOT FOUND
	}

}