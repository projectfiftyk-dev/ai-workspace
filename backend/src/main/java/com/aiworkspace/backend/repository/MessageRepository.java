package com.aiworkspace.backend.repository;

import com.aiworkspace.backend.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findBySourceIdOrderBySentAtAsc(String sourceId);
}
