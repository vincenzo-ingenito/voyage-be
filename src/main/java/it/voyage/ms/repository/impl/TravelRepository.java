package it.voyage.ms.repository.impl;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.TravelEty;

@Repository
public interface TravelRepository extends MongoRepository<TravelEty, String> {

	List<TravelEty> findByUserId(String userId);
	long deleteByIdAndUserId(String id, String userId);
}