/* StartupBlueprint.AI  —  Frontend Logic
   IBM AICTE EduNet  |  watsonx.ai + IBM Granite */

'use strict';

// Section metadata
const SECTIONS = [
  { slug:'executive_summary',      label:'Executive Summary',          icon:'bi-briefcase-fill',         color:'ic-blue'   },
  { slug:'business_model_canvas',  label:'Business Model Canvas',      icon:'bi-grid-3x3-gap-fill',       color:'ic-purple' },
  { slug:'market_research',        label:'Market Research',            icon:'bi-bar-chart-fill',          color:'ic-green'  },
  { slug:'competitor_analysis',    label:'Competitor Analysis',        icon:'bi-diagram-3-fill',          color:'ic-orange' },
  { slug:'swot_analysis',          label:'SWOT Analysis',              icon:'bi-grid-fill',               color:'ic-red'    },
  { slug:'target_customers',       label:'Target Customers',           icon:'bi-person-lines-fill',       color:'ic-teal'   },
  { slug:'value_proposition',      label:'Value Proposition',          icon:'bi-gem',                     color:'ic-pink'   },
  { slug:'estimated_budget',       label:'Estimated Budget',           icon:'bi-calculator-fill',         color:'ic-indigo' },
  { slug:'revenue_model',          label:'Revenue Model',              icon:'bi-currency-exchange',       color:'ic-yellow' },
  { slug:'pricing_strategy',       label:'Pricing Strategy',           icon:'bi-tags-fill',               color:'ic-cyan'   },
  { slug:'marketing_strategy',     label:'Marketing Strategy',         icon:'bi-megaphone-fill',          color:'ic-blue'   },
  { slug:'go_to_market_strategy',  label:'Go-To-Market Strategy',      icon:'bi-send-fill',               color:'ic-purple' },
  { slug:'funding_opportunities',  label:'Funding Opportunities',      icon:'bi-bank2',                   color:'ic-green'  },
  { slug:'government_schemes',     label:'Government Startup Schemes', icon:'bi-flag-fill',               color:'ic-orange' },
  { slug:'legal_checklist',        label:'Legal Checklist',            icon:'bi-clipboard-check-fill',    color:'ic-red'    },
  { slug:'risk_analysis',          label:'Risk Analysis',              icon:'bi-exclamation-triangle-fill',color:'ic-teal'  },
  { slug:'growth_roadmap',         label:'Growth Roadmap',             icon:'bi-map-fill',                color:'ic-pink'   },
  { slug:'investor_pitch',         label:'Investor Pitch',             icon:'bi-mic-fill',                color:'ic-indigo' },
  { slug:'action_plan_30_60_90',   label:'30-60-90 Day Action Plan',   icon:'bi-calendar-check-fill',     color:'ic-yellow' },
];

// Loading states
const LOADING_MESSAGES = [
  'Connecting to IBM watsonx.ai…',
  'Generating sections 1–10 (Executive Summary → Pricing Strategy)…',
  'Drafting Executive Summary, Market Research, Competitor Analysis…',
  'Building SWOT, Value Proposition, Revenue Model…',
  'Now generating sections 11–19…',
  'Writing Marketing Strategy, Go-To-Market, Funding Opportunities…',
  'Compiling Government Schemes, Legal Checklist, Risk Analysis…',
  'Drafting Growth Roadmap, Investor Pitch, 30-60-90 Day Plan…',
  'Merging and structuring all 19 sections…',
  'Finalising your complete blueprint…',
];

// DOM helpers
const $  = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

// Theme
function initTheme() {
  const saved  = localStorage.getItem('theme') || 'light';
  applyTheme(saved);

  const btn  = $('#themeToggle');
  if (btn) btn.addEventListener('click', () => {
    const current = document.documentElement.getAttribute('data-bs-theme') || 'light';
    const next    = current === 'light' ? 'dark' : 'light';
    applyTheme(next);
    localStorage.setItem('theme', next);
  });
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-bs-theme', theme);
  $$('#themeIcon').forEach(el => {
    el.className = theme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill';
  });
}

// Character counters
function initCharCounters() {
  [['#idea', '#ideaCount', 500], ['#goal', '#goalCount', 300]].forEach(([inp, cnt, max]) => {
    const input   = $(inp);
    const counter = $(cnt);
    if (!input || !counter) return;
    const update = () => {
      counter.textContent = `${input.value.length} / ${max}`;
      counter.style.color = input.value.length > max * 0.9 ? 'var(--danger)' : 'var(--text-muted)';
    };
    input.addEventListener('input', update);
  });
}

// Loading overlay
let _loadingTimer = null;
let _progressVal  = 5;

