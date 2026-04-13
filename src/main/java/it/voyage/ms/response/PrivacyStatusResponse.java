package it.voyage.ms.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrivacyStatusResponse {

	private boolean privateProfile;
	private boolean showEmergencyFAB;
}
