package com.facturx.app.job;

import com.facturx.app.auth.AppUserPrincipal;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.facturx.app.generation.GenerationRequest;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

	private final JobService jobService;
	private final DocumentService documentService;
	private final PermissionService permissionService;

	public JobController(
			JobService jobService,
			DocumentService documentService,
			PermissionService permissionService) {

		this.jobService = jobService;
		this.documentService = documentService;
		this.permissionService = permissionService;
	}

	private Long currentUserId(Authentication authentication) {
		AppUserPrincipal principal =
				(AppUserPrincipal) authentication.getPrincipal();

		return principal.getUser().getId();
	}

	private void requireJobPermission(
			Job job,
			Authentication authentication) {

		Permission permission =
				job.getType() == JobType.GENERATION
						? Permission.GENERATE_DOCUMENT
						: Permission.VALIDATE_DOCUMENT;

		permissionService.requirePermission(
				currentUserId(authentication),
				job.getDocument().getOrganization().getId(),
				permission
		);
	}

	@PostMapping("/validation")
	public JobResponse createValidationJob(
			@RequestParam Long documentId,
			Authentication authentication) {

		Document document = documentService.getDocument(documentId);

		permissionService.requirePermission(
				currentUserId(authentication),
				document.getOrganization().getId(),
				Permission.VALIDATE_DOCUMENT
		);

		Job job = jobService.createValidationJob(documentId);

		return JobResponse.from(job);
	}

	@PostMapping("/generation")
	public JobResponse createGenerationJob(
			@RequestParam Long documentId,
			@RequestBody(required = false) GenerationRequest request,
			Authentication authentication) {

		Document document =
				documentService.getDocument(documentId);

		Long userId =
				currentUserId(authentication);

		permissionService.requirePermission(
				userId,
				document.getOrganization().getId(),
				Permission.GENERATE_DOCUMENT
		);

		String profile =
				request != null
						? request.profile()
						: null;

		Job job =
				jobService.createGenerationJob(
						documentId,
						profile,
						userId
				);

		return JobResponse.from(job);
	}

	@GetMapping("/{id}")
	public JobResponse getJob(
			@PathVariable Long id,
			Authentication authentication) {

		Job job = jobService.getJob(id);

		requireJobPermission(
				job,
				authentication
		);

		return JobResponse.from(job);
	}

	@PostMapping("/{id}/retry")
	public JobResponse retryJob(
			@PathVariable Long id,
			Authentication authentication) {

		Job job = jobService.getJob(id);

		requireJobPermission(
				job,
				authentication
		);

		Job retriedJob =
				jobService.retryJob(id);

		return JobResponse.from(retriedJob);
	}

}
