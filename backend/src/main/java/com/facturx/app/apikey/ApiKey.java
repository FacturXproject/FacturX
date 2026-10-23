package com.facturx.app.apikey;

import com.facturx.app.organization.Organization;
import com.facturx.app.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * F17 : cle d'acces a l'API publique. Une cle appartient a un utilisateur et agit dans
 * UNE organisation, avec les permissions de son proprietaire limitees par ses scopes.
 *
 * Le secret n'est jamais stocke : seulement son empreinte SHA-256 (keyHash). Le secret
 * est aleatoire sur 256 bits, un hash lent (BCrypt) n'apporterait rien ici et empecherait
 * de retrouver la cle par son empreinte. "prefix" (les premiers caracteres) sert
 * uniquement a reconnaitre une cle dans l'interface.
 */
@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "ux_api_keys_key_hash", columnList = "key_hash", unique = true),
        @Index(name = "ix_api_keys_owner_id", columnList = "owner_id")
})
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Supprimer l'organisation (ou le compte) supprime ses cles, cote base.
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 16)
    private String prefix;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Convert(converter = ApiKeyScopesConverter.class)
    @Column(nullable = false, length = 255)
    private Set<ApiKeyScope> scopes = EnumSet.noneOf(ApiKeyScope.class);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public ApiKey() {}

    public boolean isRevoked() { return revokedAt != null; }

    // getters
    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public String getPrefix() { return prefix; }
    public String getKeyHash() { return keyHash; }
    public Set<ApiKeyScope> getScopes() { return scopes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getRevokedAt() { return revokedAt; }

    // setters
    public void setOrganization(Organization organization) { this.organization = organization; }
    public void setOwner(User owner) { this.owner = owner; }
    public void setName(String name) { this.name = name; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public void setScopes(Set<ApiKeyScope> scopes) { this.scopes = EnumSet.copyOf(scopes); }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