function showLoading() {
  const overlay = $('#loadingOverlay');
  if (!overlay) return;
  overlay.classList.add('active');
  _progressVal = 5;
  setProgress(_progressVal);
  setLoadingMessage(0);
  setActiveStep(1);

  let msgIdx = 0;
  _loadingTimer = setInterval(() => {
    msgIdx = (msgIdx + 1) % LOADING_MESSAGES.length;
    setLoadingMessage(msgIdx);

    // Two-call flow: progress moves slowly through two phases
    // Phase 1 (0-48%): sections 1-10 generating
    // Phase 2 (48-90%): sections 11-19 generating
    const increment = _progressVal < 48 ? (Math.random() * 6 + 2) : (Math.random() * 5 + 1.5);
    _progressVal = Math.min(_progressVal + increment, 90);
    setProgress(_progressVal);

    // Advance steps aligned with two-call progress
    if (_progressVal > 15)  setActiveStep(2);
    if (_progressVal > 55)  setActiveStep(3);
    if (_progressVal > 78)  setActiveStep(4);
  }, 3000);
}

function hideLoading(success = true) {
  clearInterval(_loadingTimer);
  if (success) {
    setProgress(100);
    setLoadingMessage(LOADING_MESSAGES.length - 1);
  }
  setTimeout(() => {
    const overlay = $('#loadingOverlay');
    if (overlay) overlay.classList.remove('active');
    _progressVal = 5;
  }, 500);
}

function setProgress(val) {
  const bar = $('#loadingBar');
  if (bar) bar.style.width = val + '%';
}

function setLoadingMessage(idx) {
  const el = $('#loadingStatus');
  if (el) el.textContent = LOADING_MESSAGES[idx] || '';
}

function setActiveStep(num) {
  $$('.step-item').forEach(el => {
    const step = parseInt(el.dataset.step);
    if (step < num)  { el.classList.remove('active'); el.classList.add('done'); }
    if (step === num){ el.classList.add('active'); el.classList.remove('done'); }
    if (step > num)  { el.classList.remove('active', 'done'); }
  });
}

// Render section cards
function renderSectionCards(blueprint) {
  const grid = $('#sectionsGrid');
  if (!grid) return;
  grid.innerHTML = '';

  SECTIONS.forEach((sec, idx) => {
    const content = (blueprint.sections && blueprint.sections[sec.slug]) || '';
    const preview = stripMarkdown(content).slice(0, 220);

    const col = document.createElement('div');
    col.className = 'col-md-6 col-lg-4';
    col.style.animationDelay = `${idx * 60}ms`;

    col.innerHTML = `
      <div class="section-preview-card h-100">
        <div class="spc-header">
          <div class="spc-icon ${sec.color}"><i class="bi ${sec.icon}"></i></div>
          <div>
            <div class="spc-num">Section ${idx + 1}</div>
            <div class="spc-title">${sec.label}</div>
          </div>
        </div>
        <div class="spc-body">${escapeHtml(preview)}${content.length > 220 ? '…' : ''}</div>
      </div>
    `;
    grid.appendChild(col);
  });
}

