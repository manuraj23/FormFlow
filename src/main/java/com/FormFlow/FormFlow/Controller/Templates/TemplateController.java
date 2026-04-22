package com.FormFlow.FormFlow.Controller.Templates;

import com.FormFlow.FormFlow.DTO.FormDetails.FormCreateDTO;
import com.FormFlow.FormFlow.DTO.Templates.TemplateSummaryDTO;
import com.FormFlow.FormFlow.DTO.Templates.TemplateUseResponseDTO;
import com.FormFlow.FormFlow.Service.Templates.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user/templates")
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateService templateService;

	@Operation(summary = "Get all available templates")
	@GetMapping
	public ResponseEntity<List<TemplateSummaryDTO>> getTemplates() {
		return ResponseEntity.ok(templateService.getTemplates());
	}

	@Operation(summary = "Get a template for preview/edit")
	@GetMapping("/{templateId}")
	public ResponseEntity<FormCreateDTO> getTemplate(@PathVariable UUID templateId) {
		return ResponseEntity.ok(templateService.getTemplate(templateId));
	}

	@Operation(summary = "Use a template and create a draft form owned by the logged-in user")
	@PostMapping("/{templateId}/use")
	public ResponseEntity<TemplateUseResponseDTO> useTemplate(@PathVariable UUID templateId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		UUID formId = templateService.useTemplate(templateId, username);

		return ResponseEntity.ok(new TemplateUseResponseDTO(
				formId,
				"Template copied successfully. You can now edit and publish this form under your account."
		));
	}
}
