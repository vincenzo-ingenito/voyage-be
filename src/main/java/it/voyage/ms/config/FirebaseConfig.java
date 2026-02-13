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
    public FirebaseApp firebaseApp() throws IOException {
        FileInputStream serviceAccount = new FileInputStream(pathServiceAccount);

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setStorageBucket(storageBucketName)  
            .build();

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        } else {
            return FirebaseApp.getInstance();
        }
    }

    @Bean
    public Storage storage() throws IOException {
    	 FileInputStream serviceAccount = new FileInputStream(pathServiceAccount);
        return StorageOptions.newBuilder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build().getService();
    }
}