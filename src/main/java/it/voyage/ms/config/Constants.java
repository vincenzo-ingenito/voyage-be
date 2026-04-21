package it.voyage.ms.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants application.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {
 
	
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Profile {

		/**
		 * Test profile.
		 */
		public static final String TEST = "test";

		/**
		 * Dev profile.
		 */
		public static final String DEV = "dev";
 
	}
	
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Properties {

		public static final String MS_NAME = "voyage";
 
	}
	
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Google {

		public static final String PLACES_AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
		public static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";
		public static final String PLACES_NEARBY_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
		public static final int NEARBY_DEFAULT_RADIUS = 1500;
		public static final int NEARBY_MAX_RADIUS = 5000;
		public static final int NEARBY_MAX_RESULTS = 20;
 
	}

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Optimizer {

		public static final int OPTIMIZER_MAX_POINTS_PER_DAY = 20;
 
	}


}
