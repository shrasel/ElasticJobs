/**
 * ElasticJobs - Pure Elasticsearch 8.x Frontend
 */

// Smart Base API URL: points to http://localhost:8090 if served from a standalone frontend dev server
const API_BASE = (window.location.port === '8090') ? window.location.origin : 'http://localhost:8090';

// Application State
const state = {
    query: '',
    searchMode: 'HYBRID',
    fuzzyEnabled: true,
    sortBy: 'RELEVANCE',
    page: 0,
    size: 20,
    filters: {
        workplaceTypes: [],
        experienceLevels: [],
        minSalary: null,
        visaSponsored: null,
        geoRadius: {
            lat: 33.0198, // Plano, TX default
            lon: -96.6989,
            radiusMiles: 25.0
        },
        geoEnabled: false
    },
    selectedResumeFile: null,
    autocompleteDebounceTimer: null
};

// DOM Elements
const searchInput = document.getElementById('search-input');
const clearInputBtn = document.getElementById('clear-input-btn');
const searchSubmitBtn = document.getElementById('search-submit-btn');
const autocompleteDropdown = document.getElementById('autocomplete-dropdown');
const autocompleteList = document.getElementById('autocomplete-list');
const autocompleteSpeed = document.getElementById('autocomplete-speed');
const jobsContainer = document.getElementById('jobs-container');
const emptyState = document.getElementById('empty-state');
const resultsCount = document.getElementById('results-count');
const searchLatency = document.getElementById('search-latency');
const sortSelect = document.getElementById('sort-select');
const modeTabs = document.querySelectorAll('.mode-tab');
const activeChipsBar = document.getElementById('active-chips-bar');
const themeToggleBtn = document.getElementById('theme-toggle-btn');
const themeIcon = document.getElementById('theme-icon');

// Mobile Drawer Elements
const mobileFilterBtn = document.getElementById('mobile-filter-btn');
const filterSidebar = document.getElementById('filter-sidebar');
const closeFilterBtn = document.getElementById('close-filter-btn');
const drawerOverlay = document.getElementById('drawer-overlay');
const activeFilterCountBadge = document.getElementById('active-filter-count');

// Filter Inputs
const geoEnabledCheck = document.getElementById('geo-enabled-check');
const geoRadiusSlider = document.getElementById('geo-radius-slider');
const geoRadiusLabel = document.getElementById('geo-radius-label');
const nearMeBtn = document.getElementById('near-me-btn');
const geoStatus = document.getElementById('geo-status');
const salarySlider = document.getElementById('salary-slider');
const salaryValueLabel = document.getElementById('salary-value-label');
const visaCheck = document.getElementById('visa-check');
const fuzzyCheck = document.getElementById('fuzzy-check');
const resetFiltersBtn = document.getElementById('reset-filters-btn');

// Modals
const jobModal = document.getElementById('job-modal');
const modalContent = document.getElementById('modal-content');
const closeModalBtn = document.getElementById('close-modal-btn');
const resumeBtn = document.getElementById('resume-btn');
const resumeModal = document.getElementById('resume-modal');
const closeResumeModalBtn = document.getElementById('close-resume-modal-btn');
const dropzone = document.getElementById('dropzone');
const resumeFileInput = document.getElementById('resume-file-input');
const selectedFileInfo = document.getElementById('selected-file-info');
const selectedFilename = document.getElementById('selected-filename');
const removeFileBtn = document.getElementById('remove-file-btn');
const resumeTextInput = document.getElementById('resume-text-input');
const resumeVisaCheck = document.getElementById('resume-visa-check');
const submitResumeBtn = document.getElementById('submit-resume-btn');
const resumeResults = document.getElementById('resume-results');
const resumeJobsList = document.getElementById('resume-jobs-list');

// ==========================================================================
// Initialization & Theme Management (Light Theme Default)
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    setupEventListeners();
    executeSearch(); // Initial load
});

function initTheme() {
    try {
        localStorage.removeItem('elasticjobs_theme');
        localStorage.removeItem('theme');
    } catch (e) {}
    document.body.classList.remove('dark-theme');
}

