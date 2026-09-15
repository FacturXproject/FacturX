package com.facturx.app.invoice;

import com.facturx.app.auth.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class InvoiceViewController {

	private final InvoiceViewService invoiceViewService;

	public InvoiceViewController(InvoiceViewService invoiceViewService) {
		this.invoiceViewService = invoiceViewService;
	}

	private Long currentUserId(Authentication authentication) {
		AppUserPrincipal principal =
				(AppUserPrincipal) authentication.getPrincipal();

		return principal.getUser().getId();
	}

	@GetMapping("/{id}/invoice-view")
	public InvoiceViewResponse getInvoiceView(
			@PathVariable Long id,
			Authentication authentication
	) {
		return invoiceViewService.getInvoiceView(
				id,
				currentUserId(authentication)
		);
	}
}
