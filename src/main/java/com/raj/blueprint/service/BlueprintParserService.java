package com.raj.blueprint.service;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class BlueprintParserService {

    public record SectionDef(String slug, String label) {}

    public static final List<SectionDef> BLUEPRINT_SECTIONS = List.of(
        new SectionDef("executive_summary",     "Executive Summary"),
        new SectionDef("business_model_canvas", "Business Model Canvas"),
        new SectionDef("market_research",       "Market Research"),
        new SectionDef("competitor_analysis",   "Competitor Analysis"),
        new SectionDef("swot_analysis",         "SWOT Analysis"),
        new SectionDef("target_customers",      "Target Customers"),
        new SectionDef("value_proposition",     "Value Proposition"),
        new SectionDef("estimated_budget",      "Estimated Budget"),
        new SectionDef("revenue_model",         "Revenue Model"),
        new SectionDef("pricing_strategy",      "Pricing Strategy"),
        new SectionDef("marketing_strategy",    "Marketing Strategy"),
        new SectionDef("go_to_market_strategy", "Go-To-Market Strategy"),
        new SectionDef("funding_opportunities", "Funding Opportunities"),
        new SectionDef("government_schemes",    "Government Startup Schemes"),
        new SectionDef("legal_checklist",       "Legal Checklist"),
        new SectionDef("risk_analysis",         "Risk Analysis"),
        new SectionDef("growth_roadmap",        "Growth Roadmap"),
        new SectionDef("investor_pitch",        "Investor Pitch"),
        new SectionDef("action_plan_30_60_90",  "30-60-90 Day Action Plan")
    );

    private static final Map<String, String> HEADING_ALIASES = Map.ofEntries(
        Map.entry("budget estimation", "estimated_budget"),
        Map.entry("startup budget", "estimated_budget"),
        Map.entry("budget breakdown", "estimated_budget"),
        Map.entry("budget plan", "estimated_budget"),
        Map.entry("financial budget", "estimated_budget"),
        Map.entry("revenue streams", "revenue_model"),
        Map.entry("revenue generation", "revenue_model"),
        Map.entry("revenue strategy", "revenue_model"),
        Map.entry("monetization model", "revenue_model"),
        Map.entry("monetisation model", "revenue_model"),
        Map.entry("marketing plan", "marketing_strategy"),
        Map.entry("marketing and sales", "marketing_strategy"),
        Map.entry("sales and marketing", "marketing_strategy"),
        Map.entry("go to market", "go_to_market_strategy"),
        Map.entry("gtm strategy", "go_to_market_strategy"),
        Map.entry("launch strategy", "go_to_market_strategy"),
        Map.entry("risk assessment", "risk_analysis"),
        Map.entry("risks and mitigation", "risk_analysis"),
        Map.entry("risk management", "risk_analysis"),
        Map.entry("key risks", "risk_analysis"),
        Map.entry("growth plan", "growth_roadmap"),
        Map.entry("product roadmap", "growth_roadmap"),
        Map.entry("business roadmap", "growth_roadmap"),
        Map.entry("government schemes", "government_schemes"),
        Map.entry("startup schemes", "government_schemes"),
        Map.entry("government grants", "government_schemes"),
        Map.entry("grants and schemes", "government_schemes"),
        Map.entry("funding options", "funding_opportunities"),
        Map.entry("investment opportunities", "funding_opportunities"),
        Map.entry("fundraising", "funding_opportunities"),
        Map.entry("30 60 90 day plan", "action_plan_30_60_90"),
        Map.entry("action plan", "action_plan_30_60_90"),
        Map.entry("90 day plan", "action_plan_30_60_90"),
        Map.entry("30 60 90", "action_plan_30_60_90"),
        Map.entry("pitch deck", "investor_pitch"),
        Map.entry("elevator pitch", "investor_pitch"),
        Map.entry("investor deck", "investor_pitch")
    );

    private final Parser mdParser;
    private final HtmlRenderer htmlRenderer;
    private final PolicyFactory htmlSanitizer;

    public BlueprintParserService() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.mdParser = Parser.builder().extensions(extensions).build();
        this.htmlRenderer = HtmlRenderer.builder().extensions(extensions).build();
        this.htmlSanitizer = new HtmlPolicyBuilder()
            .allowElements("p", "br", "strong", "em", "b", "i", "u",
                "h1", "h2", "h3", "h4", "h5", "h6",
                "ul", "ol", "li", "blockquote", "pre", "code",
                "table", "thead", "tbody", "tr", "th", "td", "hr", "a", "span")
            .allowAttributes("href", "title").onElements("a")
            .allowAttributes("class").onElements("span", "p")
            .allowUrlProtocols("http", "https", "mailto")
            .toFactory();
    }

    public String mdToHtml(String text) {
        if (text == null || text.isBlank() || (text.startsWith("*") && text.endsWith("*"))) {
            String safePlaceholder = htmlSanitizer.sanitize(text == null ? "" : text);
            return "<p class=\"text-muted fst-italic\">" + safePlaceholder + "</p>";
        }
        String cleaned = Pattern.compile("^•\\s+", Pattern.MULTILINE).matcher(text).replaceAll("- ");
        Node document = mdParser.parse(cleaned);
        String rawHtml = htmlRenderer.render(document);
        return htmlSanitizer.sanitize(rawHtml);
    }

    public Map<String, String> parseSections(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            fillMissing(result);
            return result;
        }

        String[] parts = raw.split("(?m)^##\\s+");
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                String[] lines = part.split("\\R", 2);
                String heading = lines[0].replaceAll(":+$", "").trim();
                String body = lines.length > 1 ? lines[1].trim() : "";

                String matchedSlug = matchSlug(heading);
                if (matchedSlug != null) {
                    if (!result.containsKey(matchedSlug) || body.length() > result.get(matchedSlug).length()) {
                        result.put(matchedSlug, body);
                    }
                } else {
                    String safeKey = heading.toLowerCase().replaceAll("[^a-z0-9]+", "_");
                    if (!safeKey.isEmpty() && !result.containsKey(safeKey)) {
                        result.put(safeKey, body);
                    }
                }
            }
        } else {
            result.put("executive_summary", raw.trim());
        }

        fillMissing(result);
        return result;
    }

    private void fillMissing(Map<String, String> result) {
        for (SectionDef sec : BLUEPRINT_SECTIONS) {
            result.putIfAbsent(sec.slug(), "*" + sec.label() + " content will appear here after generation.*");
        }
    }

    private String normalise(String text) {
        String t = text.trim().replaceAll("^[\\d]+[\\.):\\-\\s]+", "");
        t = t.replace("**", "").replace("__", "");
        t = t.replaceAll("[-–—]", " ");
        return t.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private String matchSlug(String heading) {
        String normHeading = normalise(heading);
        for (Map.Entry<String, String> entry : HEADING_ALIASES.entrySet()) {
            if (normHeading.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        for (SectionDef sec : BLUEPRINT_SECTIONS) {
            String normLabel = normalise(sec.label());
            if (normHeading.contains(normLabel) || normLabel.contains(normHeading)) {
                return sec.slug();
            }
        }
        Set<String> headingWords = new HashSet<>(Arrays.asList(normHeading.split(" ")));
        for (SectionDef sec : BLUEPRINT_SECTIONS) {
            Set<String> labelWords = new HashSet<>(Arrays.asList(normalise(sec.label()).split(" ")));
            if (labelWords.isEmpty()) continue;
            long matches = labelWords.stream().filter(headingWords::contains).count();
            if ((double) matches / labelWords.size() >= 0.6) {
                return sec.slug();
            }
        }
        return null;
    }
}
