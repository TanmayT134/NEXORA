
package com.nexora.nexora_backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.nexora_backend.dto.ResumeAnalysisAiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeAnalysisAiService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public ResumeAnalysisAiResponse analyzeResume(String resumeText) {

        if (resumeText == null || resumeText.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resume text is empty"
            );
        }

        String prompt = buildAnalysisPrompt(resumeText);

        String aiResponse = geminiService.generateResponse(prompt);

        if (aiResponse == null || aiResponse.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini returned an empty response"
            );
        }

        String jsonResponse = extractJson(aiResponse);

        try {
            return objectMapper.readValue(
                    jsonResponse,
                    ResumeAnalysisAiResponse.class
            );

        } catch (JsonProcessingException exception) {

            String errorMessage = exception.getOriginalMessage();

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini JSON parsing failed: " + errorMessage
            );
        }
    }

    private String buildAnalysisPrompt(String resumeText) {

        return """
                You are NEXORA's professional resume analysis engine.

                Analyse the resume text provided below and extract
                information accurately.

                STRICT INSTRUCTIONS:

                1. Extract only information explicitly supported by
                   the resume.

                2. Never invent candidate details, skills, companies,
                   qualifications, projects or certifications.

                3. If a field is missing or unclear, return null.

                4. If a list has no relevant information, return [].

                5. Do not estimate skill proficiency or years of
                   experience without explicit evidence.

                6. Skill categorization rules:

					- PROGRAMMING_LANGUAGE:
					  Java, JavaScript, Python, C, C++, SQL.

					- FRAMEWORK:
					  Spring Boot, Spring MVC, Hibernate, React.js,
					  Angular, Bootstrap, Express.js.

					- DATABASE:
					  MySQL, PostgreSQL, MongoDB, Oracle.

					- WEB_TECHNOLOGY:
					  HTML, HTML5, CSS, CSS3, Axios,
					  DOM Manipulation, Responsive Web Design.

					- BACKEND_TECHNOLOGY:
					  JDBC, Servlets, Jakarta Servlets, JSP,
					  Java Server Pages, REST APIs, RESTful APIs.

					- TOOL:
					  Git, GitHub, Postman, Eclipse IDE, VS Code.

					- CLOUD:
					  AWS, Azure, Google Cloud.

					- AI_ML:
					  TensorFlow, PyTorch, Machine Learning,
					  Deep Learning, NLP.

					- SOFT_SKILL:
					  Communication, Leadership, Teamwork.

					- OTHER:
					  Use only when no other category is appropriate.

					Always return one valid category from the specified enum.
					Do not classify a technology as OTHER when a suitable
					category exists.

                7. Use these exact employment types:
                   FULL_TIME,
                   PART_TIME,
                   INTERNSHIP,
                   FREELANCE,
                   CONTRACT,
                   APPRENTICESHIP,
                   OTHER.

                8. DATE EXTRACTION RULES:

				   For every startDate, endDate, issueDate and expiryDate,
				   extract the date as accurately as possible.

				   Return dates in yyyy-MM-dd format using these rules:

				   - If the resume provides an exact day, month and year:
				     return that exact date and precision "EXACT".

				   - If the resume provides only month and year:
				     use the first day of that month as the normalized date
				     and return precision "MONTH".

				   - If the resume provides only a year:
				     use January 1 of that year as the normalized date
				     and return precision "YEAR".

				   - If the date is missing or unclear:
				     return both date and precision as null.

				   NEVER invent a day, month or year.

				   Every date must have a corresponding precision field.

                9. For currentlyWorking, return true only when
                   the resume clearly indicates an ongoing role.
                   Otherwise return false or null when unclear.

                10. Return valid JSON only.
                    Do not include Markdown code fences,
                    explanations or additional text.

                11. Every field declared as a string must contain a JSON string
			    or null. Never return an array or object for a string field.

				12. For technologiesUsed, return a single comma-separated
				    string, not a JSON array.

				13. For description, return a single plain-text string,
				    not a JSON array.

				14. Do not return nested arrays or objects except for the
				    explicitly defined top-level lists:
				    skills, experiences, projects, education, certifications.

                REQUIRED JSON STRUCTURE:

                {
                  "candidateName": null,
                  "candidateEmail": null,
                  "candidatePhone": null,
                  "professionalSummary": null,
                  "currentJobTitle": null,
                  "skills": [],
                  "experiences": [],
                  "projects": [],
                  "education": [],
                  "certifications": []
                }

                Every skill object must contain:
                skillName, category, proficiencyLevel,
                yearsOfExperience.

                Every experience object must contain:
				jobTitle, companyName, employmentType,
				startDate, startDatePrecision,
				endDate, endDatePrecision,
				currentlyWorking, description, technologiesUsed.

                Every project object must contain:
				projectName, description, projectRole,
				technologiesUsed, projectUrl,
				startDate, startDatePrecision,
				endDate, endDatePrecision.

                Every education object must contain:
                degree, fieldOfStudy, institutionName,
                graduationYear, gradeOrCgpa, startYear,
                endYear, description.

                Every certification object must contain:
				certificationName, issuingOrganization,
				issueDate, issueDatePrecision,
				expiryDate, expiryDatePrecision,
				credentialId, credentialUrl.

                RESUME TEXT:

                ---
                %s
                ---
                """.formatted(resumeText);
    }

    private String extractJson(String response) {

        String cleaned = response.trim();

        if (cleaned.startsWith("```")) {

            cleaned = cleaned.replaceFirst(
                    "^```(?:json)?\\s*",
                    ""
            );

            cleaned = cleaned.replaceFirst(
                    "\\s*```$",
                    ""
            ).trim();
        }

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace < 0 || lastBrace < firstBrace) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini did not return a valid JSON object"
            );
        }

        return cleaned.substring(firstBrace, lastBrace + 1);
    }
}
