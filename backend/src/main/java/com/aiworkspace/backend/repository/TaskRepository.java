package com.aiworkspace.backend.repository;

import com.aiworkspace.backend.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskRepository extends MongoRepository<Task, String> {
}