function setupEventListeners() {
    // Mode Switcher Tabs
    modeTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            modeTabs.forEach(t => {
                t.classList.remove('active');
                t.setAttribute('aria-selected', 'false');
            });
            tab.classList.add('active');
            tab.setAttribute('aria-selected', 'true');
            state.searchMode = tab.dataset.mode;
            executeSearch();
        });
    });

    // Search Input & Clear
    searchInput.addEventListener('input', (e) => {
        const val = e.target.value;
        state.query = val;
        clearInputBtn.style.display = val.length > 0 ? 'block' : 'none';
        handleAutocomplete(val);
    });

    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            hideAutocomplete();
            executeSearch();
        } else if (e.key === 'Escape') {
            hideAutocomplete();
        }
    });

    clearInputBtn.addEventListener('click', () => {
        searchInput.value = '';
        state.query = '';
        clearInputBtn.style.display = 'none';
        hideAutocomplete();
        executeSearch();
    });

    searchSubmitBtn.addEventListener('click', () => {
        hideAutocomplete();
        executeSearch();
    });

    // Close autocomplete on click outside
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.search-box-wrapper')) {
            hideAutocomplete();
        }
    });

    // Quick Suggestion Chips
    document.querySelectorAll('.quick-chips .chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const query = chip.dataset.query;
            const mode = chip.dataset.mode;
            searchInput.value = query;
            state.query = query;
            clearInputBtn.style.display = 'block';

            if (mode) {
                state.searchMode = mode;
                modeTabs.forEach(t => {
                    const match = t.dataset.mode === mode;
                    t.classList.toggle('active', match);
                    t.setAttribute('aria-selected', match ? 'true' : 'false');
                });
            }
            executeSearch();
        });
    });

    // Sort Dropdown
    sortSelect.addEventListener('change', (e) => {
        state.sortBy = e.target.value;
        executeSearch();
    });

    // Mobile Drawer Toggle
    mobileFilterBtn.addEventListener('click', () => openFilterDrawer());
    closeFilterBtn.addEventListener('click', () => closeFilterDrawer());
    drawerOverlay.addEventListener('click', () => closeFilterDrawer());

    // Geo Radius Controls
    geoEnabledCheck.addEventListener('change', (e) => {
        state.filters.geoEnabled = e.target.checked;
        if (state.filters.geoEnabled) {
            sortSelect.value = 'GEO_DISTANCE';
            state.sortBy = 'GEO_DISTANCE';
        }
        executeSearch();
    });

    geoRadiusSlider.addEventListener('input', (e) => {
        const miles = parseFloat(e.target.value);
        geoRadiusLabel.textContent = `${miles} miles`;
        state.filters.geoRadius.radiusMiles = miles;
        if (state.filters.geoEnabled) {
            executeSearch();
        }
    });

    nearMeBtn.addEventListener('click', () => {
        if (navigator.geolocation) {
            geoStatus.textContent = 'Locating GPS...';
            navigator.geolocation.getCurrentPosition(
                (pos) => {
                    state.filters.geoRadius.lat = pos.coords.latitude;
                    state.filters.geoRadius.lon = pos.coords.longitude;
                    geoStatus.textContent = `GPS: (${pos.coords.latitude.toFixed(2)}, ${pos.coords.longitude.toFixed(2)})`;
                    geoEnabledCheck.checked = true;
                    state.filters.geoEnabled = true;
                    nearMeBtn.textContent = '📍 Current GPS';
                    executeSearch();
                },
                (err) => {
                    geoStatus.textContent = 'Plano, TX (33.02, -96.70)';
                    nearMeBtn.textContent = '📍 Plano, TX (Default)';
                }
            );
        }
    });

    // Salary Slider
    salarySlider.addEventListener('input', (e) => {
        const val = parseInt(e.target.value, 10);
        if (val <= 50000) {
            salaryValueLabel.textContent = 'Any salary';
            state.filters.minSalary = null;
        } else {
            salaryValueLabel.textContent = `$${(val / 1000).toFixed(0)}k+ / yr`;
            state.filters.minSalary = val;
        }
        executeSearch();
    });

    // Checkbox Facets (Workplace & Experience)
    document.querySelectorAll('input[name="workplace"]').forEach(cb => {
        cb.addEventListener('change', () => {
            state.filters.workplaceTypes = Array.from(document.querySelectorAll('input[name="workplace"]:checked'))
                .map(i => i.value);
            executeSearch();
        });
    });

    document.querySelectorAll('input[name="experience"]').forEach(cb => {
        cb.addEventListener('change', () => {
            state.filters.experienceLevels = Array.from(document.querySelectorAll('input[name="experience"]:checked'))
                .map(i => i.value);
            executeSearch();
        });
    });

    visaCheck.addEventListener('change', (e) => {
        state.filters.visaSponsored = e.target.checked ? true : null;
        executeSearch();
    });

    fuzzyCheck.addEventListener('change', (e) => {
        state.fuzzyEnabled = e.target.checked;
        executeSearch();
    });

    resetFiltersBtn.addEventListener('click', resetAllFilters);
    document.getElementById('empty-reset-btn').addEventListener('click', resetAllFilters);

    // Job Modal Close
    closeModalBtn.addEventListener('click', () => jobModal.style.display = 'none');
    jobModal.addEventListener('click', (e) => {
        if (e.target === jobModal) jobModal.style.display = 'none';
    });

    // Resume Modal Open/Close
    resumeBtn.addEventListener('click', () => {
        resumeModal.style.display = 'flex';
    });
    closeResumeModalBtn.addEventListener('click', () => {
        resumeModal.style.display = 'none';
    });
    resumeModal.addEventListener('click', (e) => {
        if (e.target === resumeModal) resumeModal.style.display = 'none';
    });

    // Resume Upload Drag & Drop
    dropzone.addEventListener('click', () => resumeFileInput.click());
    dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropzone.classList.add('dragover');
    });
    dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
    dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.classList.remove('dragover');
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            handleResumeFileSelected(e.dataTransfer.files[0]);
        }
    });
    resumeFileInput.addEventListener('change', (e) => {
        if (e.target.files && e.target.files[0]) {
            handleResumeFileSelected(e.target.files[0]);
        }
    });

    removeFileBtn.addEventListener('click', () => {
        state.selectedResumeFile = null;
        resumeFileInput.value = '';
        selectedFileInfo.style.display = 'none';
        dropzone.style.display = 'block';
    });

    submitResumeBtn.addEventListener('click', submitResumeMatching);
}

