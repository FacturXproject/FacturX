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
import com.facturx.app.generation.GenerationResponse;
import com.facturx.app.generation.GenerationService;
import com.facturx.app.generation.GenerationStatus;
import com.facturx.app.validation.FacturXValidationService;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;

import jakarta.servlet.http.Cookie;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

	@Autowired
	private DocumentService documentService;

	@MockitoBean
	private FacturXValidationService validationService;

	@MockitoBean
	private GenerationService generationService;

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

	private long createGenerationJob(
			Cookie session,
			long documentId)
			throws Exception {

		MvcResult result = mockMvc.perform(
						post("/api/jobs/generation")
								.param(
										"documentId",
										String.valueOf(documentId)
								)
								.with(csrf())
								.cookie(session)
								.contentType(
										MediaType.APPLICATION_JSON
								)
								.content("""
										{
											"profile":"EN16931"
										}
										""")
				)
				.andExpect(status().isOk())
				.andExpect(
						jsonPath("$.documentId")
								.value(documentId)
				)
				.andExpect(
						jsonPath("$.type")
								.value("GENERATION")
				)
				.andExpect(
						jsonPath("$.profile")
								.value("EN16931")
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
						JobRetryException.class
				)
				.hasMessageContaining(
						"Maximum number of attempts"
				);
	}

	@Test
	void unknownJobReturns404()
			throws Exception {

		Cookie session =
				registerAndLogin("job-not-found");

		mockMvc.perform(
				get("/api/jobs/999999")
						.cookie(session)
		)
				.andExpect(status().isNotFound())
				.andExpect(
						jsonPath("$.error")
								.value("JOB_NOT_FOUND")
				)
				.andExpect(
						jsonPath("$.message")
								.value("Job not found: 999999")
				);
	}

	@Test
	void retryOfCompletedJobReturns409()
			throws Exception {

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		)).thenReturn(null);

		Cookie session =
				registerAndLogin("job-invalid-retry");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(
						session,
						organizationId
				);

		long jobId =
				createValidationJob(
						session,
						documentId
				);

		waitForStatus(
				jobId,
				JobStatus.DONE
		);

		mockMvc.perform(
				post("/api/jobs/" + jobId + "/retry")
						.with(csrf())
						.cookie(session)
		)
				.andExpect(status().isConflict())
				.andExpect(
						jsonPath("$.error")
								.value("JOB_RETRY_CONFLICT")
				)
				.andExpect(
						jsonPath("$.message")
								.value("Only failed jobs can be retried.")
				);
	}

	@Test
	void secondActiveJobForSameDocumentIsRejected()
			throws Exception {

		CountDownLatch ready =
				new CountDownLatch(2);

		CountDownLatch start =
				new CountDownLatch(1);

		CountDownLatch allowValidationToFinish =
				new CountDownLatch(1);

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		)).thenAnswer(invocation -> {

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

		ExecutorService executor =
				Executors.newFixedThreadPool(2);

		try {

			Future<Object> first =
					executor.submit(() -> {

						ready.countDown();
						start.await();

						try {
							return jobService.createValidationJob(
									documentId
							);
						} catch (Exception e) {
							return e;
						}
					});

			Future<Object> second =
					executor.submit(() -> {

						ready.countDown();
						start.await();

						try {
							return jobService.createValidationJob(
									documentId
							);
						} catch (Exception e) {
							return e;
						}
					});

			assertThat(
					ready.await(
							2,
							TimeUnit.SECONDS
					)
			).isTrue();

			start.countDown();

			Object firstResult =
					first.get(
							2,
							TimeUnit.SECONDS
					);

			Object secondResult =
					second.get(
							2,
							TimeUnit.SECONDS
					);

			long accepted =
					java.util.stream.Stream
							.of(firstResult, secondResult)
							.filter(Job.class::isInstance)
							.count();

			long rejected =
					java.util.stream.Stream
							.of(firstResult, secondResult)
							.filter(JobConflictException.class::isInstance)
							.count();

			assertThat(accepted)
					.isEqualTo(1);

			assertThat(rejected)
					.isEqualTo(1);

		} finally {

			allowValidationToFinish.countDown();

			executor.shutdownNow();
		}
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

	@Test
	void generationJobMovesToDone()
			throws Exception {

		when(generationService.generate(
				anyLong(),
				anyString(),
				anyLong()
		)).thenAnswer(invocation -> {

			Long documentId =
					invocation.getArgument(0);

			return new GenerationResponse(
					1L,
					documentId,
					100L,
					"Factur-X_test.pdf",
					"EN16931",
					GenerationStatus.VALID,
					10L,
					0,
					0,
					null,
					OffsetDateTime.now()
			);
		});

		Cookie session =
				registerAndLogin("generation-done");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(
						session,
						organizationId
				);

		long jobId =
				createGenerationJob(
						session,
						documentId
				);

		Job job =
				waitForStatus(
						jobId,
						JobStatus.DONE
				);

		assertThat(job.getType())
				.isEqualTo(JobType.GENERATION);

		assertThat(job.getProfile())
				.isEqualTo("EN16931");

		assertThat(job.getRequestedBy())
				.isNotNull();

		assertThat(job.getAttempts())
				.isEqualTo(1);

		assertThat(job.getError())
				.isNull();

		mockMvc.perform(
						get("/api/jobs/" + jobId)
								.cookie(session)
				)
				.andExpect(status().isOk())
				.andExpect(
						jsonPath("$.type")
								.value("GENERATION")
				)
				.andExpect(
						jsonPath("$.status")
								.value("DONE")
				)
				.andExpect(
						jsonPath("$.profile")
								.value("EN16931")
				);
	}

	@Test
	void generationFailureMarksJobAsFailed()
			throws Exception {

		when(generationService.generate(
				anyLong(),
				anyString(),
				anyLong()
		)).thenAnswer(invocation -> {

			Long documentId =
					invocation.getArgument(0);

			return new GenerationResponse(
					1L,
					documentId,
					null,
					null,
					"EN16931",
					GenerationStatus.FAILED,
					null,
					0,
					0,
					"Generation service unavailable",
					OffsetDateTime.now()
			);
		});

		Cookie session =
				registerAndLogin("generation-failed");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(
						session,
						organizationId
				);

		long jobId =
				createGenerationJob(
						session,
						documentId
				);

		Job job =
				waitForStatus(
						jobId,
						JobStatus.FAILED
				);

		assertThat(job.getType())
				.isEqualTo(JobType.GENERATION);

		assertThat(job.getAttempts())
				.isEqualTo(1);

		assertThat(job.getError())
				.isEqualTo(
						"Generation service unavailable"
				);
	}

	@Test
	void invalidGeneratedDocumentStillMarksJobAsDone()
			throws Exception {

		when(generationService.generate(
				anyLong(),
				anyString(),
				anyLong()
		)).thenAnswer(invocation -> {

			Long documentId =
					invocation.getArgument(0);

			return new GenerationResponse(
					1L,
					documentId,
					100L,
					"Factur-X_invalid.pdf",
					"EN16931",
					GenerationStatus.INVALID,
					10L,
					2,
					0,
					null,
					OffsetDateTime.now()
			);
		});

		Cookie session =
				registerAndLogin("generation-invalid");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(
						session,
						organizationId
				);

		long jobId =
				createGenerationJob(
						session,
						documentId
				);

		Job job =
				waitForStatus(
						jobId,
						JobStatus.DONE
				);

		assertThat(job.getStatus())
				.isEqualTo(JobStatus.DONE);

		assertThat(job.getError())
				.isNull();
	}

	@Test
	void userWithoutPermissionCannotStartGenerationJob()
			throws Exception {

		Cookie ownerSession =
				registerAndLogin("generation-owner");

		long organizationId =
				createOrganization(ownerSession);

		long documentId =
				uploadPdf(
						ownerSession,
						organizationId
				);

		Cookie otherUserSession =
				registerAndLogin("generation-other");

		mockMvc.perform(
						post("/api/jobs/generation")
								.param(
										"documentId",
										String.valueOf(documentId)
								)
								.with(csrf())
								.cookie(otherUserSession)
								.contentType(
										MediaType.APPLICATION_JSON
								)
								.content("""
										{
											"profile":"EN16931"
										}
										""")
				)
				.andExpect(status().isForbidden());
	}

	@Test
	void retryIsRejectedWhenAnotherJobIsActiveForSameDocument()
			throws Exception {

		CountDownLatch secondJobStarted =
				new CountDownLatch(1);

		CountDownLatch allowSecondJobToFinish =
				new CountDownLatch(1);

		when(validationService.validate(
				any(byte[].class),
				anyString(),
				anyLong()
		))
				.thenThrow(
						new RuntimeException("First job fails")
				)
				.thenAnswer(invocation -> {

					secondJobStarted.countDown();

					allowSecondJobToFinish.await(
							3,
							TimeUnit.SECONDS
					);

					return null;
				});

		Cookie session =
				registerAndLogin("job-retry-conflict");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(session, organizationId);

		// Job A -> FAILED
		long failedJobId =
				createValidationJob(
						session,
						documentId
				);

		waitForStatus(
				failedJobId,
				JobStatus.FAILED
		);

		// Job B -> PROCESSING
		Job activeJob =
				jobService.createValidationJob(
						documentId
				);

		assertThat(
				secondJobStarted.await(
						2,
						TimeUnit.SECONDS
				)
		).isTrue();

		// Retry de A doit être refusé car B tourne déjà
		assertThatThrownBy(
				() -> jobService.retryJob(failedJobId)
		)
				.isInstanceOf(
						JobConflictException.class
				)
				.hasMessageContaining(
						"already queued or processing"
				);

		allowSecondJobToFinish.countDown();

		waitForStatus(
				activeJob.getId(),
				JobStatus.DONE
		);
	}

	@Test
	void interruptedJobIsMarkedFailedOnRecovery()
			throws Exception {

		Cookie session =
				registerAndLogin("job-recovery");

		long organizationId =
				createOrganization(session);

		long documentId =
				uploadPdf(
						session,
						organizationId
				);

		Document document =
				documentService.getDocument(documentId);

		Job interruptedJob = new Job();
		interruptedJob.setDocument(document);
		interruptedJob.setType(JobType.VALIDATION);
		interruptedJob.setStatus(JobStatus.PROCESSING);
		interruptedJob.setAttempts(1);

		interruptedJob =
				jobRepository.save(interruptedJob);

		jobService.recoverInterruptedJobs();

		Job recovered =
				jobRepository.findById(
						interruptedJob.getId()
				).orElseThrow();

		assertThat(recovered.getStatus())
				.isEqualTo(JobStatus.FAILED);

		assertThat(recovered.getError())
				.isEqualTo(
						"Job interrupted by application restart."
				);

		assertThat(recovered.getFinishedAt())
				.isNotNull();
	}
}
