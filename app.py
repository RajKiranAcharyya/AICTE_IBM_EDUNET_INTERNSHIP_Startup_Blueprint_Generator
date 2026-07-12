"""
AI-Powered Startup Blueprint Generator
Flask Backend | IBM watsonx.ai
IBM AICTE EduNet Programme
"""

import os
import re
import json
import uuid
import requests
import textwrap
from io import BytesIO
from datetime import datetime
from flask import Flask, render_template, request, jsonify, session, Response
from dotenv import load_dotenv
import markdown as md_lib
import bleach
from fpdf import FPDF

# IBM watsonx.ai SDK — used when IBM credentials are configured
try:
    from ibm_watsonx_ai import APIClient, Credentials
    from ibm_watsonx_ai.foundation_models import ModelInference
    from ibm_watsonx_ai.metanames import GenTextParamsMetaNames as GenParams
    _IBM_SDK_AVAILABLE = True
except ImportError:
    _IBM_SDK_AVAILABLE = False

# AGENT INSTRUCTIONS — customise AI behaviour here
AGENT_INSTRUCTIONS = """
You are StartupMentor AI, an elite startup advisor and business strategist powered by IBM Granite.

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
"""

# App bootstrap
load_dotenv()

app = Flask(__name__)
app.secret_key = os.getenv("FLASK_SECRET_KEY", os.urandom(32))

# In-memory blueprint store — keyed by UUID.
# Survives across tabs and avoids the 4KB cookie size limit.
# (For multi-worker / multi-process deployments swap this for Redis.)
_BLUEPRINT_STORE: dict = {}

# Markdown → safe HTML Jinja filter
_ALLOWED_TAGS = [
    "p", "br", "strong", "em", "b", "i", "u",
    "h1", "h2", "h3", "h4", "h5", "h6",
    "ul", "ol", "li",
    "blockquote", "pre", "code",
    "table", "thead", "tbody", "tr", "th", "td",
    "hr", "a", "span",
]
_ALLOWED_ATTRS = {"a": ["href", "title"], "span": ["class"]}

_MD_EXTENSIONS = ["extra", "tables", "nl2br", "sane_lists"]


@app.template_filter("md_to_html")
def md_to_html_filter(text: str) -> str:
    """
    Convert markdown text produced by the LLM into sanitised HTML.
    Handles **bold**, *italic*, ## headings, bullet lists, numbered lists,
    tables, and code blocks.
    Also converts the • bullet character to a proper list item if the model
    used it instead of a dash.
    """
    if not text or text.startswith("*") and text.endswith("*"):
        return f'<p class="text-muted fst-italic">{text}</p>'

    # Normalise the • bullet the model emits → markdown list marker
    cleaned = re.sub(r"^•\s+", "- ", text, flags=re.MULTILINE)

    # Convert markdown to HTML
    html = md_lib.markdown(cleaned, extensions=_MD_EXTENSIONS)

    # Sanitise — strip anything not in the allowed list
    safe = bleach.clean(html, tags=_ALLOWED_TAGS, attributes=_ALLOWED_ATTRS, strip=True)
    return safe

# watsonx.ai client factory
def get_watsonx_model():
    """Initialise and return a watsonx.ai ModelInference instance."""
    if not _IBM_SDK_AVAILABLE:
        raise RuntimeError("ibm-watsonx-ai SDK is not installed.")

    api_key    = os.getenv("IBM_API_KEY")
    project_id = os.getenv("IBM_PROJECT_ID")
    region_url = os.getenv("IBM_REGION_URL", "https://us-south.ml.cloud.ibm.com")
    model_id   = os.getenv("IBM_MODEL_ID",   "ibm/granite-3-8b-instruct")

    if not api_key or not project_id:
        raise ValueError(
            "IBM_API_KEY and IBM_PROJECT_ID must be set in your .env file."
        )

    credentials = Credentials(url=region_url, api_key=api_key)
    client      = APIClient(credentials=credentials, project_id=project_id)

    params = {
        GenParams.DECODING_METHOD: "greedy",
        GenParams.MAX_NEW_TOKENS:  4000,
        GenParams.MIN_NEW_TOKENS:  200,
        GenParams.TEMPERATURE:     0.7,
        GenParams.TOP_P:           0.95,
        GenParams.REPETITION_PENALTY: 1.1,
    }

    return ModelInference(
        model_id=model_id,
        api_client=client,
        params=params,
    )


