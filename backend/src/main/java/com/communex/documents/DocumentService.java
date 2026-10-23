package com.communex.documents;

import com.communex.common.ActivityLogService;
import com.communex.documents.dto.DocumentRequest;
import com.communex.documents.dto.DocumentResponse;
import com.communex.users.User;
import com.communex.users.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DocumentService {

    private final UserDocumentRepository repo;
    private final UserRepository users;
    private final ActivityLogService activity;

    // Constructor
    public DocumentService(
            UserDocumentRepository repo,
            UserRepository users,
            ActivityLogService activity
    ) {
        this.repo = repo;
        this.users = users;
        this.activity = activity;
    }

    // Retrieve the current user's documents
    @Transactional(readOnly = true)
    public List<DocumentResponse> list(String email) {

        return repo.findByUserEmailOrderByUpdatedAtDesc(email)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    // Create a new document
    @Transactional
    public DocumentResponse create(
            String email,
            DocumentRequest request
    ) {
        UserDocument document = new UserDocument();

        document.setUser(user(email));
        document.setTitle(request.title().trim());

        String content = request.content();

        if (content == null) {
            content = "";
        }

        document.setContent(content);

        document = repo.save(document);

        // Record document creation
        activity.record(
                email,
                "DOCUMENT_CREATED",
                "DOCUMENT",
                document.getId(),
                document.getTitle()
        );

        return DocumentResponse.from(document);
    }

    // Update an existing document owned by the current user
    @Transactional
    public DocumentResponse update(
            Long id,
            String email,
            DocumentRequest request
    ) {
        UserDocument document = repo.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));

        document.setTitle(request.title().trim());

        String content = request.content();

        if (content == null) {
            content = "";
        }

        document.setContent(content);

        document = repo.save(document);

        // Record document update
        activity.record(
                email,
                "DOCUMENT_SAVED",
                "DOCUMENT",
                document.getId(),
                document.getTitle()
        );

        return DocumentResponse.from(document);
    }

    // Delete an existing document owned by the current user
    @Transactional
    public void delete(Long id, String email) {

        UserDocument document = repo.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));

        repo.delete(document);

        // Record document deletion
        activity.record(
                email,
                "DOCUMENT_DELETED",
                "DOCUMENT",
                id,
                document.getTitle()
        );
    }

    // Find a user by email
    private User user(String email) {

        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }
}