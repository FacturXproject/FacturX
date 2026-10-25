package com.facturx.app.gdpr;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.apikey.ApiKey;
import com.facturx.app.apikey.ApiKeyRepository;
import com.facturx.app.apikey.ApiKeyScope;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.document.DocumentStatus;
import com.facturx.app.organization.Invitation;
import com.facturx.app.organization.InvitationRepository;
import com.facturx.app.organization.InvitationStatus;
import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.OrganizationRepository;
import com.facturx.app.organization.Role;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;

import jakarta.servlet.http.Cookie;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@ActiveProfiles("test")
class GdprFlowTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMemberRepository memberRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private ApiKeyRepository apiKeyRepository;

	@Autowired
	private InvitationRepository invitationRepository;

	private static String uniqueEmail(String label) {
			return label + "-" + UUID.randomUUID() + "@x.fr";
	}

	private static String uniqueKeyHash() {
			return UUID.randomUUID().toString().replace("-", "")
							+ UUID.randomUUID().toString().replace("-", "");
	}

	private static String registerBody(
					String email,
					String password) {

			return """
							{
									"email":"%s",
									"password":"%s",
									"firstName":"Jean",
									"lastName":"Dupont"
							}
							""".formatted(email, password);
	}

	private RegisteredUser register(
					String label) throws Exception {

			String email = uniqueEmail(label);
			String password = "correcthorsebattery";

			MvcResult result = mockMvc.perform(
											post("/api/auth/register")
															.with(csrf())
															.contentType(MediaType.APPLICATION_JSON)
															.content(registerBody(email, password))
							)
							.andExpect(status().isCreated())
							.andReturn();

			Cookie session =
							result.getResponse()
											.getCookie("EFACTURE_SESSION");

			assertThat(session).isNotNull();

			Long userId = userRepository
							.findByEmail(email)
							.orElseThrow()
							.getId();

			return new RegisteredUser(
							userId,
							email,
							password,
							session
			);
	}

	private long createOrganization(
					Cookie session) throws Exception {

			MvcResult result = mockMvc.perform(
											post("/api/organizations?name=Cabinet Test")
															.with(csrf())
															.cookie(session)
							)
							.andExpect(status().isOk())
							.andReturn();

			return new ObjectMapper()
							.readTree(
											result.getResponse()
															.getContentAsString()
							)
							.get("id")
							.asLong();
	}

	@Test
	void exportContainsAllUserDataWithoutSensitiveSecrets()
					throws Exception {

			RegisteredUser user =
							register("gdpr-export");

			long organizationId =
							createOrganization(user.session());

			Organization organization =
							organizationRepository
											.findById(organizationId)
											.orElseThrow();

			User storedUser =
							userRepository
											.findById(user.id())
											.orElseThrow();

			Document document = new Document();

			document.setOrganization(organization);
			document.setOwner(storedUser);
			document.setFilename("facture-export.pdf");
			document.setType("application/pdf");
			document.setSize(250);
			document.setStoragePath(
							"/tmp/gdpr-export-document.pdf"
			);
			document.setStatus(DocumentStatus.UPLOADED);

			documentRepository.save(document);

			ApiKey apiKey = new ApiKey();

			apiKey.setOrganization(organization);
			apiKey.setOwner(storedUser);
			apiKey.setName("Integration comptable");
			apiKey.setPrefix("ef_test_1234");
			apiKey.setKeyHash(uniqueKeyHash());
			apiKey.setScopes(
							EnumSet.of(ApiKeyScope.DOCUMENTS_READ)
			);

			apiKeyRepository.save(apiKey);

			Invitation invitation = new Invitation();

			invitation.setOrganization(organization);
			invitation.setEmail(user.email());
			invitation.setRole(Role.CLIENT);
			invitation.setStatus(InvitationStatus.PENDING);
			invitation.setToken(UUID.randomUUID().toString());
			invitation.setExpiresAt(
							LocalDateTime.now().plusDays(7)
			);

			invitationRepository.save(invitation);

			mockMvc.perform(
											get("/api/gdpr/export")
															.cookie(user.session())
							)
							.andExpect(status().isOk())

							// User
							.andExpect(
											jsonPath("$.user.id")
															.value(user.id())
							)
							.andExpect(
											jsonPath("$.user.email")
															.value(user.email())
							)
							.andExpect(
											jsonPath("$.user.firstName")
															.value("Jean")
							)
							.andExpect(
											jsonPath("$.user.lastName")
															.value("Dupont")
							)
							.andExpect(
											jsonPath("$.user.status")
															.value("active")
							)

							// Organizations
							.andExpect(
											jsonPath("$.organizations[0].id")
															.value(organizationId)
							)
							.andExpect(
											jsonPath("$.organizations[0].role")
															.value("ADMIN")
							)

							// Documents
							.andExpect(
											jsonPath("$.documents[0].organizationId")
															.value(organizationId)
							)
							.andExpect(
											jsonPath("$.documents[0].filename")
															.value("facture-export.pdf")
							)
							.andExpect(
											jsonPath("$.documents[0].type")
															.value("application/pdf")
							)
							.andExpect(
											jsonPath("$.documents[0].size")
															.value(250)
							)
							.andExpect(
											jsonPath("$.documents[0].status")
															.value("UPLOADED")
							)

							// Internal storage path is not exported
							.andExpect(
											jsonPath("$.documents[0].storagePath")
															.doesNotExist()
							)

							// API keys
							.andExpect(
											jsonPath("$.apiKeys[0].organizationId")
															.value(organizationId)
							)
							.andExpect(
											jsonPath("$.apiKeys[0].name")
															.value("Integration comptable")
							)
							.andExpect(
											jsonPath("$.apiKeys[0].prefix")
															.value("ef_test_1234")
							)
							.andExpect(
											jsonPath("$.apiKeys[0].scopes[0]")
															.value("DOCUMENTS_READ")
							)

							// The API key hash must never leave the backend
							.andExpect(
											jsonPath("$.apiKeys[0].keyHash")
															.doesNotExist()
							)

							// Invitations
							.andExpect(
											jsonPath("$.invitations[0].organizationId")
															.value(organizationId)
							)
							.andExpect(
											jsonPath("$.invitations[0].email")
															.value(user.email())
							)
							.andExpect(
											jsonPath("$.invitations[0].role")
															.value("CLIENT")
							)
							.andExpect(
											jsonPath("$.invitations[0].status")
															.value("PENDING")
							)

							// Invitation token is sensitive and must not be exported
							.andExpect(
											jsonPath("$.invitations[0].token")
															.doesNotExist()
							);
	}

	@Test
	void deletionRequiresExactConfirmation()
					throws Exception {

			RegisteredUser user =
							register("gdpr-confirmation");

			mockMvc.perform(
											post("/api/gdpr/delete")
															.with(csrf())
															.cookie(user.session())
															.contentType(MediaType.APPLICATION_JSON)
															.content("""
																			{
																					"confirmation":"NO"
																			}
																			""")
							)
							.andExpect(status().isBadRequest())
							.andExpect(
											jsonPath("$.error")
															.value("INVALID_CONFIRMATION")
							);

			User storedUser =
							userRepository.findById(user.id())
											.orElseThrow();

			assertThat(storedUser.getStatus())
							.isEqualTo(User.STATUS_ACTIVE);

			assertThat(storedUser.getEmail())
							.isEqualTo(user.email());
	}

	@Test
	void lastAdminCannotDeleteAccount()
					throws Exception {

			RegisteredUser user =
							register("gdpr-last-admin");

			createOrganization(user.session());

			mockMvc.perform(
											post("/api/gdpr/delete")
															.with(csrf())
															.cookie(user.session())
															.contentType(MediaType.APPLICATION_JSON)
															.content("""
																			{
																					"confirmation":"SUPPRIMER"
																			}
																			""")
							)
							.andExpect(status().isConflict())
							.andExpect(
											jsonPath("$.error")
															.value("LAST_ADMIN_REQUIRED")
							);

			User storedUser =
							userRepository.findById(user.id())
											.orElseThrow();

			assertThat(storedUser.getStatus())
							.isEqualTo(User.STATUS_ACTIVE);

			assertThat(
							memberRepository.findByUserId(user.id())
			).isNotEmpty();
	}

	@Test
	void deletionAnonymizesUserWhenDocumentsMustBeKept()
					throws Exception {

			RegisteredUser admin =
							register("gdpr-admin");

			long organizationId =
							createOrganization(admin.session());

			RegisteredUser user =
							register("gdpr-user-with-document");

			Organization organization =
							organizationRepository
											.findById(organizationId)
											.orElseThrow();

			User storedUser =
							userRepository
											.findById(user.id())
											.orElseThrow();

			OrganizationMember membership =
							new OrganizationMember();

			membership.setUser(storedUser);
			membership.setOrganization(organization);
			membership.setRole(Role.CLIENT);

			memberRepository.save(membership);

			Document document = new Document();

			document.setOrganization(organization);
			document.setOwner(storedUser);
			document.setFilename("facture.pdf");
			document.setType("application/pdf");
			document.setSize(100);
			document.setStoragePath(
							"/tmp/gdpr-test-document.pdf"
			);
			document.setStatus(
							DocumentStatus.UPLOADED
			);

			Document savedDocument =
							documentRepository.save(document);

			Invitation pendingInvitation = new Invitation();
			pendingInvitation.setOrganization(organization);
			pendingInvitation.setEmail(user.email());
			pendingInvitation.setRole(Role.CLIENT);
			pendingInvitation.setStatus(InvitationStatus.PENDING);
			pendingInvitation.setToken(UUID.randomUUID().toString());
			pendingInvitation.setExpiresAt(
							LocalDateTime.now().plusDays(7)
			);
			invitationRepository.save(pendingInvitation);

			mockMvc.perform(
											post("/api/gdpr/delete")
															.with(csrf())
															.cookie(user.session())
															.contentType(MediaType.APPLICATION_JSON)
															.content("""
																			{
																					"confirmation":"SUPPRIMER"
																			}
																			""")
							)
							.andExpect(status().isNoContent());

			User anonymizedUser =
							userRepository
											.findById(user.id())
											.orElseThrow();

			assertThat(anonymizedUser.getStatus())
							.isEqualTo(
											User.STATUS_ANONYMIZED
							);

			assertThat(anonymizedUser.getEmail())
							.isEqualTo(
											"deleted-"
															+ user.id()
															+ "@anonymized.local"
							);

			assertThat(anonymizedUser.getFirstName())
							.isEqualTo("Deleted");

			assertThat(anonymizedUser.getLastName())
							.isEqualTo("User");

			assertThat(
							memberRepository.findByUserId(user.id())
			).isEmpty();

			// L'email en clair ne doit plus apparaitre dans les invitations
			assertThat(
							invitationRepository
											.findByEmailIgnoreCaseOrderByCreatedAtDesc(
															user.email()
											)
			).isEmpty();

			Document preservedDocument =
							documentRepository
											.findById(savedDocument.getId())
											.orElseThrow();

			assertThat(
							preservedDocument.getOwner().getId()
			).isEqualTo(user.id());

			mockMvc.perform(
											get("/api/auth/me")
															.cookie(user.session())
							)
							.andExpect(status().isUnauthorized());

			mockMvc.perform(
											post("/api/auth/login")
															.with(csrf())
															.contentType(
																			MediaType.APPLICATION_JSON
															)
															.content("""
																			{
																					"email":"%s",
																					"password":"%s"
																			}
																			""".formatted(
																			user.email(),
																			user.password()
															))
							)
							.andExpect(status().isUnauthorized());
	}

	@Test
	void deletionPhysicallyDeletesUserWithoutDocuments()
					throws Exception {

			RegisteredUser admin =
							register("gdpr-delete-admin");

			long organizationId =
							createOrganization(admin.session());

			RegisteredUser user =
							register("gdpr-user-without-document");

			Organization organization =
							organizationRepository
											.findById(organizationId)
											.orElseThrow();

			User storedUser =
							userRepository
											.findById(user.id())
											.orElseThrow();

			OrganizationMember membership =
							new OrganizationMember();

			membership.setUser(storedUser);
			membership.setOrganization(organization);
			membership.setRole(Role.CLIENT);

			memberRepository.save(membership);

			assertThat(
							documentRepository.existsByOwnerId(user.id())
			).isFalse();

			mockMvc.perform(
											post("/api/gdpr/delete")
															.with(csrf())
															.cookie(user.session())
															.contentType(MediaType.APPLICATION_JSON)
															.content("""
																			{
																					"confirmation":"SUPPRIMER"
																			}
																			""")
							)
							.andExpect(status().isNoContent());

			assertThat(
							userRepository.findById(user.id())
			).isEmpty();

			assertThat(
							memberRepository.findByUserId(user.id())
			).isEmpty();

			mockMvc.perform(
											get("/api/auth/me")
															.cookie(user.session())
							)
							.andExpect(status().isUnauthorized());

			mockMvc.perform(
											post("/api/auth/login")
															.with(csrf())
															.contentType(MediaType.APPLICATION_JSON)
															.content("""
																			{
																					"email":"%s",
																					"password":"%s"
																			}
																			""".formatted(
																			user.email(),
																			user.password()
															))
							)
							.andExpect(status().isUnauthorized());
	}

	@Test
	void deletionInvalidatesAllUserSessionsAndCannotRestoreDeletedAccount()
					throws Exception {

			RegisteredUser user =
							register("gdpr-multi-session");

			MvcResult secondLogin = mockMvc.perform(
											post("/api/auth/login")
															.with(csrf())
															.contentType(MediaType.APPLICATION_JSON)
															.content("""
																			{
																					"email":"%s",
																					"password":"%s"
																			}
																			""".formatted(
																			user.email(),
																			user.password()
															))
							)
							.andExpect(status().isOk())
							.andReturn();

			Cookie secondSession =
							secondLogin.getResponse()
											.getCookie("EFACTURE_SESSION");

			assertThat(secondSession).isNotNull();

			mockMvc.perform(
											post("/api/gdpr/delete")
															.with(csrf())
															.cookie(user.session())
															.contentType(MediaType.APPLICATION_JSON)
															.content("""
																			{
																					"confirmation":"SUPPRIMER"
																			}
																			""")
							)
							.andExpect(status().isNoContent());

			mockMvc.perform(
											get("/api/auth/me")
															.cookie(user.session())
							)
							.andExpect(status().isUnauthorized());

			mockMvc.perform(
											get("/api/auth/me")
															.cookie(secondSession)
							)
							.andExpect(status().isUnauthorized());

			mockMvc.perform(
											put("/api/me")
															.with(csrf())
															.cookie(secondSession)
															.contentType(MediaType.APPLICATION_JSON)
															.content("""
																			{
																					"firstName":"Restored",
																					"lastName":"User"
																			}
																			""")
							)
							.andExpect(status().isUnauthorized());

			assertThat(
							userRepository.findById(user.id())
			).isEmpty();
	}

	private record RegisteredUser(
					Long id,
					String email,
					String password,
					Cookie session
	) {
	}
}