// ==========================================================================
// Autocomplete (Typeahead <15ms)
// ==========================================================================
function handleAutocomplete(query) {
    clearTimeout(state.autocompleteDebounceTimer);
    if (!query || query.trim().length < 1) {
        hideAutocomplete();
        return;
    }

    state.autocompleteDebounceTimer = setTimeout(async () => {
        try {
            const res = await fetch(`${API_BASE}/api/v1/jobs/autocomplete?q=${encodeURIComponent(query)}&size=8`);
            if (!res.ok) return;
            const data = await res.json();
            renderAutocomplete(data.suggestions, data.took_ms);
        } catch (err) {
            console.error('Autocomplete error:', err);
        }
    }, 120);
}

function renderAutocomplete(suggestions, tookMs) {
    if (!suggestions || suggestions.length === 0) {
        hideAutocomplete();
        return;
    }

    autocompleteSpeed.textContent = `${tookMs}ms`;
    autocompleteList.innerHTML = '';

    suggestions.forEach(item => {
        const div = document.createElement('div');
        div.className = 'autocomplete-item';
        div.innerHTML = `
            <span class="autocomplete-item-text">${highlightMatch(item.text, state.query)}</span>
            <span class="autocomplete-item-type ${item.category}">${item.category}</span>
        `;
        div.addEventListener('click', () => {
            searchInput.value = item.text;
            state.query = item.text;
            clearInputBtn.style.display = 'block';
            hideAutocomplete();
            executeSearch();
        });
        autocompleteList.appendChild(div);
    });

    autocompleteDropdown.style.display = 'block';
}

function hideAutocomplete() {
    autocompleteDropdown.style.display = 'none';
}

function highlightMatch(text, query) {
    if (!query) return text;
    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    return text.replace(regex, '<strong style="color: #818cf8;">$1</strong>');
}

