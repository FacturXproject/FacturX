package com.facturx.app.publicapi;

import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.OrganizationRepository;
import com.facturx.app.permission.AccessDeniedException;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    static final String KEY_PREFIX = "fxk_";
    private static final int KEY_RANDOM_BYTES = 32;
    private static final int DISPLAYED_PREFIX_LENGTH = 12;

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         OrganizationMemberRepository memberRepository,
                         OrganizationRepository organizationRepository,
                         UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    public ApiKeyCreatedResponse create(Long userId, String name, Long organizationId, Set<ApiKeyScope> scopes) {
        // A key acts on behalf of its owner inside one organization, so the owner has
        // to belong to it. What the key can then do is the owner's role, narrowed by scopes.
        memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(AccessDeniedException::new);

        byte[] random = new byte[KEY_RANDOM_BYTES];
        secureRandom.nextBytes(random);
        String rawKey = KEY_PREFIX + HexFormat.of().formatHex(random);

        ApiKey apiKey = new ApiKey();
        apiKey.setUserId(userId);
        apiKey.setOrganizationId(organizationId);
        apiKey.setName(name.trim());
        apiKey.setKeyHash(hash(rawKey));
        apiKey.setKeyPrefix(rawKey.substring(0, DISPLAYED_PREFIX_LENGTH));
        apiKey.setScopes(scopes);
        apiKeyRepository.save(apiKey);

        return new ApiKeyCreatedResponse(rawKey, toResponse(apiKey));
    }

    public List<ApiKeyResponse> listMine(Long userId) {
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Revoking is a soft delete: the row stays so the owner keeps a trace of the key.
    public void revoke(Long keyId, Long userId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndUserId(keyId, userId)
                .orElseThrow(ApiKeyNotFoundException::new);

        if (!apiKey.isRevoked()) {
            apiKey.setRevokedAt(Instant.now());
            apiKeyRepository.save(apiKey);
        }
    }

    /**
     * Resolves the key sent by a client. Empty for an unknown key, a revoked key, or a
     * key whose owner's account is gone or disabled - the caller answers 401 for all
     * three without saying which.
     */
    public Optional<ApiKeyPrincipal> authenticate(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        Optional<ApiKey> found = apiKeyRepository.findByKeyHash(hash(rawKey));
        if (found.isEmpty() || found.get().isRevoked()) {
            return Optional.empty();
        }

        ApiKey apiKey = found.get();
        Optional<User> owner = userRepository.findById(apiKey.getUserId());
        if (owner.isEmpty() || !owner.get().isActive()) {
            return Optional.empty();
        }

        apiKeyRepository.touchLastUsedAt(apiKey.getId(), Instant.now());

        return Optional.of(new ApiKeyPrincipal(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getOrganizationId(),
                apiKey.getScopes()));
    }

    private ApiKeyResponse toResponse(ApiKey apiKey) {
        String organizationName = organizationRepository.findById(apiKey.getOrganizationId())
                .map(Organization::getName)
                .orElse(null);
        return ApiKeyResponse.from(apiKey, organizationName);
    }

    // Keys are 256 bits of randomness, so a plain SHA-256 is enough here - a slow
    // password hash (BCrypt) protects low-entropy secrets, which a key is not.
    private static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
