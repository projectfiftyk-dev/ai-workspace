package com.aiworkspace.backend.repository;

import com.aiworkspace.backend.model.Source;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SourceRepository extends MongoRepository<Source, String> {
}
