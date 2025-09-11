package it.voyage.ms.repository.impl;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.UserEty;

@Repository
public interface UserRepository extends MongoRepository<UserEty, String> {
    Optional<UserEty> findByEmail(String email);
}