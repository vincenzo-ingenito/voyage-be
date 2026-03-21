package it.voyage.ms.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage; // <-- NUOVA IMPORT
import com.google.cloud.storage.StorageOptions; // <-- NUOVA IMPORT
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account.path}")
    private String pathServiceAccount;

    @Value("${firebase.storage.bucket-name}")
    private String storageBucketName;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try (FileInputStream serviceAccount = new FileInputStream(pathServiceAccount)) {
            return GoogleCredentials.fromStream(serviceAccount);
        }
    }

    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setStorageBucket(storageBucketName)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public Storage storage(GoogleCredentials credentials) throws IOException {
        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }
}