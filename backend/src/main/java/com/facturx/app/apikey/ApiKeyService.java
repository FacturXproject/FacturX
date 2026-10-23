package com.facturx.app.apikey;

import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.permission.AccessDeniedException;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F17 : creation, liste, revocation et verification des cles API.
 *
 * Format d'une cle : "fxk_" + 32 octets aleatoires en base64url (43 caracteres).
 */
@Service
public class ApiKeyService {

    public static final String KEY_PREFIX = "fxk_";
    private static final int SECRET_BYTES = 32;
    private static final int DISPLAY_PREFIX_LENGTH = KEY_PREFIX.length() + 8;
    // lastUsedAt n'est reecrit qu'une fois par minute : pas une ecriture en base par requete.
    private static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(1);

    private final SecureRandom random = new SecureRandom();
    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ApiKeyService(
            ApiKeyRepository apiKeyRepository,
            OrganizationMemberRepository memberRepository,
            UserRepository userRepository) {

        this.apiKeyRepository = apiKeyRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    /** Le resultat d'une creation : la cle enregistree et son secret, a montrer une seule fois. */
    public record CreatedApiKey(ApiKey apiKey, String secret) {
    }

    @Transactional
    public CreatedApiKey create(Long userId, ApiKeyCreateRequest request) {
        // Seul un membre de l'organisation peut y creer une cle.
        Organization organization = memberRepository
                .findByUserIdAndOrganizationId(userId, request.organizationId())
                .orElseThrow(AccessDeniedException::new)
                .getOrganization();

        User owner = userRepository.findById(userId)
                .orElseThrow(AccessDeniedException::new);

        String secret = generateSecret();

        ApiKey apiKey = new ApiKey();
        apiKey.setOrganization(organization);
        apiKey.setOwner(owner);
        apiKey.setName(request.name().trim());
        apiKey.setPrefix(secret.substring(0, DISPLAY_PREFIX_LENGTH));
        apiKey.setKeyHash(hash(secret));
        apiKey.setScopes(request.scopes());

        return new CreatedApiKey(apiKeyRepository.save(apiKey), secret);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listForUser(Long userId) {
        return apiKeyRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
    }

    /** Revoque une cle de l'utilisateur. Idempotent ; 404 si la cle n'est pas a lui. */
    @Transactional
    public ApiKey revoke(Long userId, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .filter(key -> key.getOwner().getId().equals(userId))
                .orElseThrow(ApiKeyNotFoundException::new);

        if (!apiKey.isRevoked()) {
            apiKey.setRevokedAt(Instant.now());
        }
        return apiKey;
    }

    /**
     * Retrouve la cle active correspondant au secret presente. Vide si la cle est
     * inconnue, revoquee, si son proprietaire est desactive ou n'est plus membre de
     * l'organisation de la cle.
     */
    @Transactional
    public Optional<ApiKeyPrincipal> authenticate(String secret) {
        if (secret == null || !secret.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        return apiKeyRepository.findByKeyHash(hash(secret))
                .filter(key -> !key.isRevoked())
                .filter(key -> key.getOwner().isActive())
                .filter(key -> memberRepository.findByUserIdAndOrganizationId(
                        key.getOwner().getId(), key.getOrganization().getId()).isPresent())
                .map(key -> {
                    markUsed(key);
                    return ApiKeyPrincipal.from(key);
                });
    }

    private void markUsed(ApiKey apiKey) {
        Instant now = Instant.now();
        if (apiKey.getLastUsedAt() == null
                || apiKey.getLastUsedAt().isBefore(now.minus(LAST_USED_RESOLUTION))) {
            apiKey.setLastUsedAt(now);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
