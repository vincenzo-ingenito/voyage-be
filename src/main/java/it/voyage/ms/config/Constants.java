package it.voyage.ms.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants application.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

 
	public static final class Collections {

		public static final String CONFIG_DATA = "config_data";

		private Collections() {

		}
	}

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


}
