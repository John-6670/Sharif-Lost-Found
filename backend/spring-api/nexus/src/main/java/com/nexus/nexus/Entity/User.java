package com.nexus.nexus.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users_user", schema = "auth")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(name = "is_superuser", nullable = false)
    @Builder.Default
    private Boolean isSuperuser = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime registrationDate;

    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
}
