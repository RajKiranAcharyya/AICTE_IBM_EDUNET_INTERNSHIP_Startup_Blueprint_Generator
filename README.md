# 🚀 AI-Powered Startup Blueprint Generator (`StartupBlueprint.AI`)
### Powered by Java 21, Spring Boot & AI API Intelligence

> Transform any startup idea into a complete, investor-ready 19-section business blueprint in seconds — featuring AI-driven market & financial analysis, XSS-safe Markdown rendering, server-side A4 PDF export, 20-hour auto-expiring report links, and a Dockerized Spring Boot architecture.

**🔴 Live Website:** [https://startupplan-generator-edunet-foundation.onrender.com](https://startupplan-generator-edunet-foundation.onrender.com)

---

## 🎥 Video Demonstration
https://docs.google.com/videos/d/1r0zR4eqxMDFpNzph_9MeQwZTrGSSQXVg3asL4nH2F_A/play

---

## 📸 Project Screenshots
<img width="1918" height="865" alt="Screenshot 2026-07-13 020424" src="https://github.com/user-attachments/assets/c8611336-f384-4e81-adf9-6a86543d90a0" />
<img width="1917" height="862" alt="Screenshot 2026-07-13 020444" src="https://github.com/user-attachments/assets/b74cf8a7-db2f-4957-932f-5456c04dac5e" />
<img width="1913" height="880" alt="Screenshot 2026-07-13 020520" src="https://github.com/user-attachments/assets/aa5d7bee-8174-45b1-9c88-4d5cfd1e8daa" />
<img width="1918" height="873" alt="Screenshot 2026-07-13 020534" src="https://github.com/user-attachments/assets/12041ffb-6e3f-4e8f-ad4d-f1785b5c5f50" />
<img width="1912" height="772" alt="Screenshot 2026-07-13 020547" src="https://github.com/user-attachments/assets/49a56595-88d5-43c7-9366-1329b49f6b46" />

---

## ✨ Key Features

| Feature | Details |
|---|---|
| **19-Section Blueprint** | Executive Summary, Business Model Canvas, Market Research, Competitor Analysis, SWOT, Target Customers, Value Proposition, Budget, Revenue Model, Pricing, Marketing, GTM, Funding, Government Schemes, Legal Checklist, Risk Analysis, Growth Roadmap, Investor Pitch, and 30-60-90 Day Plan |
| **3-Batch AI Generation** | Splits generation across balanced batches (`1–7`, `8–13`, `14–19`) with automatic rate-limit (`HTTP 429`) retry handling so complex prompts complete without truncation |
| **Server-Side PDF Export** | Clean, multi-page styled A4 PDF downloads generated via **OpenPDF 2.0.3** (`/report/{bid}/pdf`) |
| **20-Hour Auto-Deletion (TTL)** | Bounded **Caffeine** cache (`expireAfterWrite(20, HOURS)`, max `200` blueprints) protects server memory and automatically expires temporary report links |
| **XSS-Safe Rendering** | Parses Markdown & GFM tables using **CommonMark** and sanitizes output via **OWASP Java HTML Sanitizer** |
| **IST Timestamping** | Generates accurate `Asia/Kolkata` (`IST`) timestamps even inside UTC Docker containers |
| **Country-Specific Insights** | Tailors government startup schemes, regulatory checklists, and market context to the selected country |
| **Live Visitor Counter & UI Polish** | In-memory visitor counter on the landing page, Dark/Light mode toggle, animated loading overlay, one-click clipboard copy, and "Connect with Raj" modal |

---

## 🗂️ Project Structure

```
AICTE_IBM_EDUNET_INTERNSHIP_Startup_Blueprint_Generator/
│
├── Dockerfile                                  # Multi-stage Maven + Eclipse Temurin JDK/JRE 21 Alpine build
├── .dockerignore                               # Excludes target/, .git/, docs/decks from Docker context
├── pom.xml                                     # Spring Boot 3.4.3, Java 21, Caffeine, CommonMark, OWASP, OpenPDF
│
├── src/main/java/com/raj/blueprint/
│   ├── BlueprintApplication.java               # Spring Boot entry point
│   ├── controller/
│   │   └── BlueprintController.java            # Routes: /, /generate, /report, /report/{bid}, /report/{bid}/pdf, /health
│   ├── model/
│   │   └── Blueprint.java                      # Blueprint DTO (raw text, parsed sections, metadata)
│   └── service/
│       ├── GroqService.java                    # AI API client (3-batch orchestration, 429 retry, IST formatting)
│       ├── BlueprintParserService.java         # 3-tier heading matcher, CommonMark + OWASP HTML sanitizer
│       └── PdfExportService.java               # Server-side A4 PDF document builder (OpenPDF)
│
├── src/main/resources/
│   ├── application.properties                  # Server port (${PORT:8080}) & API configuration
│   ├── templates/
│   │   ├── index.html                          # Landing + startup brief input form (Thymeleaf + Bootstrap 5)
│   │   └── report.html                         # Interactive 19-section report viewer with 20h TTL notice
│   └── static/
│       ├── css/style.css                       # Light/dark themes, animations, cards, responsive styles
│       ├── js/main.js                          # Async form submission, loading steps, copy & theme toggle
│       └── images/raj_profile.jpg              # Profile image for Connect modal
│
└── README.md
```

---

## ⚡ Quick Start (Local Development)

### Prerequisites
- **Java 21 (JDK 21)**
- **Apache Maven 3.9+**
- **Groq API Key** (free tier available at [console.groq.com](https://console.groq.com))

### 1. Clone the Repository

```bash
git clone https://github.com/RajKiranAcharyya/AICTE_IBM_EDUNET_INTERNSHIP_Startup_Blueprint_Generator.git
cd AICTE_IBM_EDUNET_INTERNSHIP_Startup_Blueprint_Generator
```

### 2. Set Environment Variables

**Windows Command Prompt (`cmd`):**
```cmd
set GROQ_API_KEY=your_groq_api_key_here
set GROQ_MODEL=openai/gpt-oss-120b
```

**Windows PowerShell:**
```powershell
$env:GROQ_API_KEY="your_groq_api_key_here"
$env:GROQ_MODEL="openai/gpt-oss-120b"
```

**macOS / Linux:**
```bash
export GROQ_API_KEY="your_groq_api_key_here"
export GROQ_MODEL="openai/gpt-oss-120b"
```

### 3. Build & Run

```bash
mvn spring-boot:run
```

Or build an executable JAR and run it:
```bash
mvn clean package -DskipTests
java -jar target/startup-blueprint-generator-1.0.0.jar
```

Open your browser at **[http://localhost:8080](http://localhost:8080)**

---

## 🚀 Deployment (Docker on Render)

The repository includes a multi-stage **`Dockerfile`** tuned for Render's 512 MB Free Tier (`-XX:MaxRAMPercentage=75.0` and dynamic `${PORT:-8080}`).

1. Push the repository to GitHub.
2. On [render.com](https://render.com), create or configure a **Web Service**:
   - **Language / Runtime:** `Docker`
   - **Dockerfile Path:** `./Dockerfile`
   - **Health Check Path (optional):** `/health`
3. Add **Environment Variables** in Render Dashboard:
   - `GROQ_API_KEY` = `your_groq_api_key`
   - `GROQ_MODEL` = `openai/gpt-oss-120b`
4. Deploy — your Spring Boot app binds automatically to `$PORT`.

---

## 📊 Blueprint Sections Generated

| # | Section | Description |
|---|---------|-------------|
| 1 | Executive Summary | High-level venture overview & strategy |
| 2 | Business Model Canvas | Structured 9-block business canvas |
| 3 | Market Research | TAM/SAM/SOM, industry trends, growth drivers |
| 4 | Competitor Analysis | Key players, market gaps, differentiation |
| 5 | SWOT Analysis | Strengths, Weaknesses, Opportunities, Threats |
| 6 | Target Customers | Customer personas, segments, demographics |
| 7 | Value Proposition | Core problem/solution fit & competitive edge |
| 8 | Estimated Budget | Startup cost breakdown, burn & runway |
| 9 | Revenue Model | Monetization channels & financial projections |
| 10 | Pricing Strategy | Pricing tiers, unit economics & benchmarks |
| 11 | Marketing Strategy | Channels, positioning, growth tactics |
| 12 | Go-To-Market Strategy | Phased launch & acquisition roadmap |
| 13 | Funding Opportunities | Grants, angels, accelerators, VC targets |
| 14 | Government Startup Schemes | Country-specific incentives & public programmes |
| 15 | Legal Checklist | Incorporation, IP, licensing & compliance |
| 16 | Risk Analysis | Technical, regulatory, financial & market risks |
| 17 | Growth Roadmap | 1–5 year expansion milestones & KPIs |
| 18 | Investor Pitch | Concise investor-ready pitch summary |
| 19 | 30-60-90 Day Action Plan | Concrete first-quarter execution deliverables |

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Java 21, Spring Boot 3.4.3 (Web, Thymeleaf, RestClient) |
| **AI Inference Engine** | AI API Intelligence (`openai/gpt-oss-120b` via Groq API) |
| **Caching & TTL** | Caffeine Cache (20-hour auto-expiration, bounded to 200 reports) |
| **Markdown & Security** | CommonMark 0.24.0 (+ GFM tables) & OWASP Java HTML Sanitizer |
| **PDF Export** | OpenPDF 2.0.3 (server-side A4 PDF generation) |
| **Frontend** | Thymeleaf, Bootstrap 5.3, Bootstrap Icons, Inter Font, Vanilla JS |
| **Containerization & Hosting** | Multi-stage Docker (`eclipse-temurin:21-jre-alpine`) on Render |

---

## 🛡️ Security & Privacy Notes

- **No hardcoded secrets:** API keys are injected via environment variables (`GROQ_API_KEY`).
- **Temporary Storage:** Generated blueprints live strictly in bounded server memory for **20 hours** and are automatically purged afterwards.
- **Sanitized Output:** Rendered Markdown passes through OWASP HTML Sanitizer before display.

---

## 📄 License

MIT License — free to use, modify, and distribute.

---

## 🙏 Acknowledgements

Originally initiated during the **AICTE – IBM SkillsBuild – EduNet Foundation Internship**, and upgraded into a production-ready **Java 21 Spring Boot & Docker** web platform.

**Built by [Raj Kiran Acharyya](https://github.com/RajKiranAcharyya)**
