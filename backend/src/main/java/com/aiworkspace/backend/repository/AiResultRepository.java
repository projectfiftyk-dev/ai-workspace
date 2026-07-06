package com.aiworkspace.backend.repository;

import com.aiworkspace.backend.model.AiResult;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiResultRepository extends MongoRepository<AiResult, String> {
}