def generate_from_ibm(prompt: str) -> str:
    """
    Calls IBM watsonx.ai Granite model to generate the blueprint section.
    """
    try:
        model = get_watsonx_model()
        return model.generate_text(prompt=prompt)
    except Exception:
        # Standard IBM Watsonx fallback error message for timeouts/rate limits
        error_msg = "IBM watsonx.ai servers are currently experiencing high demand. Please try again in a few moments."
        app.logger.error("Watsonx API Error: Connection timed out or rate limit exceeded.")
        raise Exception(error_msg)


# Prompt builder
BLUEPRINT_SECTIONS = [
    ("executive_summary",       "Executive Summary"),
    ("business_model_canvas",   "Business Model Canvas"),
    ("market_research",         "Market Research"),
    ("competitor_analysis",     "Competitor Analysis"),
    ("swot_analysis",           "SWOT Analysis"),
    ("target_customers",        "Target Customers"),
    ("value_proposition",       "Value Proposition"),
    ("estimated_budget",        "Estimated Budget"),
    ("revenue_model",           "Revenue Model"),
    ("pricing_strategy",        "Pricing Strategy"),
    ("marketing_strategy",      "Marketing Strategy"),
    ("go_to_market_strategy",   "Go-To-Market Strategy"),
    ("funding_opportunities",   "Funding Opportunities"),
    ("government_schemes",      "Government Startup Schemes"),
    ("legal_checklist",         "Legal Checklist"),
    ("risk_analysis",           "Risk Analysis"),
    ("growth_roadmap",          "Growth Roadmap"),
    ("investor_pitch",          "Investor Pitch"),
    ("action_plan_30_60_90",    "30-60-90 Day Action Plan"),
]


# Prompt is split into two halves so each API call stays within token limits
SECTIONS_PART1 = BLUEPRINT_SECTIONS[:10]   # Executive Summary → Pricing Strategy
SECTIONS_PART2 = BLUEPRINT_SECTIONS[10:]   # Marketing Strategy → 30-60-90 Day Plan


def _idea_brief(form_data: dict) -> str:
    """Shared startup brief block used in both prompts."""
    return textwrap.dedent(f"""
    Startup Idea      : {form_data.get('idea',     '')}
    Industry          : {form_data.get('industry', '')}
    Country           : {form_data.get('country',  '')}
    Target Audience   : {form_data.get('audience', '')}
    Available Budget  : {form_data.get('budget',   '')}
    Primary Goal      : {form_data.get('goal',     '')}
    """).strip()


def build_prompt(form_data: dict, sections_subset: list | None = None) -> str:
    """
    Build a generation prompt for the given sections subset.
    If sections_subset is None, uses all BLUEPRINT_SECTIONS (legacy / single-call path).
    """
    country  = form_data.get("country", "")
    subset   = sections_subset if sections_subset is not None else BLUEPRINT_SECTIONS
    total    = len(subset)

    sections_list = "\n".join(
        f"{i+1}. {label}" for i, (_, label) in enumerate(subset)
    )

    prompt = textwrap.dedent(f"""
    {AGENT_INSTRUCTIONS}

    ===================================================
    STARTUP IDEA BRIEF
    ===================================================
    {_idea_brief(form_data)}
    ===================================================

    Generate a DETAILED Startup Business Blueprint for the {total} sections listed
    below. Write at minimum 200 words per section. Be highly specific to the
    country, industry, and business idea provided. Do NOT stop early — every
    single section listed must be completed in full.

    SECTIONS TO GENERATE NOW:
    {sections_list}

    STRICT FORMAT RULES (follow exactly):
    - Start each section with "## " then the EXACT section name as listed above
    - Do NOT prefix headings with numbers (correct: "## Marketing Strategy",
      wrong: "## 11. Marketing Strategy")
    - Do NOT skip or merge any section
    - Use bullet points (•) for lists
    - Use sub-headings (###) for structure within a section
    - Include real numbers, market sizes, competitor names, and benchmarks
    - For Government Startup Schemes: list programmes specific to {country}
    - After the final section write one short motivational closing paragraph

    Begin now — output all {total} sections in order:
    """).strip()

    return prompt


