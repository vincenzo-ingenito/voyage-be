package it.voyage.ms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AttachmentDTO {
	private String name;
	private String type; // 'image' | 'document'
	private String mimeType;
	@JsonProperty("fileIndex") 
	private Integer fileIndex;

}