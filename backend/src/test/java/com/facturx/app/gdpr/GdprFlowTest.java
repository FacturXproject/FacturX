package com.facturx.app.gdpr;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.document.DocumentStatus;
import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.OrganizationRepository;
import com.facturx.app.organization.Role;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;

import jakarta.servlet.http.Cookie;

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

	private static String uniqueEmail(String label) {
		return label + "-" + UUID.randomUUID() + "@x.fr";
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
	void exportContainsUserDataAndOrganizations()
			throws Exception {

		RegisteredUser user =
				register("gdpr-export");

		long organizationId =
				createOrganization(user.session());

		mockMvc.perform(
						get("/api/gdpr/export")
								.cookie(user.session())
				)
				.andExpect(status().isOk())
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
				.andExpect(
						jsonPath("$.organizations[0].id")
								.value(organizationId)
				)
				.andExpect(
						jsonPath("$.organizations[0].role")
								.value("ADMIN")
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
								.value(
										"Confirmation must be DELETE"
								)
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
											"confirmation":"DELETE"
										}
										""")
				)
				.andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.error")
								.value(
										"User is the last admin of an organization"
								)
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
	void deletionAnonymizesUserRemovesMembershipsAndKeepsDocuments()
			throws Exception {

		RegisteredUser admin =
				register("gdpr-admin");

		long organizationId =
				createOrganization(admin.session());

		RegisteredUser user =
				register("gdpr-user");

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

		mockMvc.perform(
						post("/api/gdpr/delete")
								.with(csrf())
								.cookie(user.session())
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{
											"confirmation":"DELETE"
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

	private record RegisteredUser(
			Long id,
			String email,
			String password,
			Cookie session
	) {
	}
}