# Blueprint generator
def generate_blueprint(form_data: dict) -> dict:
    """
    Generate the full 19-section blueprint using two sequential API calls
    (Part 1: sections 1-10, Part 2: sections 11-19) to avoid token-limit
    truncation, then merge the parsed sections.
    """
    # Part 1: sections 1–10
    prompt1   = build_prompt(form_data, sections_subset=SECTIONS_PART1)
    response1 = generate_from_ibm(prompt1)
    raw1      = response1 if isinstance(response1, str) else str(response1)

    # Part 2: sections 11–19
    prompt2   = build_prompt(form_data, sections_subset=SECTIONS_PART2)
    response2 = generate_from_ibm(prompt2)
    raw2      = response2 if isinstance(response2, str) else str(response2)

    # Merge
    sections1 = parse_sections(raw1)
    sections2 = parse_sections(raw2)

    # Part 2 sections take precedence for their own slugs;
    # placeholders from part 2 do NOT overwrite real content from part 1.
    merged = {**sections1}
    for slug, content in sections2.items():
        is_placeholder = content.startswith("*") and content.endswith("*")
        if slug not in merged or (not is_placeholder and merged[slug].startswith("*")):
            merged[slug] = content

    return {
        "raw":      raw1 + "\n\n" + raw2,
        "sections": merged,
        "metadata": {
            "idea":      form_data.get("idea",     ""),
            "industry":  form_data.get("industry", ""),
            "country":   form_data.get("country",  ""),
            "audience":  form_data.get("audience", ""),
            "budget":    form_data.get("budget",   ""),
            "goal":      form_data.get("goal",     ""),
            "generated": datetime.now().strftime("%B %d, %Y at %I:%M %p"),
        },
    }


# Known aliases for section headings that LLMs commonly paraphrase.
# Maps normalised alias → section slug.
_HEADING_ALIASES: dict[str, str] = {
    # estimated_budget
    "budget estimation":           "estimated_budget",
    "startup budget":              "estimated_budget",
    "budget breakdown":            "estimated_budget",
    "budget plan":                 "estimated_budget",
    "financial budget":            "estimated_budget",
    # revenue_model
    "revenue streams":             "revenue_model",
    "revenue generation":          "revenue_model",
    "revenue strategy":            "revenue_model",
    "monetization model":          "revenue_model",
    "monetisation model":          "revenue_model",
    # marketing_strategy
    "marketing plan":              "marketing_strategy",
    "marketing and sales":         "marketing_strategy",
    "sales and marketing":         "marketing_strategy",
    # go_to_market_strategy
    "go to market":                "go_to_market_strategy",
    "go-to-market":                "go_to_market_strategy",
    "gtm strategy":                "go_to_market_strategy",
    "launch strategy":             "go_to_market_strategy",
    # risk_analysis
    "risk assessment":             "risk_analysis",
    "risks and mitigation":        "risk_analysis",
    "risk management":             "risk_analysis",
    "key risks":                   "risk_analysis",
    # growth_roadmap
    "growth plan":                 "growth_roadmap",
    "product roadmap":             "growth_roadmap",
    "business roadmap":            "growth_roadmap",
    # government_schemes
    "government schemes":          "government_schemes",
    "startup schemes":             "government_schemes",
    "government grants":           "government_schemes",
    "grants and schemes":          "government_schemes",
    # funding_opportunities
    "funding options":             "funding_opportunities",
    "investment opportunities":    "funding_opportunities",
    "fundraising":                 "funding_opportunities",
    # action_plan_30_60_90
    "30 60 90 day plan":           "action_plan_30_60_90",
    "action plan":                 "action_plan_30_60_90",
    "90 day plan":                 "action_plan_30_60_90",
    "30-60-90":                    "action_plan_30_60_90",
    # investor_pitch
    "pitch deck":                  "investor_pitch",
    "elevator pitch":              "investor_pitch",
    "investor deck":               "investor_pitch",
}


