package it.voyage.ms.controller.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<UserEty> login(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        String idToken = authorization.substring(7);

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            
            Optional<UserEty> existingUser = userRepository.findById(uid);
            UserEty user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setLastLogin(new Date());
            } else {
                user = new UserEty();
                user.setId(uid);
                user.setDisplayName(decodedToken.getName());
                user.setEmail(decodedToken.getEmail());
                user.setPhotoURL(decodedToken.getPicture());
                user.setCreatedAt(new Date());
                user.setLastLogin(new Date());
            }

            UserEty savedUser = userRepository.save(user);
            return new ResponseEntity<>(savedUser, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }
}