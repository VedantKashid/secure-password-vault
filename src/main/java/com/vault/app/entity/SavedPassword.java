package com.vault.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_passwords")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String platform; // e.g., Netflix, GitHub, Bank

    @Column(nullable = false)
    private String loginUsername; // The username for that specific platform

    @Column(nullable = false)
    private String encryptedPassword; // The password we will encrypt before saving

    // This creates the Foreign Key linking this password to a specific User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private Boolean isBreached = false;
}