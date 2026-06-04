package com.vault.app.repository;

import com.vault.app.entity.SavedPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPasswordRepository extends JpaRepository<SavedPassword, Long> {
    // Custom method to instantly find all passwords belonging to one specific user
    List<SavedPassword> findByUserId(Long userId);
    // Ensures we only fetch a password if it belongs to the specific user requesting it
    Optional<SavedPassword> findByIdAndUserId(Long id, Long userId);
}