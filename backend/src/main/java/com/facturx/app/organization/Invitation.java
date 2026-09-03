package com.facturx.app.organization;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitations")
public class Invitation{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status = InvitationStatus.PENDING;

    private String token;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt = LocalDateTime.now();
    
    //empty constructor
    public Invitation() {}

    //getters
    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public InvitationStatus getStatus() { return status; }
    public String getToken() { return token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    //setters
    public void setOrganization (Organization organization) { this.organization = organization; }
    public void setEmail ( String email) { this.email = email; }
    public void setRole(Role role) { this.role = role; }
    public void setStatus(InvitationStatus status) { this.status = status; }
    public void setToken(String token) { this.token = token; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}