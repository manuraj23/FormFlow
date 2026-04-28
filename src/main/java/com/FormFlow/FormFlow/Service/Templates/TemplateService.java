package com.FormFlow.FormFlow.Service.Templates;

import com.FormFlow.FormFlow.DTO.FormDetails.FormCreateDTO;
import com.FormFlow.FormFlow.DTO.Templates.TemplateSummaryDTO;
import com.FormFlow.FormFlow.Entity.Template.FormTemplate;
import com.FormFlow.FormFlow.Repository.FormTemplateRepository;
import com.FormFlow.FormFlow.Service.User.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemplateService {

	private final FormTemplateRepository formTemplateRepository;
	private final UserService userService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	@Transactional
	public void seedDefaultTemplates() {
		for (TemplateSeed seed : defaultTemplates()) {
			FormTemplate existing = formTemplateRepository.findByTemplateName(seed.templateName()).orElse(null);
			if (existing != null) {
				boolean changed = false;
				if (existing.getImageUrl() == null || existing.getImageUrl().isBlank()) {
					existing.setImageUrl(seed.imageUrl());
					changed = true;
				}
				// Keep existing default templates aligned with current seed field model
				// so type normalization fixes are applied on startup.
				if (!seed.sections().equals(existing.getSections())) {
					existing.setSections(seed.sections());
					changed = true;
				}
				if (changed) {
					formTemplateRepository.save(existing);
				}
				continue;
			}

			FormTemplate template = new FormTemplate();
			template.setTemplateName(seed.templateName());
			template.setTitle(seed.title());
			template.setDescription(seed.description());
			template.setTheme("default");
			template.setImageUrl(seed.imageUrl());
			template.setSettings(Map.of("allowMultipleSubmissions", false));
			template.setSections(seed.sections());
			template.setActive(true);
			formTemplateRepository.save(template);
		}
	}

	@Transactional(readOnly = true)
	public List<TemplateSummaryDTO> getTemplates() {
		return formTemplateRepository.findByActiveTrueOrderByTemplateNameAsc().stream().map(template -> {
			TemplateSummaryDTO dto = new TemplateSummaryDTO();
			dto.setId(template.getId());
			dto.setTemplateName(template.getTemplateName());
			dto.setTitle(template.getTitle());
			dto.setDescription(template.getDescription());
			dto.setImageUrl(template.getImageUrl());
			return dto;
		}).toList();
	}

	@Transactional(readOnly = true)
	public FormCreateDTO getTemplate(UUID templateId) {
		FormTemplate template = formTemplateRepository.findById(templateId)
				.filter(FormTemplate::isActive)
				.orElseThrow(() -> new RuntimeException("Template not found"));

		FormCreateDTO dto = new FormCreateDTO();
		dto.setTitle(template.getTitle());
		dto.setDescription(template.getDescription());
		dto.setImageUrl(template.getImageUrl());
		dto.setTheme(template.getTheme());
		dto.setPublished(false);
		dto.setSettings(template.getSettings());
		dto.setSections(objectMapper.convertValue(template.getSections(), new TypeReference<>() {}));
		return dto;
	}

	@Transactional
	public UUID useTemplate(UUID templateId, String username) {
		FormCreateDTO templateDraft = getTemplate(templateId);
		templateDraft.setPublished(false);
		return userService.createForm(templateDraft, username);
	}

	private record TemplateSeed(String templateName, String title, String description, String imageUrl, List<Map<String, Object>> sections) {}

	private List<TemplateSeed> defaultTemplates() {
		return List.of(
				seed("Employee Onboarding Form", "Employee Onboarding Form", "Collect key details for new employee onboarding.", templateImage("Employee onboarding.png"), List.of(
						section("Personal Details", 1, List.of(
								field("TEXT", 1, "Full Name", true, "Enter employee full name", null),
								field("EMAIL", 2, "Work Email", true, "name@company.com", null),
								field("PHONE", 3, "Phone Number", true, "Enter phone number", null)
						)),
						section("Employment Details", 2, List.of(
								field("DATE", 1, "Joining Date", true, null, null),
								field("TEXT", 2, "Designation", true, "Enter designation", null),
								field("TEXT", 3, "Department", true, "Enter department", null)
						)),
						section("Compliance", 3, List.of(
								field("TEXT", 1, "Government ID Number", true, "Enter ID number", null),
								field("FILE", 2, "Upload ID Proof", true, null, null),
								field("CHECKBOX", 3, "I confirm submitted details are correct", true, null, List.of("Confirmed"))
						))
				)),
				seed("Student Registration Form", "Student Registration Form", "UG programme registration form with personal, parent, education and stream preference sections.", templateImage("Student Registration Form.png"), List.of(
						section("Personal Details", 1, List.of(
								field("TEXT", 1, "Student Full Name", true, "Enter full name", null),
								field("DATE", 2, "Date of Birth", true, null, null),
								field("EMAIL", 3, "Email Address", true, "student@email.com", null),
								field("PHONE", 4, "Mobile Number", true, "Enter mobile number", null)
						)),
						section("Parents Details", 2, List.of(
								field("TEXT", 1, "Father's Name", true, "Enter father's name", null),
								field("TEXT", 2, "Mother's Name", true, "Enter mother's name", null),
								field("PHONE", 3, "Parent Contact Number", true, "Enter contact number", null)
						)),
						section("Educational Details", 3, List.of(
								field("TEXT", 1, "School Name", true, "Enter school name", null),
								field("NUMBER", 2, "10th Percentage", true, "Enter percentage", null),
								field("NUMBER", 3, "12th Percentage", true, "Enter percentage", null),
								field("FILE", 4, "Upload Marksheet", true, null, null)
						)),
						section("Stream Preference", 4, List.of(
								field("DROPDOWN", 1, "Preferred Stream", true, null, List.of("CSE", "ECE", "EEE", "MECH", "CIVIL", "IT")),
								field("MULTI_SELECT", 2, "Other Interested Streams", false, null, List.of("AI/ML", "Data Science", "Cyber Security", "Robotics")),
								field("TEXTAREA", 3, "Why this stream?", false, "Share your preference", null)
						))
				)),
				seed("Job Application Form", "Job Application Form", "Capture candidate details, experience and role fit.", templateImage("Job Application Form.png"), List.of(
						section("Candidate Profile", 1, List.of(
								field("TEXT", 1, "Full Name", true, "Enter full name", null),
								field("EMAIL", 2, "Email", true, "name@email.com", null),
								field("PHONE", 3, "Phone", true, "Enter phone number", null)
						)),
						section("Professional Details", 2, List.of(
								field("NUMBER", 1, "Years of Experience", true, "Enter years", null),
								field("TEXT", 2, "Current Role", true, "Enter current role", null),
								field("TEXT", 3, "Current Company", false, "Enter company", null)
						)),
						section("Application", 3, List.of(
								field("FILE", 1, "Upload Resume", true, null, null),
								field("TEXTAREA", 2, "Cover Letter", false, "Why should we hire you?", null),
								field("DROPDOWN", 3, "Applied Position", true, null, List.of("Software Engineer", "QA Engineer", "Product Analyst", "Designer"))
						))
				)),
				seed("Event Registration Form", "Event Registration Form", "Register attendees and collect participation preferences.", templateImage("Event Registration Form.png"), List.of(
						section("Attendee Information", 1, List.of(
								field("TEXT", 1, "Full Name", true, "Enter full name", null),
								field("EMAIL", 2, "Email", true, "name@email.com", null),
								field("PHONE", 3, "Phone", false, "Enter phone number", null)
						)),
						section("Event Preferences", 2, List.of(
								field("DROPDOWN", 1, "Ticket Type", true, null, List.of("Standard", "VIP", "Student")),
								field("CHECKBOX", 2, "Sessions Interested", false, null, List.of("Keynote", "Workshop", "Networking")),
								field("TEXTAREA", 3, "Special Requirements", false, "Dietary/accessibility needs", null)
						))
				)),
				seed("Contact Us Form", "Contact Us Form", "Collect incoming inquiries from users or customers.", templateImage("Contact Us Form.png"), List.of(
						section("Contact Details", 1, List.of(
								field("TEXT", 1, "Name", true, "Enter your name", null),
								field("EMAIL", 2, "Email", true, "name@email.com", null),
								field("TEXT", 3, "Subject", true, "Enter subject", null),
								field("TEXTAREA", 4, "Message", true, "Type your message", null)
						))
				)),
				seed("Feedback Form", "Feedback Form", "Gather user feedback on product, service or event quality.", templateImage("Feedback Form.png"), List.of(
						section("Feedback", 1, List.of(
								field("RATING", 1, "Overall Rating", true, null, null),
								field("TEXTAREA", 2, "What did you like?", false, "Share positives", null),
								field("TEXTAREA", 3, "What can be improved?", false, "Share improvements", null)
						))
				)),
				seed("Customer Survey Form", "Customer Survey Form", "Run customer surveys and segment responses.", templateImage("Customer Survey Form.png"), List.of(
						section("Customer Profile", 1, List.of(
								field("DROPDOWN", 1, "Customer Type", true, null, List.of("Individual", "Business")),
								field("TEXT", 2, "Industry", false, "Enter industry", null)
						)),
						section("Survey Questions", 2, List.of(
								field("RADIO", 1, "How satisfied are you?", true, null, List.of("Very Satisfied", "Satisfied", "Neutral", "Dissatisfied")),
								field("RADIO", 2, "Would you recommend us?", true, null, List.of("Yes", "No")),
								field("TEXTAREA", 3, "Additional Comments", false, "Write your comments", null)
						))
				)),
				seed("Leave Application Form", "Leave Application Form", "Submit and track employee leave requests.", templateImage("Leave Application Form.png"), List.of(
						section("Leave Details", 1, List.of(
								field("TEXT", 1, "Employee Name", true, "Enter name", null),
								field("DROPDOWN", 2, "Leave Type", true, null, List.of("Sick Leave", "Casual Leave", "Paid Leave", "Maternity/Paternity")),
								field("DATE", 3, "Start Date", true, null, null),
								field("DATE", 4, "End Date", true, null, null)
						)),
						section("Reason", 2, List.of(
								field("TEXTAREA", 1, "Reason for Leave", true, "Provide reason", null),
								field("CHECKBOX", 2, "Manager Informed", true, null, List.of("Yes"))
						))
				)),
				seed("Expense Reimbursement Form", "Expense Reimbursement Form", "Claim business expenses with category and proof.", templateImage("Expense Reimbursement Form.png"), List.of(
						section("Claimant Details", 1, List.of(
								field("TEXT", 1, "Employee Name", true, "Enter name", null),
								field("TEXT", 2, "Department", true, "Enter department", null)
						)),
						section("Expense Details", 2, List.of(
								field("DATE", 1, "Expense Date", true, null, null),
								field("DROPDOWN", 2, "Expense Category", true, null, List.of("Travel", "Food", "Supplies", "Accommodation", "Other")),
								field("NUMBER", 3, "Amount", true, "Enter amount", null),
								field("TEXTAREA", 4, "Description", true, "Describe expense", null)
						)),
						section("Attachments", 3, List.of(
								field("FILE", 1, "Upload Receipt", true, null, null)
						))
				)),
				seed("Vendor Registration Form", "Vendor Registration Form", "Register vendors and collect company/compliance details.", templateImage("Vendor Registration Form.png"), List.of(
						section("Company Details", 1, List.of(
								field("TEXT", 1, "Company Name", true, "Enter company name", null),
								field("TEXT", 2, "Contact Person", true, "Enter contact name", null),
								field("EMAIL", 3, "Business Email", true, "contact@company.com", null)
						)),
						section("Business Information", 2, List.of(
								field("TEXT", 1, "GST/Tax ID", true, "Enter tax id", null),
								field("TEXT", 2, "Service Category", true, "Enter service category", null),
								field("PHONE", 3, "Contact Number", true, "Enter phone number", null)
						)),
						section("Documents", 3, List.of(
								field("FILE", 1, "Upload Company Profile", false, null, null),
								field("FILE", 2, "Upload Compliance Documents", true, null, null)
						))
				)),
				seed("Internship Application Form", "Internship Application Form", "Collect internship applications and skills information.", templateImage("Internship Application Form.png"), List.of(
						section("Applicant Details", 1, List.of(
								field("TEXT", 1, "Full Name", true, "Enter full name", null),
								field("EMAIL", 2, "Email", true, "name@email.com", null),
								field("PHONE", 3, "Phone", true, "Enter phone", null)
						)),
						section("Academic Background", 2, List.of(
								field("TEXT", 1, "College Name", true, "Enter college", null),
								field("TEXT", 2, "Degree", true, "Enter degree", null),
								field("NUMBER", 3, "Current CGPA", false, "Enter CGPA", null)
						)),
						section("Internship Details", 3, List.of(
								field("MULTI_SELECT", 1, "Preferred Domains", true, null, List.of("Backend", "Frontend", "AI/ML", "QA", "DevOps")),
								field("FILE", 2, "Upload Resume", true, null, null),
								field("TEXTAREA", 3, "Statement of Purpose", false, "Why do you want this internship?", null)
						))
				)),
				seed("Course Enrollment Form", "Course Enrollment Form", "Enroll students into available courses.", templateImage("Course Enrollment Form.png"), List.of(
						section("Student Information", 1, List.of(
								field("TEXT", 1, "Student Name", true, "Enter name", null),
								field("EMAIL", 2, "Email", true, "student@email.com", null),
								field("TEXT", 3, "Student ID", true, "Enter student id", null)
						)),
						section("Course Selection", 2, List.of(
								field("DROPDOWN", 1, "Course", true, null, List.of("Data Structures", "Operating Systems", "Database Systems", "Computer Networks")),
								field("TEXT", 2, "Semester", true, "Enter semester", null),
								field("CHECKBOX", 3, "Preferred Mode", false, null, List.of("Online", "Offline"))
						))
				)),
				seed("Bug Report Form", "Bug Report Form", "Capture reproducible bug reports from users or testers.", templateImage("Bug Report Form.png"), List.of(
						section("Issue Summary", 1, List.of(
								field("TEXT", 1, "Bug Title", true, "Short bug title", null),
								field("DROPDOWN", 2, "Severity", true, null, List.of("Critical", "High", "Medium", "Low")),
								field("TEXT", 3, "Environment", false, "Browser/OS/App version", null)
						)),
						section("Reproduction Steps", 2, List.of(
								field("TEXTAREA", 1, "Steps to Reproduce", true, "List exact steps", null),
								field("TEXTAREA", 2, "Expected Result", true, "Expected behavior", null),
								field("TEXTAREA", 3, "Actual Result", true, "Actual behavior", null)
						)),
						section("Attachments", 3, List.of(
								field("FILE", 1, "Upload Screenshot/Video", false, null, null)
						))
				)),
				seed("Appointment Booking Form", "Appointment Booking Form", "Book appointments with date/time and purpose.", templateImage("Appointment Booking Form.png"), List.of(
						section("Requester Details", 1, List.of(
								field("TEXT", 1, "Name", true, "Enter your name", null),
								field("EMAIL", 2, "Email", true, "name@email.com", null),
								field("PHONE", 3, "Phone", false, "Enter phone number", null)
						)),
						section("Appointment Details", 2, List.of(
								field("DATE", 1, "Preferred Date", true, null, null),
								field("TIME", 2, "Preferred Time", true, null, null),
								field("TEXTAREA", 3, "Purpose", true, "Reason for appointment", null)
						))
				))
		);
	}

	private TemplateSeed seed(String name, String title, String description, String imageUrl, List<Map<String, Object>> sections) {
		return new TemplateSeed(name, title, description, imageUrl, sections);
	}

	private String templateImage(String fileName) {
		return "/template-images/" + fileName;
	}

	private Map<String, Object> section(String sectionTitle, int sectionOrder, List<Map<String, Object>> fields) {
		Map<String, Object> section = new LinkedHashMap<>();
		section.put("sectionTitle", sectionTitle);
		section.put("sectionOrder", sectionOrder);
		section.put("fields", fields);
		return section;
	}

	private Map<String, Object> field(
			String type,
			int fieldOrder,
			String label,
			boolean required,
			String placeholder,
			List<String> options
	) {
		// Normalize incoming field types to the allowed set for templates:
		// Allowed: TEXT, TEXTAREA, DROPDOWN, CHECKBOX, FILE, RADIO
		String normalizedType = normalizeFieldType(type);

		Map<String, Object> config = new LinkedHashMap<>();
		config.put("label", label);
		config.put("required", required);
		// Preserve original type for UI hints, but force EMAIL/PHONE/DATE/TIME
		// to TEXT so these fields behave like plain text inputs.
		String originalType = (type == null) ? "TEXT" : type.toUpperCase();
		if ("EMAIL".equals(originalType)
				|| "PHONE".equals(originalType)
				|| "DATE".equals(originalType)
				|| "TIME".equals(originalType)) {
			originalType = "TEXT";
		}
		config.put("originalType", originalType);
		if (placeholder != null && !placeholder.isBlank()) {
			config.put("placeholder", placeholder);
		}
		if (options != null && !options.isEmpty()) {
			config.put("options", new ArrayList<>(options));
		}

		Map<String, Object> field = new LinkedHashMap<>();
		field.put("fieldType", normalizedType);
		field.put("fieldOrder", fieldOrder);
		field.put("fieldConfig", config);
		field.put("fieldStyle", Map.of());
		return field;
	}

	private String normalizeFieldType(String type) {
		if (type == null) return "TEXT";
		switch (type.toUpperCase()) {
			case "TEXT":
			case "EMAIL":
			case "PHONE":
			case "DATE":
			case "TIME":
			case "NUMBER":
				return "TEXT";
			case "TEXTAREA":
				return "TEXTAREA";
			case "DROPDOWN":
				return "DROPDOWN";
			case "MULTI_SELECT":
			case "CHECKBOX":
				return "CHECKBOX";
			case "FILE":
				return "FILE";
			case "RADIO":
			case "RATING":
				return "RADIO";
			default:
				return "TEXT";
		}
	}
}
