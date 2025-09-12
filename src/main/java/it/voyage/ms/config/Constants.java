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


}
