package com.communex.documents;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDocumentRepository
        extends JpaRepository<UserDocument, Long> {

    // Retrieve a user's documents, ordered by most recently updated
    List<UserDocument> findByUserEmailOrderByUpdatedAtDesc(
            String email
    );

    // Find a document by its ID and owner email
    Optional<UserDocument> findByIdAndUserEmail(
            Long id,
            String email
    );
}