package com.facturx.app.job;

import com.facturx.app.auth.AppUserPrincipal;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

		Job job = jobService.createJob(
				documentId,
				JobType.VALIDATION
		);

		return JobResponse.from(job);
	}

	@GetMapping("/{id}")
	public JobResponse getJob(
			@PathVariable Long id,
			Authentication authentication) {

		Job job = jobService.getJob(id);

		permissionService.requirePermission(
				currentUserId(authentication),
				job.getDocument().getOrganization().getId(),
				Permission.VALIDATE_DOCUMENT
		);

		return JobResponse.from(job);
	}

	@PostMapping("/{id}/retry")
	public JobResponse retryJob(
			@PathVariable Long id,
			Authentication authentication) {

		Job job = jobService.getJob(id);

		permissionService.requirePermission(
				currentUserId(authentication),
				job.getDocument().getOrganization().getId(),
				Permission.VALIDATE_DOCUMENT
		);

		Job retriedJob = jobService.retryJob(id);

		return JobResponse.from(retriedJob);
	}
}
