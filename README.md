# 🚀 AI-Powered Startup Blueprint Generator
### Powered by IBM watsonx.ai & IBM Granite Models

> Transform any startup idea into a complete, investor-ready business blueprint in seconds using the power of IBM Granite foundation models on IBM watsonx.ai.

**🔴 Live Demo:** [https://startupplan-generator-edunet-foundation.onrender.com](https://startupplan-generator-edunet-foundation.onrender.com)

---

## 🏆 Core Technologies Used (The IBM Stack)
This project is proudly built as part of the **IBM BOB** (Build-O-Bot / Best of the Best) programme, fully utilizing the IBM Cloud ecosystem:
* **IBM watsonx.ai**: The core generative AI engine powering the entire intelligence layer.
* **IBM Granite Models** (`ibm/granite-3-8b-instruct`): High-performance enterprise-grade LLMs used for strategic business generation.
* **IBM Cloud Tools**: Used for secure, scalable enterprise deployment architecture.
* **IBM Cloud Storage**: Integrated for secure, persistent storage of startup blueprint documents and AI assets.
* **IBM AICTE EduNet Foundation**: Program framework and cloud provisioning.

---

## ✨ Features

| Feature | Details |
|---|---|
| **19-Section Blueprint** | Executive Summary, SWOT, BMC, Market Research, Competitor Analysis, Legal Checklist, Investor Pitch, 30-60-90 Day Plan & more |
| **IBM Granite AI** | Powered by `ibm/granite-3-8b-instruct` on IBM watsonx.ai |
| **Country-Specific** | Government schemes, legal requirements, and market data tailored to your country |
| **Dark Mode** | Full dark/light mode toggle with local storage persistence |
| **PDF Export** | Client-side PDF generation via html2pdf.js |
| **Copy Report** | One-click full report copy to clipboard |
| **Mobile-Friendly** | Fully responsive Bootstrap 5 UI |
| **Animated UI** | Floating cards, orbit loading animation, staggered card reveals |

---

## 🗂️ Project Structure

```
startup-blueprint-ai/
│
├── app.py                  # Flask app + watsonx.ai integration + AGENT_INSTRUCTIONS
├── requirements.txt        # Python dependencies
├── .env.example            # Environment variable template
├── .env                    # Your secrets (never commit this!)
│
├── templates/
│   ├── index.html          # Main landing + form page
│   └── report.html         # Full business report page
│
├── static/
│   ├── css/
│   │   └── style.css       # Custom styles (dark mode, animations, cards)
│   └── js/
│       └── main.js         # Frontend logic (form, loading, PDF, copy)
│
└── README.md
```

---

## ⚡ Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/startup-blueprint-ai.git
cd startup-blueprint-ai
```

### 2. Create a Virtual Environment

```bash
# Windows
python -m venv venv
venv\Scripts\activate

# macOS / Linux
python3 -m venv venv
source venv/bin/activate
```

### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

### 4. Configure Environment Variables

```bash
# Copy the example file
cp .env.example .env
```

Open `.env` and fill in your credentials:

```env
IBM_API_KEY=your_ibm_api_key_here
IBM_PROJECT_ID=your_project_id_here
IBM_REGION_URL=https://us-south.ml.cloud.ibm.com
IBM_MODEL_ID=ibm/granite-3-8b-instruct
FLASK_SECRET_KEY=your_random_secret_key
```

### 5. Run the App

```bash
python app.py
```

Open your browser at **http://localhost:5000**

---

## 🔑 Getting IBM watsonx.ai Credentials

### IBM API Key
1. Go to [IBM Cloud Console](https://cloud.ibm.com)
2. Navigate to **Manage → Access (IAM) → API Keys**
3. Click **Create an IBM Cloud API key**
4. Copy the key to your `.env` file

### Project ID
1. Go to [IBM watsonx.ai](https://dataplatform.cloud.ibm.com)
2. Open or create a **Project**
3. Go to **Manage → General**
4. Copy the **Project ID** to your `.env` file

### Region URL
Choose the region closest to you:

| Region    | URL |
|-----------|-----|
| Dallas    | `https://us-south.ml.cloud.ibm.com` |
| London    | `https://eu-gb.ml.cloud.ibm.com` |
| Frankfurt | `https://eu-de.ml.cloud.ibm.com` |
| Tokyo     | `https://jp-tok.ml.cloud.ibm.com` |
| Sydney    | `https://au-syd.ml.cloud.ibm.com` |

### Model ID
Recommended models:
- `ibm/granite-3-8b-instruct` ← Default, best performance
- `ibm/granite-13b-instruct-v2`
- `ibm/granite-3-2b-instruct` (faster, lower cost)

---

## 🤖 Customising the AI Agent

Open [`app.py`](app.py) and edit the `AGENT_INSTRUCTIONS` block at the top:

```python
AGENT_INSTRUCTIONS = """
You are StartupMentor AI...
# Change persona, tone, expertise, country-specific schemes here
"""
```

You can customise:
- **AI Personal** — name, role, experience level
- **Mentoring Style** — formal, casual, aggressive growth hacker, etc.
- **Country Focus** — add more country-specific government schemes
- **Response Tone** — inspiring, realistic, conservative, etc.
- **Section Depth** — add instructions for specific sections

---

## 🚀 Deployment

### Deploy to Render (Recommended — Free Tier)

1. **Push to GitHub**
   ```bash
   git init
   git add .
   git commit -m "Initial commit: Startup Blueprint Generator"
   git remote add origin https://github.com/YOUR_USERNAME/startup-blueprint-ai.git
   git push -u origin main
   ```

2. **Create Render Web Service**
   - Go to [render.com](https://render.com) → **New → Web Service**
   - Connect your GitHub repository
   - Set the following:
     | Field | Value |
     |---|---|
     | **Runtime** | Python 3 |
     | **Build Command** | `pip install -r requirements.txt` |
     | **Start Command** | `gunicorn app:app --bind 0.0.0.0:$PORT` |

3. **Add Environment Variables in Render Dashboard**
   - `IBM_API_KEY`
   - `IBM_PROJECT_ID`
   - `IBM_REGION_URL`
   - `IBM_MODEL_ID`
   - `FLASK_SECRET_KEY`

4. Click **Deploy** — your app will be live at `https://your-app.onrender.com`

---

## 🛡️ Security Notes

- **Never commit your `.env` file** — it's in `.gitignore` by default
- Rotate your IBM API key regularly
- Use a strong random value for `FLASK_SECRET_KEY`
- In production, enable HTTPS (Render provides this automatically)
- The app uses Flask sessions for blueprint storage — configure a persistent session backend for multi-instance deployments

---

## 📊 Blueprint Sections Generated

| # | Section | Description |
|---|---------|-------------|
| 1 | Executive Summary | High-level overview of the business |
| 2 | Business Model Canvas | 9-block BMC with all components |
| 3 | Market Research | TAM/SAM/SOM, trends, opportunities |
| 4 | Competitor Analysis | Key players, gaps, differentiation |
| 5 | SWOT Analysis | Strengths, Weaknesses, Opportunities, Threats |
| 6 | Target Customers | Personas, demographics, psychographics |
| 7 | Value Proposition | Unique value, problem/solution fit |
| 8 | Estimated Budget | Startup costs, burn rate, breakeven |
| 9 | Revenue Model | Revenue streams and projections |
| 10 | Pricing Strategy | Pricing model, tiers, benchmarks |
| 11 | Marketing Strategy | Channels, content, SEO, social |
| 12 | Go-To-Market Strategy | Launch plan, partnerships, channels |
| 13 | Funding Opportunities | VC, angels, crowdfunding, grants |
| 14 | Government Schemes | Country-specific programs and incentives |
| 15 | Legal Checklist | Registration, IP, compliance, contracts |
| 16 | Risk Analysis | Key risks and mitigation strategies |
| 17 | Growth Roadmap | 1-3 year milestones and KPIs |
| 18 | Investor Pitch | One-paragraph pitch deck narrative |
| 19 | 30-60-90 Day Plan | Hyper-specific daily action items |

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| **AI Backend** | IBM watsonx.ai + IBM Granite Models |
| **Cloud Infrastructure**| IBM Cloud Tools & IBM Cloud Storage |
| **Program Framework** | IBM BOB (IBM AICTE EduNet) |
| **Web Framework** | Python Flask 3.0 |
| **Frontend** | Bootstrap 5.3, Bootstrap Icons, Inter Font |
| **PDF Export** | html2pdf.js (client-side) |
| **Environment** | python-dotenv |
| **Production Server** | Gunicorn |

---

## 📄 License

MIT License — free to use, modify, and distribute.

---

## 🙏 Acknowledgements

Built as part of the **IBM SKILLSBUILD AICTE INTERNSHIP COLLABORATED WITH EduNet Foundation** using IBM watsonx.ai and IBM Granite foundation models.

---

*Made with ❤️ using IBM watsonx.ai*
