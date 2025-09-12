package it.voyage.ms.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Base response.
 */
@Getter
@Setter
@NoArgsConstructor
public class BaseResponse {

	/**
	 * Max size span id.
	 */
	private static final int MAX_SIZE_SPAN_ID = 100;

	/**
	 * Max size trace id.
	 */
	private static final int MAX_SIZE_TRACE_ID = 100;

	/**
	 * Max size problem title.
	 */
	private static final int MAX_SIZE_TITLE = 1000;

	/**
	 * Max size problem type.
	 */
	private static final int MAX_SIZE_TYPE = 100;

	/**
	 * Max size problem instance.
	 */
	private static final int MAX_SIZE_INSTANCE = 1000;

	
	/**
	 * Trace id log.
	 */
	@Pattern(regexp = ".*")
	@Size(min = 0, max = MAX_SIZE_TRACE_ID)
	private String traceId;

	/**
	 * Span id log.
	 */
	@Pattern(regexp = ".*")
	@Size(min = 0, max = MAX_SIZE_SPAN_ID)
	private String spanId;
	
	/**
	 * Span id log.
	 */
	@Pattern(regexp = ".*")
	private int statusCode;

	/**
	 * Type.
	 */
	@Schema(description = "Identificativo del problema verificatosi")
	@Size(min = 0, max = MAX_SIZE_TYPE)
	@Pattern(regexp = ".*")
	private String type;
	
	/**
	 * Title.
	 */
	@Schema(description = "Sintesi del problema (invariante per occorrenze diverse dello stesso problema)")
	@Size(min = 0, max = MAX_SIZE_TITLE)
	@Pattern(regexp = ".*")
	private String title;

	/**
	 * Detail.
	 */
	@Schema(description = "Descrizione del problema")
	@Size(min = 0, max = MAX_SIZE_INSTANCE)
	@Pattern(regexp = ".*")
	private String detail;
 
	/**
	 * Instance.
	 */
	@Schema(description = "URI che potrebbe fornire ulteriori informazioni riguardo l'occorrenza del problema")
	@Size(min = 0, max = MAX_SIZE_INSTANCE)
	@Pattern(regexp = ".*")
	private String instance;
	
}