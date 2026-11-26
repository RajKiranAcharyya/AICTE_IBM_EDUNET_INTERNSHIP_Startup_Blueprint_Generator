package com.raj.blueprint.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.raj.blueprint.model.Blueprint;
import com.raj.blueprint.service.BlueprintParserService;
import com.raj.blueprint.service.GroqService;
import com.raj.blueprint.service.PdfExportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class BlueprintController {

    private final GroqService groqService;
    private final BlueprintParserService parserService;
    private final PdfExportService pdfExportService;

    // Bounded cache: auto-deletes every blueprint after 20 hours, max 200 entries
    private final Cache<String, Blueprint> blueprintStore = Caffeine.newBuilder()
        .expireAfterWrite(20, TimeUnit.HOURS)
        .maximumSize(200)
        .build();

    // In-memory visitor counter seeded at 34 (resets on container restart/sleep)
    private final AtomicLong visitorCount = new AtomicLong(34);

    public BlueprintController(GroqService groqService,
                               BlueprintParserService parserService,
                               PdfExportService pdfExportService) {
        this.groqService = groqService;
        this.parserService = parserService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("visitorCount", visitorCount.incrementAndGet());
        return "index";
    }

    @PostMapping("/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generate(
            @RequestParam(defaultValue = "") String idea,
            @RequestParam(defaultValue = "") String industry,
            @RequestParam(defaultValue = "") String country,
            @RequestParam(defaultValue = "") String audience,
            @RequestParam(defaultValue = "") String budget,
            @RequestParam(defaultValue = "") String goal,
            HttpSession session) {

        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("idea", idea.trim());
        formData.put("industry", industry.trim());
        formData.put("country", country.trim());
        formData.put("audience", audience.trim());
        formData.put("budget", budget.trim());
        formData.put("goal", goal.trim());

        List<String> missing = formData.entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .toList();

        if (!missing.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Please fill in: " + String.join(", ", missing)
            ));
        }

        try {
            Blueprint blueprint = groqService.generateBlueprint(formData);
            String bid = UUID.randomUUID().toString();
            blueprintStore.put(bid, blueprint);
            session.setAttribute("last_bid", bid);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "blueprint", blueprint,
                "bid", bid
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Generation failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/report")
    public String report(HttpSession session, Model model) {
        String bid = (String) session.getAttribute("last_bid");
        Blueprint blueprint = bid != null ? blueprintStore.getIfPresent(bid) : null;
        if (blueprint == null) {
            model.addAttribute("visitorCount", visitorCount.get());
            model.addAttribute("error", "No blueprint found or it has expired (auto-deleted after 20 hours). Please generate a new one.");
            return "index";
        }
        populateReportModel(model, blueprint, bid);
        return "report";
    }

    @GetMapping("/report/{bid}")
    public String reportById(@PathVariable String bid, Model model) {
        Blueprint blueprint = blueprintStore.getIfPresent(bid);
        if (blueprint == null) {
            model.addAttribute("visitorCount", visitorCount.get());
            model.addAttribute("error", "Blueprint not found or expired (auto-deleted after 20 hours). Please generate a new one.");
            return "index";
        }
        populateReportModel(model, blueprint, bid);
        return "report";
    }

    @GetMapping("/report/{bid}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String bid) {
        Blueprint blueprint = blueprintStore.getIfPresent(bid);
        if (blueprint == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdfBytes = pdfExportService.buildPdf(blueprint);
        String idea = blueprint.getMetadata().getOrDefault("idea", "blueprint");
        String slug = idea.substring(0, Math.min(40, idea.length()))
            .toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String filename = "startup-blueprint-" + (slug.isEmpty() ? "report" : slug) + ".pdf";

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(pdfBytes);
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> health() {
        return Map.of("status", "ok", "timestamp", Instant.now().toString());
    }

    private void populateReportModel(Model model, Blueprint blueprint, String bid) {
        Map<String, String> htmlSections = new LinkedHashMap<>();
        blueprint.getSections().forEach((slug, md) -> htmlSections.put(slug, parserService.mdToHtml(md)));

        model.addAttribute("blueprint", blueprint);
        model.addAttribute("htmlSections", htmlSections);
        model.addAttribute("sections", BlueprintParserService.BLUEPRINT_SECTIONS);
        model.addAttribute("bid", bid);
    }
}
