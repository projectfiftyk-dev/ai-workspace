package com.aiworkspace.backend.repository;

import com.aiworkspace.backend.model.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrganizationRepository extends MongoRepository<Organization, String> {

    Optional<Organization> findByNameIgnoreCase(String name);
}
