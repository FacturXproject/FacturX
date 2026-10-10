package com.facturx.app.publicapi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * F17: an API key lets a technical client call the public API without a browser
 * session. The key itself is never stored - only its SHA-256 hash (see
 * {@link ApiKeyService}), so a database leak does not leak usable keys.
 *
 * userId / organizationId are plain ids on purpose (same choice as
 * ValidationRun.documentId): no foreign key, so deleting an organization or removing
 * a member keeps working exactly as before. A key whose owner is no longer a member
 * simply stops having any permission - the role is re-checked on every request.
 */
@Entity
@Table(name = "api_keys", uniqueConstraints = @UniqueConstraint(name = "ux_api_keys_key_hash", columnNames = "key_hash"))
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 100)
    private String name;

    // SHA-256 of the full key, hex-encoded (always 64 chars).
    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    // First characters of the key, kept in clear so the owner can tell their keys apart.
    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    // Comma-separated ApiKeyScope values, e.g. "documents:read,documents:write".
    @Column(nullable = false, length = 100)
    private String scopes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public ApiKey() {}

    // getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public String getKeyHash() { return keyHash; }
    public String getKeyPrefix() { return keyPrefix; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getRevokedAt() { return revokedAt; }

    public Set<ApiKeyScope> getScopes() {
        return Arrays.stream(scopes.split(","))
                .map(ApiKeyScope::fromValue)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ApiKeyScope.class)));
    }

    public boolean isRevoked() { return revokedAt != null; }

    // setters
    public void setUserId(Long userId) { this.userId = userId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public void setName(String name) { this.name = name; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public void setScopes(Set<ApiKeyScope> scopes) {
        this.scopes = scopes.stream()
                .sorted()
                .map(ApiKeyScope::getValue)
                .collect(Collectors.joining(","));
    }
}
