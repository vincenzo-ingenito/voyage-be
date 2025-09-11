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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import it.voyage.ms.dto.AbstractDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * The Class ErrorResponseDTO.
 * 
 * 	Error response.
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ErrorResponseDTO extends AbstractDTO {

	/**
	 * Codice.
	 */
	@Schema(description = "Codice di errore")
	@Min(value = 100)
	@Max(value = 599)
	private final Integer code;
	
	/**
	 * Messaggio.
	 */
	@Schema(description = "Messaggio di errore")
	@Size(min = 0, max = 1000)
	private final String message;

}
