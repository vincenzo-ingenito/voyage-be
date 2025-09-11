/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.voyage.ms.controller.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.voyage.ms.dto.response.ErrorResponseDTO;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 *	Exceptions Handler.
 */
@ControllerAdvice
@Slf4j
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {NotFoundException.class})
    protected ResponseEntity<ErrorResponseDTO> handleItemsNotFoundException(final NotFoundException ex, final WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        
    	return new ResponseEntity<>(new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), "Configuration items not found"), headers, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {BusinessException.class})
    protected ResponseEntity<ErrorResponseDTO> handleGenericException(final BusinessException ex, final WebRequest request) {
        log.error("Internal server error." , ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        
    	return new ResponseEntity<>(new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error"), headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {ValidationException.class})
    protected ResponseEntity<ErrorResponseDTO> handleValidationException(final ValidationException ex, final WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    	return new ResponseEntity<>(new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), "Bad request"), headers, HttpStatus.BAD_REQUEST);
    }

}