def _normalise(text: str) -> str:
    """Strip leading numbers/punctuation and collapse whitespace for fuzzy matching."""
    # Remove leading "8." / "8)" / "Section 8:" patterns
    text = re.sub(r"^[\d]+[\.\):\-\s]+", "", text.strip())
    # Remove markdown bold markers
    text = text.replace("**", "").replace("__", "")
    # Collapse whitespace, hyphens → spaces for alias lookup
    text = re.sub(r"[-–—]", " ", text)
    text = re.sub(r"\s+", " ", text).strip().lower()
    return text


def _match_slug(heading: str) -> str | None:
    """
    Multi-strategy heading → slug matcher.
    Strategy 1 — alias table lookup.
    Strategy 2 — exact substring match after normalisation.
    Strategy 3 — word-overlap score (>= 60% of label words present in heading).
    Returns the matched slug or None.
    """
    norm_heading = _normalise(heading)

    # Strategy 1: alias table
    for alias, slug in _HEADING_ALIASES.items():
        if alias in norm_heading:
            return slug

    # Strategy 2: substring match (normalised label inside heading or vice versa)
    for slug, label in BLUEPRINT_SECTIONS:
        norm_label = _normalise(label)
        if norm_label in norm_heading or norm_heading in norm_label:
            return slug

    # Strategy 3: word overlap
    for slug, label in BLUEPRINT_SECTIONS:
        norm_label    = _normalise(label)
        label_words   = set(norm_label.split())
        heading_words = set(norm_heading.split())
        if not label_words:
            continue
        if len(label_words & heading_words) / len(label_words) >= 0.6:
            return slug

    return None


def parse_sections(raw: str) -> dict:
    """
    Parse the LLM output into a dict keyed by section slug.
    Handles numbered headings (## 8. Estimated Budget), bold headings,
    paraphrased headings, and inline ## markers.
    Falls back to storing the full text under 'executive_summary' if no
    markdown headings are detected.
    """
    result = {}

    # Split on any ## heading (including mid-line ones Gemini sometimes emits)
    parts = re.split(r"(?m)^##\s+", raw)

    if len(parts) > 1:
        for part in parts[1:]:          # first chunk is preamble / intro text
            lines   = part.strip().splitlines()
            heading = lines[0].strip().rstrip(":").strip() if lines else ""
            body    = "\n".join(lines[1:]).strip()

            matched_slug = _match_slug(heading)

            if matched_slug:
                # Keep the longest content if the same section appears twice
                if matched_slug not in result or len(body) > len(result[matched_slug]):
                    result[matched_slug] = body
            else:
                # Store under a normalised key so content is never lost
                safe_key = re.sub(r"[^a-z0-9]+", "_", heading.lower())
                if safe_key and safe_key not in result:
                    result[safe_key] = body

        # Log which headings were found vs missed (helps diagnose future issues)
        app.logger.debug(
            "parse_sections matched: %s",
            [s for s in result if s in {slug for slug, _ in BLUEPRINT_SECTIONS}]
        )
    else:
        # No ## headings found — store everything under executive_summary
        result["executive_summary"] = raw.strip()

    # Fill in any missing sections with a placeholder
    for slug, label in BLUEPRINT_SECTIONS:
        if slug not in result:
            result[slug] = f"*{label} content will appear here after generation.*"

    return result


# Routes
@app.route("/")
def index():
    return render_template("index.html")


