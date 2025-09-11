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
package it.voyage.ms.dto.response;

import it.voyage.ms.dto.AbstractDTO;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 *	Base response.
 */
@Getter
@Setter
public class ResponseDTO extends AbstractDTO {

	/**
	 * Trace id log.
	 */
	@Size(min = 0, max = 100)
	private String traceID;
	
	/**
	 * Span id log.
	 */
	@Size(min = 0, max = 100)
	private String spanID;
	
	/**
	 * Error.
	 */
	private ErrorResponseDTO error;

	/**
	 * Instantiates a new response DTO.
	 */
	public ResponseDTO() {
	}

	/**
	 * Instantiates a new response DTO.
	 *
	 * @param traceInfo the trace info
	 */
	public ResponseDTO(final LogTraceInfoDTO traceInfo) {
		traceID = traceInfo.getTraceID();
		spanID = traceInfo.getSpanID(); 
	}
	
	/**
	 * Instantiates a new response DTO.
	 *
	 * @param traceInfo the trace info
	 * @param errorCode the error code
	 * @param errorMsg the error msg
	 */
	public ResponseDTO(final LogTraceInfoDTO traceInfo, final Integer errorCode, final String errorMsg) {
		traceID = traceInfo.getTraceID();
		spanID = traceInfo.getSpanID();
		error = new ErrorResponseDTO(errorCode, errorMsg); 
	}
	
}