// ==========================================================================
// Search Execution
// ==========================================================================
async function executeSearch() {
    renderLoadingSkeleton();
    updateActiveFilterBadge();

    const requestPayload = {
        query: state.query,
        search_mode: state.searchMode,
        fuzzy_enabled: state.fuzzyEnabled,
        sort_by: state.sortBy,
        page: state.page,
        size: state.size,
        filters: {}
    };

    if (state.filters.workplaceTypes.length > 0) {
        requestPayload.filters.workplace_types = state.filters.workplaceTypes;
    }
    if (state.filters.experienceLevels.length > 0) {
        requestPayload.filters.experience_levels = state.filters.experienceLevels;
    }
    if (state.filters.minSalary) {
        requestPayload.filters.min_salary = state.filters.minSalary;
    }
    if (state.filters.visaSponsored !== null) {
        requestPayload.filters.visa_sponsored = state.filters.visaSponsored;
    }
    if (state.filters.geoEnabled && state.filters.geoRadius) {
        requestPayload.filters.geo_radius = state.filters.geoRadius;
    }

    try {
        const res = await fetch(`${API_BASE}/api/v1/jobs/search`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestPayload)
        });

        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
        const data = await res.json();

        renderResults(data);
        updateFacets(data.facets);
        renderActiveChips();
    } catch (err) {
        console.error('Search error:', err);
        jobsContainer.innerHTML = `<div class="empty-state"><p style="color:#ef4444;">Search request failed. Ensure backend is running.</p></div>`;
    }
}

