package com.raj.blueprint.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.raj.blueprint.model.Blueprint;
import com.raj.blueprint.service.BlueprintParserService.SectionDef;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    private static final Color ACCENT_BLUE = new Color(37, 99, 235);
    private static final Color HEADER_DARK = new Color(30, 30, 46);
    private static final Color TEXT_DARK   = new Color(31, 31, 31);
    private static final Color TEXT_MUTED  = new Color(90, 90, 110);
    private static final Color LIGHT_TINT  = new Color(240, 245, 255);

    private static final List<Color> SECTION_COLORS = List.of(
        new Color(37, 99, 235), new Color(124, 58, 237), new Color(5, 150, 105),
        new Color(234, 88, 12), new Color(220, 38, 38), new Color(13, 148, 136),
        new Color(219, 39, 119), new Color(67, 56, 202), new Color(202, 138, 4),
        new Color(8, 145, 178)
    );

    public byte[] buildPdf(Blueprint blueprint) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte cb = writer.getDirectContent();
                    cb.setColorFill(ACCENT_BLUE);
                    cb.rectangle(0, PageSize.A4.getHeight() - 10, PageSize.A4.getWidth(), 10);
                    cb.fill();

                    Phrase footer = new Phrase(
                        "StartupBlueprint.AI  |  Powered by Groq AI & Java Spring Boot  |  Page " + writer.getPageNumber(),
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_MUTED)
                    );
                    ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                        PageSize.A4.getWidth() / 2, 24, 0);
                }
            });

            document.open();
            Map<String, String> meta = blueprint.getMetadata();

            // Cover Banner
            PdfPTable coverTable = new PdfPTable(1);
            coverTable.setWidthPercentage(100);
            PdfPCell coverCell = new PdfPCell();
            coverCell.setBackgroundColor(LIGHT_TINT);
            coverCell.setBorderColor(ACCENT_BLUE);
            coverCell.setBorderWidthLeft(4f);
            coverCell.setBorderWidthTop(0);
            coverCell.setBorderWidthRight(0);
            coverCell.setBorderWidthBottom(0);
            coverCell.setPadding(18);

            Paragraph pLabel = new Paragraph("STARTUP BUSINESS BLUEPRINT",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, ACCENT_BLUE));
            coverCell.addElement(pLabel);

            String idea = cleanText(meta.getOrDefault("idea", "Startup Blueprint"));
            Paragraph pTitle = new Paragraph(idea,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, HEADER_DARK));
            pTitle.setSpacingBefore(6);
            pTitle.setSpacingAfter(8);
            coverCell.addElement(pTitle);

            String details = String.format("%s  |  %s  |  Audience: %s  |  Budget: %s",
                cleanText(meta.getOrDefault("industry", "")),
                cleanText(meta.getOrDefault("country", "")),
                cleanText(meta.getOrDefault("audience", "")),
                cleanText(meta.getOrDefault("budget", ""))
            );
            Paragraph pDetails = new Paragraph(details,
                FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED));
            coverCell.addElement(pDetails);

            Paragraph pGen = new Paragraph("Generated: " + cleanText(meta.getOrDefault("generated", "")),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_MUTED));
            pGen.setSpacingBefore(4);
            coverCell.addElement(pGen);

            coverTable.addCell(coverCell);
            document.add(coverTable);

            String goal = cleanText(meta.getOrDefault("goal", ""));
            if (!goal.isBlank()) {
                Paragraph pGoalHdr = new Paragraph("BUSINESS GOAL",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, ACCENT_BLUE));
                pGoalHdr.setSpacingBefore(14);
                document.add(pGoalHdr);

                Paragraph pGoal = new Paragraph(goal,
                    FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_DARK));
                pGoal.setSpacingBefore(3);
                document.add(pGoal);
            }

            Map<String, String> sections = blueprint.getSections();
            List<SectionDef> defs = BlueprintParserService.BLUEPRINT_SECTIONS;

            for (int i = 0; i < defs.size(); i++) {
                SectionDef sec = defs.get(i);
                String content = sections.getOrDefault(sec.slug(), "");
                if (content.isBlank() || (content.startsWith("*") && content.endsWith("*"))) {
                    continue;
                }

                document.newPage();
                Color secColor = SECTION_COLORS.get(i % SECTION_COLORS.size());

                PdfPTable hdrTable = new PdfPTable(1);
                hdrTable.setWidthPercentage(100);
                PdfPCell hdrCell = new PdfPCell();
                hdrCell.setBackgroundColor(secColor);
                hdrCell.setBorder(Rectangle.NO_BORDER);
                hdrCell.setPadding(10);
                hdrCell.addElement(new Paragraph("SECTION " + (i + 1),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE)));
                hdrCell.addElement(new Paragraph(sec.label().toUpperCase(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.WHITE)));
                hdrTable.addCell(hdrCell);
                hdrTable.setSpacingAfter(10);
                document.add(hdrTable);

                String cleaned = cleanText(content);
                for (String line : cleaned.split("\\R")) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        document.add(new Paragraph(" "));
                        continue;
                    }
                    boolean isSubheading = trimmed.startsWith("###") ||
                        (trimmed.length() < 80 && (trimmed.endsWith(":") || trimmed.equals(trimmed.toUpperCase())));
                    trimmed = trimmed.replaceAll("^#{1,6}\\s*", "");

                    if (isSubheading) {
                        Paragraph sub = new Paragraph(trimmed,
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, secColor));
                        sub.setSpacingBefore(6);
                        sub.setSpacingAfter(2);
                        document.add(sub);
                    } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                        Paragraph bul = new Paragraph("•  " + trimmed.substring(2),
                            FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_DARK));
                        bul.setIndentationLeft(12);
                        bul.setSpacingAfter(3);
                        document.add(bul);
                    } else {
                        Paragraph para = new Paragraph(trimmed,
                            FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_DARK));
                        para.setSpacingAfter(4);
                        document.add(para);
                    }
                }
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile PDF: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text
            .replaceAll("\\*{1,3}(.+?)\\*{1,3}", "$1")
            .replaceAll("_{1,2}(.+?)_{1,2}", "$1")
            .replaceAll("<[^>]+>", "")
            .replace("\u2013", "-").replace("\u2014", "--")
            .replace("\u2018", "'").replace("\u2019", "'")
            .replace("\u201c", "\"").replace("\u201d", "\"")
            .replace("\u2022", "-").replace("\u2026", "...");
    }
}
