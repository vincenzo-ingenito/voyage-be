package it.voyage.ms.controller.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import it.voyage.ms.dto.response.LogTraceInfoDto;
import it.voyage.ms.response.BaseResponse;

import static it.voyage.ms.config.Constants.Properties.MS_NAME;

@Aspect
@Component
public class ResponseEnricher {
	
	/**
	 * Tracker instance.
	 */
	private Tracer tracer;

    @Autowired
	public ResponseEnricher(Tracer inTracer) {
		tracer = inTracer;
	}
	
	@Pointcut("@annotation(org.springframework.web.bind.annotation.ExceptionHandler)")
	public void exceptionHandlerMethods() {}


	@AfterReturning(pointcut = "execution(* it.voyage.ms.controller.*.*(..)) || (exceptionHandlerMethods())", returning = "response")
	public void enrichResponse(ResponseEntity<? extends BaseResponse> response) {
		LogTraceInfoDto dto = getLogTraceInfo();
		BaseResponse res = response.getBody();
		if (res != null) {
			res.setSpanId(dto.spanID());
			res.setTraceId(dto.traceID());
			res.setStatusCode(response.getStatusCode().value());
		}
	}

	/**
	 * Retrieve tracer info.
	 * 
	 * @return	 tracer info
	 */ 
	protected LogTraceInfoDto getLogTraceInfo() {
		LogTraceInfoDto out = new LogTraceInfoDto(null, null);
		SpanBuilder spanbuilder = tracer.spanBuilder(MS_NAME);
		
		if (spanbuilder != null) {
			out = new LogTraceInfoDto(
					spanbuilder.startSpan().getSpanContext().getSpanId(), 
					spanbuilder.startSpan().getSpanContext().getTraceId());
		}
		return out;
	}

	 
}