function renderResults(data) {
    const hits = data.total_hits || 0;
    resultsCount.textContent = `${hits} job${hits === 1 ? '' : 's'} found`;
    searchLatency.textContent = `${data.took_ms}ms · ${data.search_mode_used}`;

    if (!data.jobs || data.jobs.length === 0) {
        jobsContainer.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';
    jobsContainer.innerHTML = '';

    data.jobs.forEach(job => {
        const card = document.createElement('article');
        card.className = 'job-card';

        const initial = job.company && job.company.name ? job.company.name.charAt(0) : 'J';
        const salaryText = formatSalary(job.salary);
        const distancePill = (job.distance_miles !== null && job.distance_miles !== undefined)
            ? `<span class="pill pill-distance">📍 ${job.distance_miles} mi away</span>` : '';
        const visaPill = job.visa_sponsored ? `<span class="pill pill-visa">★ Visa Sponsored</span>` : '';
        const scorePill = job.score ? `<span class="score-badge">${formatScore(job.score, data.search_mode_used)}</span>` : '';

        const skillsHtml = (job.skills || []).slice(0, 5).map(s =>
            `<span class="skill-tag">${s.name}</span>`
        ).join('');

        card.innerHTML = `
            <div class="job-card-header">
                <div class="company-avatar">${initial}</div>
                <div class="job-meta-header">
                    <h2 class="job-title">${job.title}</h2>
                    <div class="company-name">
                        <span>${job.company?.name || 'Confidential'}</span>
                        <span>•</span>
                        <span>${job.location?.city || ''}${job.location?.state ? ', ' + job.location.state : ''}</span>
                    </div>
                </div>
                ${scorePill}
            </div>

            <div class="job-details-pills">
                <span class="pill pill-salary">💰 ${salaryText}</span>
                <span class="pill">${job.workplace_type || 'Full-Time'}</span>
                <span class="pill">${job.experience_level || 'Mid'}</span>
                ${distancePill}
                ${visaPill}
            </div>

            <p class="job-description-excerpt">${job.description || ''}</p>

            <div class="job-skills-tags">
                ${skillsHtml}
            </div>
        `;

        card.addEventListener('click', () => openJobModal(job));
        jobsContainer.appendChild(card);
    });
}

function updateFacets(facets) {
    if (!facets) return;

    if (facets.workplace_types) {
        document.getElementById('count-remote').textContent = facets.workplace_types['REMOTE'] || 0;
        document.getElementById('count-hybrid').textContent = facets.workplace_types['HYBRID'] || 0;
        document.getElementById('count-onsite').textContent = facets.workplace_types['ON_SITE'] || 0;
    }
    if (facets.experience_levels) {
        document.getElementById('count-entry').textContent = facets.experience_levels['ENTRY'] || 0;
        document.getElementById('count-mid').textContent = facets.experience_levels['MID'] || 0;
        document.getElementById('count-senior').textContent = facets.experience_levels['SENIOR'] || 0;
        document.getElementById('count-lead').textContent = facets.experience_levels['LEAD'] || 0;
    }
}

// ==========================================================================
// Job Details & Similar Jobs Modal
// ==========================================================================
async function openJobModal(job) {
    const salaryText = formatSalary(job.salary);
    const initial = job.company && job.company.name ? job.company.name.charAt(0) : 'J';

    modalContent.innerHTML = `
        <div class="job-card-header" style="margin-bottom: 16px;">
            <div class="company-avatar" style="width: 52px; height: 52px; font-size: 1.4rem;">${initial}</div>
            <div class="job-meta-header">
                <h2 class="job-title" style="font-size: 1.35rem;">${job.title}</h2>
                <div class="company-name" style="font-size: 0.95rem;">
                    <strong>${job.company?.name || 'Company'}</strong>
                    <span>•</span>
                    <span>${job.location?.city || ''}${job.location?.state ? ', ' + job.location.state : ''}</span>
                </div>
            </div>
        </div>

        <div class="job-details-pills" style="margin-bottom: 20px;">
            <span class="pill pill-salary">💰 ${salaryText}</span>
            <span class="pill">🏢 ${job.workplace_type}</span>
            <span class="pill">💼 ${job.employment_type}</span>
            <span class="pill">📈 ${job.experience_level}</span>
            ${job.visa_sponsored ? '<span class="pill pill-visa">★ Visa Sponsored</span>' : ''}
        </div>

        <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 8px;">Role Overview</h4>
        <p style="color: var(--text-secondary); font-size: 0.92rem; line-height: 1.6; margin-bottom: 20px;">
            ${job.description}
        </p>

        <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 10px;">Required Skills</h4>
        <div class="job-skills-tags" style="margin-bottom: 24px;">
            ${(job.skills || []).map(s => `<span class="skill-tag" style="font-size: 0.8rem; padding: 4px 10px;">${s.name} (${s.category})</span>`).join('')}
        </div>

        <!-- Similar Jobs Section (Vector Cosine Matching) -->
        <div class="similar-jobs-section">
            <h4>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                </svg>
                Similar Roles (Elasticsearch Vector Match)
            </h4>
            <div id="modal-similar-list" class="similar-jobs-list">
                <span style="font-size:0.85rem; color:var(--text-muted);">Finding semantically similar roles...</span>
            </div>
        </div>
    `;

    jobModal.style.display = 'flex';

    // Fetch Similar Jobs via Elasticsearch Vector Search
    try {
        const res = await fetch(`${API_BASE}/api/v1/jobs/${job.id}/similar?limit=3`);
        if (res.ok) {
            const simData = await res.json();
            renderSimilarJobs(simData.similar_jobs);
        }
    } catch (err) {
        console.error('Failed to load similar jobs:', err);
    }
}

function renderSimilarJobs(similarJobs) {
    const container = document.getElementById('modal-similar-list');
    if (!container) return;

    if (!similarJobs || similarJobs.length === 0) {
        container.innerHTML = '<span style="font-size:0.85rem; color:var(--text-muted);">No similar roles found.</span>';
        return;
    }

    container.innerHTML = '';
    similarJobs.forEach(item => {
        const div = document.createElement('div');
        div.className = 'similar-job-card';
        div.innerHTML = `
            <div>
                <strong style="color:var(--text-primary); font-size:0.9rem;">${item.title}</strong>
                <div style="font-size:0.8rem; color:var(--text-secondary);">${item.company?.name || ''} · ${item.location?.city || ''}</div>
            </div>
            <span class="score-badge">${(item.score * 100).toFixed(0)}% Similarity</span>
        `;
        div.addEventListener('click', () => openJobModal(item));
        container.appendChild(div);
    });
}

// ==========================================================================
// Resume Matching Flow
// ==========================================================================
function handleResumeFileSelected(file) {
    state.selectedResumeFile = file;
    selectedFilename.textContent = file.name;
    selectedFileInfo.style.display = 'flex';
    dropzone.style.display = 'none';
}

async function submitResumeMatching() {
    const textVal = resumeTextInput.value.trim();
    const hasFile = state.selectedResumeFile !== null;

    if (!hasFile && textVal.length === 0) {
        alert('Please upload a resume file or paste your resume text.');
        return;
    }

    submitResumeBtn.disabled = true;
    submitResumeBtn.innerHTML = '<span>Vectorizing & Matching with Elasticsearch...</span>';

    try {
        let res;
        const visa = resumeVisaCheck.checked ? '&visa_required=true' : '';

        if (hasFile) {
            const formData = new FormData();
            formData.append('file', state.selectedResumeFile);
            res = await fetch(`${API_BASE}/api/v1/jobs/match-resume?limit=10${visa}`, {
                method: 'POST',
                body: formData
            });
        } else {
            res = await fetch(`${API_BASE}/api/v1/jobs/match-resume-text?limit=10${visa}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: textVal })
            });
        }

        if (!res.ok) throw new Error('Resume matching failed');
        const data = await res.json();

        renderResumeMatches(data.jobs);
    } catch (err) {
        alert('Resume match failed: ' + err.message);
    } finally {
        submitResumeBtn.disabled = false;
        submitResumeBtn.innerHTML = '<span>Find Matching Jobs</span>';
    }
}

function renderResumeMatches(jobs) {
    resumeResults.style.display = 'block';
    resumeJobsList.innerHTML = '';

    if (!jobs || jobs.length === 0) {
        resumeJobsList.innerHTML = '<p style="color:var(--text-muted); font-size:0.85rem;">No direct vector matches found.</p>';
        return;
    }

    jobs.forEach(job => {
        const div = document.createElement('div');
        div.className = 'similar-job-card';
        div.innerHTML = `
            <div>
                <strong style="color:var(--text-primary); font-size:0.92rem;">${job.title}</strong>
                <div style="font-size:0.8rem; color:var(--text-secondary);">${job.company?.name} · ${job.location?.city} · ${formatSalary(job.salary)}</div>
            </div>
            <span class="score-badge" style="background:var(--accent-emerald-light); color:var(--accent-emerald); border-color:rgba(16,185,129,0.3);">
                ${(job.score * 100).toFixed(0)}% Match
            </span>
        `;
        div.addEventListener('click', () => {
            resumeModal.style.display = 'none';
            openJobModal(job);
        });
        resumeJobsList.appendChild(div);
    });
}

// ==========================================================================
// Helpers & Active Filter UI
// ==========================================================================
function resetAllFilters() {
    state.query = '';
    searchInput.value = '';
    clearInputBtn.style.display = 'none';
    state.searchMode = 'HYBRID';
    state.sortBy = 'RELEVANCE';
    sortSelect.value = 'RELEVANCE';

    state.filters = {
        workplaceTypes: [],
        experienceLevels: [],
        minSalary: null,
        visaSponsored: null,
        geoRadius: { lat: 33.0198, lon: -96.6989, radiusMiles: 25.0 },
        geoEnabled: false
    };

    geoEnabledCheck.checked = false;
    geoRadiusSlider.value = 25;
    geoRadiusLabel.textContent = '25 miles';
    salarySlider.value = 50000;
    salaryValueLabel.textContent = 'Any salary';
    visaCheck.checked = false;
    fuzzyCheck.checked = true;
    state.fuzzyEnabled = true;

    document.querySelectorAll('input[name="workplace"]').forEach(i => i.checked = false);
    document.querySelectorAll('input[name="experience"]').forEach(i => i.checked = false);

    modeTabs.forEach(t => {
        const match = t.dataset.mode === 'HYBRID';
        t.classList.toggle('active', match);
        t.setAttribute('aria-selected', match ? 'true' : 'false');
    });

    closeFilterDrawer();
    executeSearch();
}

function updateActiveFilterBadge() {
    let count = 0;
    if (state.filters.workplaceTypes.length > 0) count += state.filters.workplaceTypes.length;
    if (state.filters.experienceLevels.length > 0) count += state.filters.experienceLevels.length;
    if (state.filters.minSalary) count += 1;
    if (state.filters.visaSponsored) count += 1;
    if (state.filters.geoEnabled) count += 1;

    if (count > 0) {
        activeFilterCountBadge.textContent = count;
        activeFilterCountBadge.style.display = 'inline-block';
    } else {
        activeFilterCountBadge.style.display = 'none';
    }
}

function renderActiveChips() {
    const chips = [];

    if (state.filters.geoEnabled) {
        chips.push({ label: `Within ${state.filters.geoRadius.radiusMiles}mi of Plano, TX`, clear: () => { geoEnabledCheck.checked = false; state.filters.geoEnabled = false; } });
    }
    state.filters.workplaceTypes.forEach(w => {
        chips.push({ label: w, clear: () => {
            const cb = document.querySelector(`input[name="workplace"][value="${w}"]`);
            if (cb) cb.checked = false;
            state.filters.workplaceTypes = state.filters.workplaceTypes.filter(x => x !== w);
        }});
    });
    if (state.filters.minSalary) {
        chips.push({ label: `Salary >= $${(state.filters.minSalary / 1000).toFixed(0)}k`, clear: () => {
            salarySlider.value = 50000;
            salaryValueLabel.textContent = 'Any salary';
            state.filters.minSalary = null;
        }});
    }
    state.filters.experienceLevels.forEach(e => {
        chips.push({ label: e, clear: () => {
            const cb = document.querySelector(`input[name="experience"][value="${e}"]`);
            if (cb) cb.checked = false;
            state.filters.experienceLevels = state.filters.experienceLevels.filter(x => x !== e);
        }});
    });
    if (state.filters.visaSponsored) {
        chips.push({ label: 'Visa Sponsorship', clear: () => { visaCheck.checked = false; state.filters.visaSponsored = null; } });
    }

    if (chips.length === 0) {
        activeChipsBar.style.display = 'none';
        return;
    }

    activeChipsBar.style.display = 'flex';
    activeChipsBar.innerHTML = '';
    chips.forEach(c => {
        const div = document.createElement('div');
        div.className = 'active-chip';
        div.innerHTML = `<span>${c.label}</span><button>&times;</button>`;
        div.querySelector('button').addEventListener('click', () => {
            c.clear();
            executeSearch();
        });
        activeChipsBar.appendChild(div);
    });
}

function openFilterDrawer() {
    filterSidebar.classList.add('open');
    drawerOverlay.classList.add('active');
}

function closeFilterDrawer() {
    filterSidebar.classList.remove('open');
    drawerOverlay.classList.remove('active');
}

function formatSalary(salary) {
    if (!salary || (!salary.min && !salary.max)) return 'Competitive';
    const minK = salary.min ? `$${(salary.min / 1000).toFixed(0)}k` : '';
    const maxK = salary.max ? `$${(salary.max / 1000).toFixed(0)}k` : '';
    return minK && maxK ? `${minK} - ${maxK} / yr` : `${minK || maxK} / yr`;
}

function formatScore(score, mode) {
    if (mode === 'HYBRID') {
        return `RRF Score: ${score.toFixed(4)}`;
    } else if (mode === 'SEMANTIC') {
        return `${(score * 100).toFixed(0)}% Match`;
    } else {
        return `Relevance: ${score.toFixed(1)}`;
    }
}

function renderLoadingSkeleton() {
    jobsContainer.innerHTML = Array(3).fill(0).map(() => `
        <div class="job-card" style="opacity: 0.5;">
            <div class="job-card-header">
                <div class="company-avatar" style="background: rgba(255,255,255,0.05);"></div>
                <div class="job-meta-header">
                    <div style="height: 18px; width: 50%; background: rgba(255,255,255,0.06); border-radius: 4px; margin-bottom: 8px;"></div>
                    <div style="height: 12px; width: 30%; background: rgba(255,255,255,0.04); border-radius: 4px;"></div>
                </div>
            </div>
            <div style="height: 12px; width: 80%; background: rgba(255,255,255,0.03); border-radius: 4px; margin-bottom: 8px;"></div>
        </div>
    `).join('');
}
