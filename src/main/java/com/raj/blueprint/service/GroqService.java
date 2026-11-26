package com.raj.blueprint.service;

import com.raj.blueprint.model.Blueprint;
import com.raj.blueprint.service.BlueprintParserService.SectionDef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class GroqService {

    private static final String AGENT_INSTRUCTIONS = """
        You are StartupMentor AI, an elite startup advisor and business strategist.

        YOUR EXPERTISE:
        - 20+ years of venture capital, startup incubation, and business development experience
        - Deep knowledge of global startup ecosystems (Silicon Valley, India, EU, Southeast Asia, Africa)
        - Expert in lean startup methodology, design thinking, and agile product development
        - Fluent in government startup schemes, grants, and incentive programmes for all countries
        - Master of financial modelling, pricing strategy, and unit economics
        - Specialist in B2B, B2C, D2C, SaaS, marketplace, and platform business models

        YOUR MENTORING STYLE:
        - Speak with authority, clarity, and actionable precision
        - Ground every recommendation in real market data and trends
        - Be optimistic but realistic — highlight genuine risks alongside opportunities
        - Tailor every response to the specific country's regulatory and cultural context
        - Provide specific numbers, timelines, percentages, and benchmarks wherever possible
        - Format all responses with clear headings, bullet points, and structured sections

        GOVERNMENT SCHEMES:
        - Always research and list country-specific startup schemes, grants, and tax incentives
        - For India: mention Startup India, SIDBI, Atal Innovation Mission, state-level schemes
        - For USA: mention SBA loans, SBIR/STTR grants, state-level programmes
        - For UK: mention Innovate UK, EIS/SEIS, Start Up Loans
        - For EU: mention Horizon Europe, EIC Accelerator, national schemes
        - For other countries: research and list the most relevant programmes

        RESPONSE QUALITY:
        - Every section must be substantive, specific, and actionable (not generic filler)
        - Use real competitor names, real pricing benchmarks, and real market data
        - Tailor legal, regulatory, and compliance advice to the specified country
        - Provide investor-ready language in the pitch section
        - Make the 30-60-90 day plan hyper-specific with clear deliverables

        TONE: Professional, inspiring, mentor-like — like a seasoned investor who genuinely wants you to succeed.
        """;

    private final RestClient restClient;
    private final BlueprintParserService parserService;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String modelId;

    public GroqService(BlueprintParserService parserService) {
        this.restClient = RestClient.create();
        this.parserService = parserService;
    }

    public Blueprint generateBlueprint(Map<String, String> formData) {
        List<SectionDef> part1 = BlueprintParserService.BLUEPRINT_SECTIONS.subList(0, 10);
        List<SectionDef> part2 = BlueprintParserService.BLUEPRINT_SECTIONS.subList(10, BlueprintParserService.BLUEPRINT_SECTIONS.size());

        String raw1 = callGroq(buildPrompt(formData, part1));
        String raw2 = callGroq(buildPrompt(formData, part2));

        Map<String, String> sections1 = parserService.parseSections(raw1);
        Map<String, String> sections2 = parserService.parseSections(raw2);

        Map<String, String> merged = new LinkedHashMap<>(sections1);
        for (Map.Entry<String, String> entry : sections2.entrySet()) {
            String slug = entry.getKey();
            String content = entry.getValue();
            boolean isPlaceholder = content.startsWith("*") && content.endsWith("*");
            if (!merged.containsKey(slug) || (!isPlaceholder && merged.get(slug).startsWith("*"))) {
                merged.put(slug, content);
            }
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("idea", formData.getOrDefault("idea", ""));
        metadata.put("industry", formData.getOrDefault("industry", ""));
        metadata.put("country", formData.getOrDefault("country", ""));
        metadata.put("audience", formData.getOrDefault("audience", ""));
        metadata.put("budget", formData.getOrDefault("budget", ""));
        metadata.put("goal", formData.getOrDefault("goal", ""));
        metadata.put("generated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));

        return new Blueprint(raw1 + "\n\n" + raw2, merged, metadata);
    }

    @SuppressWarnings("unchecked")
    private String callGroq(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY must be set in application.properties or environment variables.");
        }

        Map<String, Object> body = Map.of(
            "model", modelId,
            "messages", List.of(
                Map.of("role", "system", "content", AGENT_INSTRUCTIONS),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7,
            "max_tokens", 8000,
            "top_p", 0.95
        );

        try {
            Map<String, Object> response = restClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null && message.get("content") != null) {
                        return message.get("content").toString();
                    }
                }
            }
            throw new RuntimeException("Empty response received from Groq API.");
        } catch (Exception e) {
            throw new RuntimeException("Groq AI service error: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(Map<String, String> formData, List<SectionDef> subset) {
        StringBuilder sectionsList = new StringBuilder();
        for (int i = 0; i < subset.size(); i++) {
            sectionsList.append(i + 1).append(". ").append(subset.get(i).label()).append("\n");
        }

        String country = formData.getOrDefault("country", "");
        return String.format("""
            ===================================================
            STARTUP IDEA BRIEF
            ===================================================
            Startup Idea      : %s
            Industry          : %s
            Country           : %s
            Target Audience   : %s
            Available Budget  : %s
            Primary Goal      : %s
            ===================================================

            Generate a DETAILED Startup Business Blueprint for the %d sections listed
            below. Write at minimum 200 words per section. Be highly specific to the
            country, industry, and business idea provided. Do NOT stop early — every
            single section listed must be completed in full.

            SECTIONS TO GENERATE NOW:
            %s
            STRICT FORMAT RULES (follow exactly):
            - Start each section with "## " then the EXACT section name as listed above
            - Do NOT prefix headings with numbers (correct: "## Marketing Strategy",
              wrong: "## 11. Marketing Strategy")
            - Do NOT skip or merge any section
            - Use bullet points (-) for lists
            - Use sub-headings (###) for structure within a section
            - Include real numbers, market sizes, competitor names, and benchmarks
            - For Government Startup Schemes: list programmes specific to %s
            - After the final section write one short motivational closing paragraph

            Begin now — output all %d sections in order:
            """,
            formData.getOrDefault("idea", ""),
            formData.getOrDefault("industry", ""),
            country,
            formData.getOrDefault("audience", ""),
            formData.getOrDefault("budget", ""),
            formData.getOrDefault("goal", ""),
            subset.size(),
            sectionsList.toString().trim(),
            country,
            subset.size()
        ).trim();
    }
}