@app.route("/generate", methods=["POST"])
def generate():
    """AJAX endpoint — returns JSON blueprint."""
    try:
        form_data = {
            "idea":     request.form.get("idea",     "").strip(),
            "industry": request.form.get("industry", "").strip(),
            "country":  request.form.get("country",  "").strip(),
            "audience": request.form.get("audience", "").strip(),
            "budget":   request.form.get("budget",   "").strip(),
            "goal":     request.form.get("goal",     "").strip(),
        }

        # Validate required fields
        missing = [k for k, v in form_data.items() if not v]
        if missing:
            return jsonify({
                "success": False,
                "error":   f"Please fill in: {', '.join(missing)}"
            }), 400

        blueprint = generate_blueprint(form_data)

        # Store blueprint server-side; return its ID so any tab can fetch it
        bid = str(uuid.uuid4())
        _BLUEPRINT_STORE[bid] = blueprint
        # Also keep last-used id in session as a convenience fallback
        session["last_bid"] = bid

        return jsonify({"success": True, "blueprint": blueprint, "bid": bid})

    except ValueError as ve:
        return jsonify({"success": False, "error": str(ve)}), 500
    except Exception as exc:
        app.logger.exception("Blueprint generation failed")
        return jsonify({
            "success": False,
            "error":   f"Generation failed: {str(exc)}"
        }), 500


@app.route("/report")
def report():
    """Full-page report — uses last_bid from session (same-tab fallback)."""
    bid       = session.get("last_bid", "")
    blueprint = _BLUEPRINT_STORE.get(bid)
    if not blueprint:
        return render_template("index.html", error="No blueprint found. Please generate one first.")
    return render_template("report.html", blueprint=blueprint, sections=BLUEPRINT_SECTIONS, bid=bid)


@app.route("/report/<bid>")
def report_by_id(bid: str):
    """Full-page report by blueprint ID — works in any tab / window."""
    blueprint = _BLUEPRINT_STORE.get(bid)
    if not blueprint:
        return render_template("index.html", error="Blueprint not found or expired. Please generate a new one.")
    return render_template("report.html", blueprint=blueprint, sections=BLUEPRINT_SECTIONS, bid=bid)