// Utility
function stripMarkdown(text) {
  return text
    .replace(/#{1,6}\s+/g, '')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/`(.+?)`/g, '$1')
    .replace(/•\s+/g, '')
    .replace(/\n+/g, ' ')
    .trim();
}

function escapeHtml(str) {
  const map = {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'};
  return str.replace(/[&<>"']/g, m => map[m]);
}

function showToast(msg, type = 'success') {
  const toast    = $('#liveToast');
  const toastMsg = $('#toastMessage');
  if (!toast || !toastMsg) return;

  toastMsg.textContent = msg;
  toast.className = `toast align-items-center border-0 text-bg-${type}`;
  const bsToast = bootstrap.Toast.getOrCreateInstance(toast, { delay: 2500 });
  bsToast.show();
}

// Copy functions
let _blueprintCache = null;

function buildPlainText(blueprint) {
  if (!blueprint) return '';
  const meta = blueprint.metadata || {};
  let out = `STARTUP BLUEPRINT\n${'═'.repeat(60)}\n`;
  out += `Idea     : ${meta.idea     || ''}\n`;
  out += `Industry : ${meta.industry || ''}\n`;
  out += `Country  : ${meta.country  || ''}\n`;
  out += `Audience : ${meta.audience || ''}\n`;
  out += `Budget   : ${meta.budget   || ''}\n`;
  out += `Goal     : ${meta.goal     || ''}\n`;
  out += `Generated: ${meta.generated || ''}\n`;
  out += `${'─'.repeat(60)}\n\n`;

  SECTIONS.forEach((sec, i) => {
    const content = (blueprint.sections && blueprint.sections[sec.slug]) || '';
    out += `## ${i + 1}. ${sec.label}\n${'─'.repeat(40)}\n${content}\n\n`;
  });

  out += `\nGenerated by StartupBlueprint.AI powered by IBM watsonx.ai & IBM Granite\n`;
  return out;
}

function copyToClipboard(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text)
      .then(() => showToast('Report copied to clipboard!'))
      .catch(()  => fallbackCopy(text));
  } else {
    fallbackCopy(text);
  }
}

function fallbackCopy(text) {
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.cssText = 'position:fixed;opacity:0;pointer-events:none';
  document.body.appendChild(ta);
  ta.select();
  try {
    document.execCommand('copy');
    showToast('Report copied to clipboard!');
  } catch {
    showToast('Copy failed — please select text manually.', 'danger');
  }
  document.body.removeChild(ta);
}

// PDF Export — handled server-side via /report/<bid>/pdf
// All Download PDF buttons are now plain <a> links pointing to the
// Flask route that streams a real PDF. No client-side canvas needed.

// TOC active tracking (report page)
function initTocTracking() {
  const links    = $$('.toc-link');
  const sections = links.map(l => $(l.getAttribute('href')));
  if (!links.length) return;

  const observer = new IntersectionObserver(entries => {
    entries.forEach(e => {
      if (e.isIntersecting) {
        links.forEach(l => l.classList.remove('active'));
        const active = links.find(l => l.getAttribute('href') === '#' + e.target.id);
        if (active) active.classList.add('active');
      }
    });
  }, { rootMargin:'-80px 0px -60% 0px', threshold:0 });

  sections.forEach(s => s && observer.observe(s));
}

// Report page init
function initReport() {
  initTheme();
  initTocTracking();

  // PDF buttons are <a href> links — no JS binding needed

  // Bind all copy buttons
  ['#copyFullReport','#copyFullReport2'].forEach(sel => {
    const btn = $(sel);
    if (btn) btn.addEventListener('click', () => {
      const allText = $('#reportBody') ? $('#reportBody').innerText : '';
      copyToClipboard(allText);
    });
  });
}

// Main page init
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initCharCounters();

  const form        = $('#blueprintForm');
  const resultSec   = $('#resultSection');
  const viewReportBtn = $('#viewReportBtn');
  const copyBtn     = $('#copyReportBtn');

  if (!form) return; // not on index page

  // Form submission
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!form.checkValidity()) { form.classList.add('was-validated'); return; }

    const btn = $('#generateBtn');
    btn.disabled = true;
    btn.querySelector('.btn-text').textContent = 'Generating…';

    showLoading();

    try {
      const fd   = new FormData(form);
      const resp = await fetch('/generate', { method:'POST', body:fd });
      const data = await resp.json();

      hideLoading(true);

      if (data.success && data.blueprint) {
        _blueprintCache = data.blueprint;

        // Point "View Full Report" to the stable per-blueprint URL
        // so it works correctly when opened in a new tab
        const reportUrl = data.bid ? `/report/${data.bid}` : '/report';
        const viewBtn   = $('#viewReportBtn');
        if (viewBtn) viewBtn.href = reportUrl;

        // Update result meta
        const meta = data.blueprint.metadata || {};
        const titleEl = $('#resultTitle');
        const metaEl  = $('#resultMeta');
        if (titleEl) titleEl.textContent = meta.idea ? meta.idea.slice(0, 70) + (meta.idea.length > 70 ? '…' : '') : 'Your Blueprint';
        if (metaEl)  metaEl.textContent  = `${meta.industry} · ${meta.country} · Generated ${meta.generated}`;

        renderSectionCards(data.blueprint);

        // Show result section
        resultSec.classList.remove('d-none');
        resultSec.scrollIntoView({ behavior:'smooth', block:'start' });

      } else {
        showToast(data.error || 'Generation failed. Please try again.', 'danger');
      }

    } catch (err) {
      hideLoading(false);
      console.error('Generation error:', err);
      showToast('Network error. Please check your connection and try again.', 'danger');
    } finally {
      btn.disabled = false;
      btn.querySelector('.btn-text').textContent = 'Generate My Blueprint';
    }
  });

  // Copy button on result section
  if (copyBtn) {
    copyBtn.addEventListener('click', () => {
      if (!_blueprintCache) return;
      copyToClipboard(buildPlainText(_blueprintCache));
    });
  }

  // Toast container — create if not present (index page)
  if (!$('#liveToast')) {
    const container = document.createElement('div');
    container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
    container.style.zIndex = '9999';
    container.innerHTML = `
      <div id="liveToast" class="toast align-items-center text-bg-success border-0" role="alert">
        <div class="d-flex">
          <div class="toast-body" id="toastMessage">Done!</div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
      </div>`;
    document.body.appendChild(container);
  }
});
