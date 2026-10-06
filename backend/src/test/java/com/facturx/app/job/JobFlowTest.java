package com.facturx.app.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.validation.FacturXValidationService;

import jakarta.servlet.http.Cookie;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobFlowTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobService jobService;

	@MockitoBean
	private FacturXValidationService validationService;

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

	private static Cookie sessionCookie(MvcResult result) {

		Cookie sessionCookie =
				result.getResponse()
						.getCookie("EFACTURE_SESSION");

		assertThat(sessionCookie).isNotNull();

		return sessionCookie;
	}

	private Cookie registerAndLogin(String label)
			throws Exception {

		String email = uniqueEmail(label);

		MvcResult result = mockMvc.perform(
						post("/api/auth/register")
								.with(csrf())
								.contentType(
										MediaType.APPLICATION_JSON
								)
								.content(
										registerBody(
												email,
												"correcthorsebattery"
										)
								)
				)
				.andExpect(status().isCreated())
				.andReturn();

		return sessionCookie(result);
	}

	private long createOrganization(Cookie session)
			throws Exception {

		MvcResult result = mockMvc.perform(
						post(
								"/api/organizations"
										+ "?name=Cabinet Test"
						)
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

	private static byte[] validPdfBytes() {

		return """
				%PDF-1.4
				1 0 obj<</Type/Catalog>>endobj
				trailer<</Root 1 0 R>>
				""".getBytes();
	}

	private long uploadPdf(
			Cookie session,
			long organizationId)
			throws Exception {

		MockMultipartFile file =
				new MockMultipartFile(
						"file",
						"facture.pdf",
						"application/pdf",
						validPdfBytes()
				);

		MvcResult result = mockMvc.perform(
						multipart(
								"/api/documents"
										+ "?organizationId="
										+ organizationId
						)
								.file(file)
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

	private long createValidationJob(
			Cookie session,
			long documentId)
			throws Exception {

		MvcResult result = mockMvc.perform(
						post("/api/jobs/validation")
								.param(
										"documentId",
										String.valueOf(documentId)
								)
								.with(csrf())
								.cookie(session)
				)
				.andExpect(status().isOk())
				.andExpect(
						jsonPath("$.documentId")
								.value(documentId)
				)
				.andExpect(
						jsonPath("$.type")
								.value("VALIDATION")
				)
				.andReturn();

		return new ObjectMapper()
				.readTree(
						result.getResponse()
								.getContentAsString()
				)
				.get("id")
				.asLong();
	}

	private Job waitForStatus(
			long jobId,
			JobStatus expectedStatus)
			throws Exception {

		long deadline =
				System.currentTimeMillis() + 5000;

		while (System.currentTimeMillis() < deadline) {

			Job job = jobRepository
					.findById(jobId)
					.orElseThrow();

			if (job.getStatus() == expectedStatus) {
				return job;
			}

			Thread.sleep(50);
		}

		Job job = jobRepository
				.findById(jobId)
				.orElseThrow();

		throw new AssertionError(
				"Expected job status "
						+ expectedStatus
						+ " but was "
						+ job.getStatus()
		);
	}

	@Test
	void validationJobMovesToDone()
			throws Exception {

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		)).thenReturn(null);

		Cookie session =
				registerAndLogin("job-done");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(session, organizationId);

		long jobId =
				createValidationJob(
						session,
						documentId
				);

		Job job =
				waitForStatus(
						jobId,
						JobStatus.DONE
				);

		assertThat(job.getType())
				.isEqualTo(JobType.VALIDATION);

		assertThat(job.getDocument().getId())
				.isEqualTo(documentId);

		assertThat(job.getAttempts())
				.isEqualTo(1);

		assertThat(job.getStartedAt())
				.isNotNull();

		assertThat(job.getFinishedAt())
				.isNotNull();

		assertThat(job.getError())
				.isNull();

		mockMvc.perform(
						get("/api/jobs/" + jobId)
								.cookie(session)
				)
				.andExpect(status().isOk())
				.andExpect(
						jsonPath("$.status")
								.value("DONE")
				)
				.andExpect(
						jsonPath("$.attempts")
								.value(1)
				);
	}

	@Test
	void validationFailureStoresError()
			throws Exception {

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		)).thenThrow(
				new RuntimeException(
						"Mustang unavailable"
				)
		);

		Cookie session =
				registerAndLogin("job-failed");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(session, organizationId);

		long jobId =
				createValidationJob(
						session,
						documentId
				);

		Job job =
				waitForStatus(
						jobId,
						JobStatus.FAILED
				);

		assertThat(job.getAttempts())
				.isEqualTo(1);

		assertThat(job.getError())
				.isEqualTo(
						"Mustang unavailable"
				);

		assertThat(job.getFinishedAt())
				.isNotNull();
	}

	@Test
	void failedJobCanBeRetried()
			throws Exception {

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		))
				.thenThrow(
						new RuntimeException(
								"Temporary failure"
						)
				)
				.thenReturn(null);

		Cookie session =
				registerAndLogin("job-retry");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(session, organizationId);

		long jobId =
				createValidationJob(
						session,
						documentId
				);

		Job failed =
				waitForStatus(
						jobId,
						JobStatus.FAILED
				);

		assertThat(failed.getAttempts())
				.isEqualTo(1);

		mockMvc.perform(
						post(
								"/api/jobs/"
										+ jobId
										+ "/retry"
						)
								.with(csrf())
								.cookie(session)
				)
				.andExpect(status().isOk());

		Job done =
				waitForStatus(
						jobId,
						JobStatus.DONE
				);

		assertThat(done.getAttempts())
				.isEqualTo(2);

		assertThat(done.getError())
				.isNull();
	}

	@Test
	void retryIsCappedAtThreeAttempts()
			throws Exception {

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		)).thenThrow(
				new RuntimeException(
						"Always failing"
				)
		);

		Cookie session =
				registerAndLogin("job-max-retry");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(session, organizationId);

		long jobId =
				createValidationJob(
						session,
						documentId
				);

		waitForStatus(
				jobId,
				JobStatus.FAILED
		);

		jobService.retryJob(jobId);

		waitForStatus(
				jobId,
				JobStatus.FAILED
		);

		jobService.retryJob(jobId);

		Job thirdFailure =
				waitForStatus(
						jobId,
						JobStatus.FAILED
				);

		assertThat(thirdFailure.getAttempts())
				.isEqualTo(3);

		assertThatThrownBy(
				() -> jobService.retryJob(jobId)
		)
				.isInstanceOf(
						IllegalStateException.class
				)
				.hasMessageContaining(
						"Maximum number of attempts"
				);
	}

	@Test
	void secondActiveJobForSameDocumentIsRejected()
			throws Exception {

		CountDownLatch validationStarted =
				new CountDownLatch(1);

		CountDownLatch allowValidationToFinish =
				new CountDownLatch(1);

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		)).thenAnswer(invocation -> {

			validationStarted.countDown();

			allowValidationToFinish.await(
					3,
					TimeUnit.SECONDS
			);

			return null;
		});

		Cookie session =
				registerAndLogin("job-concurrency");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(session, organizationId);

		Job firstJob =
				jobService.createJob(
						documentId,
						JobType.VALIDATION
				);

		assertThat(
				validationStarted.await(
						2,
						TimeUnit.SECONDS
				)
		).isTrue();

		assertThatThrownBy(
				() -> jobService.createJob(
						documentId,
						JobType.VALIDATION
				)
		)
				.isInstanceOf(
						IllegalStateException.class
				)
				.hasMessageContaining(
						"already queued or processing"
				);

		allowValidationToFinish.countDown();

		Job finished =
				waitForStatus(
						firstJob.getId(),
						JobStatus.DONE
				);

		assertThat(finished.getStatus())
				.isEqualTo(JobStatus.DONE);
	}

	@Test
	void userWithoutPermissionCannotStartValidationJob()
			throws Exception {

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		)).thenReturn(null);

		Cookie ownerSession =
				registerAndLogin("job-owner");

		long organizationId =
				createOrganization(ownerSession);

		long documentId =
				uploadPdf(
						ownerSession,
						organizationId
				);

		Cookie otherUserSession =
				registerAndLogin("job-other");

		mockMvc.perform(
						post("/api/jobs/validation")
								.param(
										"documentId",
										String.valueOf(documentId)
								)
								.with(csrf())
								.cookie(otherUserSession)
				)
				.andExpect(status().isForbidden());
	}
}