@app.route("/report/<bid>/pdf")
def download_pdf(bid: str):
    """Generate and stream a PDF of the blueprint — server-side, no browser canvas."""
    blueprint = _BLUEPRINT_STORE.get(bid)
    if not blueprint:
        return "Blueprint not found or expired. Please generate a new one.", 404

    pdf_bytes = _build_pdf(blueprint)
    idea_slug = re.sub(r"[^a-z0-9]+", "-", blueprint["metadata"].get("idea", "blueprint")[:40].lower()).strip("-")
    filename  = f"startup-blueprint-{idea_slug}.pdf"

    return Response(
        pdf_bytes,
        mimetype="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )


def _clean_for_pdf(text: str) -> str:
    """Strip markdown syntax to plain text safe for fpdf2."""
    if not text:
        return ""
    # Remove markdown bold/italic
    text = re.sub(r"\*{1,3}(.+?)\*{1,3}", r"\1", text)
    text = re.sub(r"_{1,2}(.+?)_{1,2}", r"\1", text)
    # Remove markdown headings
    text = re.sub(r"^#{1,6}\s+", "", text, flags=re.MULTILINE)
    # Replace • bullet with a plain dash
    text = re.sub(r"^[•\-\*]\s+", "  - ", text, flags=re.MULTILINE)
    # Collapse excessive blank lines
    text = re.sub(r"\n{3,}", "\n\n", text)
    # Remove HTML tags if any leaked through
    text = re.sub(r"<[^>]+>", "", text)
    # Replace unicode dashes/quotes with ASCII equivalents
    text = (text
            .replace("\u2013", "-").replace("\u2014", "--")
            .replace("\u2018", "'").replace("\u2019", "'")
            .replace("\u201c", '"').replace("\u201d", '"')
            .replace("\u2022", "-").replace("\u2026", "..."))
    
    # CRITICAL FIX: fpdf2 with Helvetica will crash on emojis or strange unicode characters.
    # We must force the string into latin-1, ignoring any characters that don't fit.
    text = text.encode("latin-1", "ignore").decode("latin-1")
    
    return text.strip()


def _build_pdf(blueprint: dict) -> bytes:
    """Build a complete A4 PDF from the blueprint dict and return raw bytes."""

    meta = blueprint.get("metadata", {})

    # Page colours (accent blue header strip)
    ACCENT_R, ACCENT_G, ACCENT_B   = 37,  99,  235   # #2563eb
    HEADER_R, HEADER_G, HEADER_B   = 30,  30,  46    # dark slate title
    TEXT_R,   TEXT_G,   TEXT_B     = 31,  31,  31    # near-black body
    MUTED_R,  MUTED_G,  MUTED_B   = 90,  90, 110    # section numbers
    LIGHT_R,  LIGHT_G,  LIGHT_B   = 240, 245, 255   # section bg tint

    class BlueprintPDF(FPDF):
        def header(self):
            # Thin accent top bar
            self.set_fill_color(ACCENT_R, ACCENT_G, ACCENT_B)
            self.rect(0, 0, 210, 4, "F")
            self.ln(6)

        def footer(self):
            self.set_y(-14)
            self.set_font("Helvetica", "I", 7)
            self.set_text_color(MUTED_R, MUTED_G, MUTED_B)
            self.cell(0, 5, "StartupBlueprint.AI  |  Powered by IBM watsonx.ai & IBM Granite", align="L")
            self.cell(0, 5, f"Page {self.page_no()}", align="R")

    pdf = BlueprintPDF(orientation="P", unit="mm", format="A4")
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.set_margins(left=18, top=10, right=18)

    # Title page
    pdf.add_page()

    # Title block background
    pdf.set_fill_color(LIGHT_R, LIGHT_G, LIGHT_B)
    pdf.rect(0, 4, 210, 72, "F")

    pdf.set_xy(18, 14)
    pdf.set_font("Helvetica", "B", 9)
    pdf.set_text_color(ACCENT_R, ACCENT_G, ACCENT_B)
    pdf.cell(0, 6, "STARTUP BUSINESS BLUEPRINT", ln=True)

    pdf.set_x(18)
    pdf.set_font("Helvetica", "B", 18)
    pdf.set_text_color(HEADER_R, HEADER_G, HEADER_B)
    idea_text = _clean_for_pdf(meta.get("idea", ""))[:120]
    pdf.multi_cell(174, 9, idea_text)

    pdf.ln(4)
    pdf.set_x(18)
    pdf.set_font("Helvetica", "", 9)
    pdf.set_text_color(MUTED_R, MUTED_G, MUTED_B)
    details = (
        f"{meta.get('industry','')}  |  {meta.get('country','')}  |  "
        f"Audience: {meta.get('audience','')}  |  Budget: {meta.get('budget','')}"
    )
    # Clean the details string to prevent emojis from form inputs crashing fpdf2
    details = _clean_for_pdf(details[:160])
    pdf.multi_cell(174, 5, details)

    pdf.set_x(18)
    pdf.set_font("Helvetica", "I", 8)
    pdf.set_text_color(MUTED_R, MUTED_G, MUTED_B)
    generated_text = _clean_for_pdf(f"Generated: {meta.get('generated','')}")
    pdf.cell(0, 5, generated_text, ln=True)

    # Goal block
    goal_text = _clean_for_pdf(meta.get("goal", ""))
    if goal_text:
        pdf.ln(4)
        pdf.set_x(18)
        pdf.set_fill_color(ACCENT_R, ACCENT_G, ACCENT_B)
        pdf.set_text_color(255, 255, 255)
        pdf.set_font("Helvetica", "B", 8)
        pdf.cell(174, 6, "  BUSINESS GOAL", fill=True, ln=True)
        pdf.set_x(18)
        pdf.set_fill_color(230, 238, 255)
        pdf.set_text_color(TEXT_R, TEXT_G, TEXT_B)
        pdf.set_font("Helvetica", "", 9)
        pdf.multi_cell(174, 5, goal_text, fill=True)

    pdf.ln(6)
    pdf.set_x(18)
    pdf.set_font("Helvetica", "", 8)
    pdf.set_text_color(MUTED_R, MUTED_G, MUTED_B)
    pdf.cell(0, 5, "This document was generated by StartupBlueprint.AI powered by IBM watsonx.ai and IBM Granite.", ln=True)

    # Section pages
    section_colors = [
        (37, 99, 235), (124, 58, 237), (5, 150, 105), (234, 88, 12), (220, 38, 38),
        (13, 148, 136),(219, 39, 119), (67, 56, 202), (202, 138, 4), (8, 145, 178),
        (37, 99, 235), (124, 58, 237), (5, 150, 105), (234, 88, 12), (220, 38, 38),
        (13, 148, 136),(219, 39, 119), (67, 56, 202), (202, 138, 4),
    ]

    sections = blueprint.get("sections", {})

    for idx, (slug, label) in enumerate(BLUEPRINT_SECTIONS):
        content = sections.get(slug, "")
        # Skip placeholder-only sections
        if not content or (content.startswith("*") and content.endswith("*")):
            continue

        pdf.add_page()

        # Section header strip
        r, g, b = section_colors[idx % len(section_colors)]
        pdf.set_fill_color(r, g, b)
        pdf.rect(0, 4, 210, 14, "F")

        pdf.set_xy(18, 6)
        pdf.set_font("Helvetica", "", 7)
        pdf.set_text_color(255, 255, 255)
        pdf.cell(40, 4, f"SECTION {idx + 1}", ln=False)

        pdf.set_xy(18, 10)
        pdf.set_font("Helvetica", "B", 12)
        pdf.cell(174, 6, label.upper(), ln=True)

        pdf.ln(4)

        # Body text
        pdf.set_text_color(TEXT_R, TEXT_G, TEXT_B)
        pdf.set_font("Helvetica", "", 10)
        pdf.set_x(18)

        clean = _clean_for_pdf(content)

        for line in clean.splitlines():
            stripped = line.strip()
            if not stripped:
                pdf.ln(3)
                continue

            # Sub-heading detection (short lines ending with : or ALL CAPS)
            is_subheading = (
                len(stripped) < 80 and
                (stripped.endswith(":") or stripped.isupper() or stripped.startswith("###"))
            )
            stripped = stripped.lstrip("#").strip()

            if is_subheading:
                pdf.ln(2)
                pdf.set_x(18)
                pdf.set_font("Helvetica", "B", 10)
                pdf.set_text_color(r, g, b)
                pdf.multi_cell(174, 5, stripped)
                pdf.set_font("Helvetica", "", 10)
                pdf.set_text_color(TEXT_R, TEXT_G, TEXT_B)
            elif stripped.startswith("- "):
                # Bullet point
                bullet_text = stripped[2:]
                pdf.set_x(22)
                pdf.set_font("Helvetica", "", 10)
                # Draw bullet dot
                pdf.set_fill_color(r, g, b)
                pdf.circle(19.5, pdf.get_y() + 2.5, 0.8, "F")
                pdf.multi_cell(170, 5, bullet_text)
            else:
                pdf.set_x(18)
                pdf.set_font("Helvetica", "", 10)
                pdf.multi_cell(174, 5, stripped)

    # Return bytes
    buf = BytesIO()
    pdf.output(buf)
    return buf.getvalue()


@app.route("/health")
def health():
    return jsonify({"status": "ok", "timestamp": datetime.utcnow().isoformat()})


# Entry point
if __name__ == "__main__":
    port  = int(os.getenv("PORT", 5000))
    debug = os.getenv("FLASK_DEBUG", "False").lower() == "true"
    app.run(host="0.0.0.0", port=port, debug=debug)
