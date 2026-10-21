package com.facturx.app.job;

import com.facturx.app.document.Document;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "jobs")
public class Job {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private JobType type;

	@ManyToOne
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@Enumerated(EnumType.STRING)
	private JobStatus status = JobStatus.QUEUED;

	private int attempts = 0;

	@Column(name = "requested_by")
	private Long requestedBy;

	@Column(name = "profile", length = 20)
	private String profile;

	private String error;

	private Instant startedAt;

	private Instant finishedAt;

	public Job() {
	}

	public Long getId() {
		return id;
	}

	public JobType getType() {
		return type;
	}

	public Document getDocument() {
		return document;
	}

	public JobStatus getStatus() {
		return status;
	}

	public int getAttempts() {
		return attempts;
	}

	public String getError() {
		return error;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	public Long getRequestedBy() {
		return requestedBy;
	}

	public String getProfile() {
		return profile;
	}

	public void setType(JobType type) {
		this.type = type;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public void setError(String error) {
		this.error = error;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public void setFinishedAt(Instant finishedAt) {
		this.finishedAt = finishedAt;
	}

	public void setRequestedBy(Long requestedBy) {
		this.requestedBy = requestedBy;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}
}
