const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];

const state = {
  session: safeJson(localStorage.getItem('impulse_session')),
  collections: [],
  memories: [],
  plans: [],
  lastPlan: null,
  memoryFilter: 'All',
  loadErrors: {}
};

function safeJson(value) {
  try { return JSON.parse(value || 'null'); } catch { return null; }
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll("'", '&#039;');
}

function apiErrorMessage(body, status) {
  if (body?.message) return body.message;
  if (body?.detail) return body.detail;
  if (body?.errors) return Object.values(body.errors).join(' ');
  if (status === 401) return 'Your session has expired. Please sign in again.';
  if (status === 409) return 'That already exists.';
  return `Something went wrong (HTTP ${status}).`;
}

async function api(path, options = {}) {
  let response;
  try {
    response = await fetch(path, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(state.session?.token ? { Authorization: `Bearer ${state.session.token}` } : {}),
        ...(options.headers || {})
      }
    });
  } catch {
    throw new Error('Cannot reach Impulse. Check that the backend is running and try again.');
  }
  if (!response.ok) {
    let body = null;
    try { body = await response.json(); } catch {}
    if (response.status === 401 && state.session) {
      applySession(null);
      location.hash = 'login';
    }
    throw new Error(apiErrorMessage(body, response.status));
  }
  return response.status === 204 ? null : response.json();
}

function toast(message) {
  const element = $('#toast');
  element.textContent = message;
  element.classList.add('visible');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => element.classList.remove('visible'), 3600);
}

function feedback(message, type = 'error', action = '') {
  return `<div class="feedback ${type}" ${type === 'error' ? 'role="alert"' : ''}>
    <span>${escapeHtml(message)}</span>${action}
  </div>`;
}

function setBusy(form, busy, busyLabel) {
  form.setAttribute('aria-busy', String(busy));
  form.querySelectorAll('button, input, textarea, select').forEach(element => { element.disabled = busy; });
  const submit = form.querySelector('[type="submit"] span:first-child, [type="submit"]');
  if (!submit) return;
  if (busy) {
    submit.dataset.label = submit.textContent;
    if (busyLabel) submit.textContent = busyLabel;
  } else if (submit.dataset.label) {
    submit.textContent = submit.dataset.label;
    delete submit.dataset.label;
  }
}

function initials(name) {
  return (name || 'I').split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
}

function applySession(session) {
  state.session = session;
  if (session) localStorage.setItem('impulse_session', JSON.stringify(session));
  else localStorage.removeItem('impulse_session');
  $('#auth-panel').classList.toggle('hidden', Boolean(session));
  $('#workspace-content').classList.toggle('hidden', !session);
  $('#account-state').classList.toggle('hidden', !session);
  if (!session) return;
  const label = session.displayName || session.email;
  $('#account-name').textContent = label;
  $('#account-avatar').textContent = initials(label);
  $('#profile-avatar').textContent = initials(label);
  $('#profile-name').textContent = label;
  $('#profile-email').textContent = session.email;
  $('#user-id').value = session.userId;
  $('#home-greeting').textContent = `Good to see you, ${label.split(' ')[0]}.`;
}

function route() {
  const raw = location.hash.replace(/^#/, '') || (state.session ? 'home' : 'login');
  if (!state.session) {
    const authRoute = raw === 'register' ? 'register' : 'login';
    showAuth(authRoute);
    if (raw !== authRoute) history.replaceState(null, '', `#${authRoute}`);
    return;
  }
  if (raw === 'login' || raw === 'register') {
    history.replaceState(null, '', '#home');
    return route();
  }
  const page = ['home', 'capture', 'collections', 'memories', 'plans', 'account'].includes(raw) ? raw : 'home';
  $$('.app-page').forEach(element => element.classList.toggle('hidden', element.dataset.page !== page));
  $$('.app-nav a').forEach(link => {
    const active = link.dataset.route === page;
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
  });
  document.title = `${page[0].toUpperCase()}${page.slice(1)} · Impulse`;
  window.scrollTo({ top: 0, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
}

function showAuth(mode) {
  const register = mode === 'register';
  state.authMode = mode;
  $$('.register-only').forEach(element => element.classList.toggle('hidden', !register));
  $('#display-name').required = register;
  $('#auth-back').classList.toggle('hidden', !register);
  $('#auth-kicker').textContent = register ? 'Start your memory' : 'Welcome back';
  $('#auth-title').textContent = register ? 'Create your account' : 'Sign in to Impulse';
  $('#auth-copy').textContent = register
    ? 'Save your first source and build a useful personal library.'
    : 'Continue building your personal memory.';
  $('#auth-submit span:first-child').textContent = register ? 'Create account' : 'Sign in';
  $('#auth-switch').innerHTML = register
    ? 'Already have an account? <a href="#login">Sign in</a>'
    : 'New to Impulse? <a href="#register">Create an account</a>';
  $('#auth-password').autocomplete = register ? 'new-password' : 'current-password';
  clearAuthErrors();
  document.title = `${register ? 'Create account' : 'Sign in'} · Impulse`;
}

function clearAuthErrors() {
  $$('.field-error').forEach(element => { element.textContent = ''; });
  $('#auth-result').innerHTML = '';
}

function validateAuth() {
  clearAuthErrors();
  let valid = true;
  if (state.authMode === 'register' && $('#display-name').value.trim().length < 2) {
    $('#display-name-error').textContent = 'Enter at least 2 characters.';
    valid = false;
  }
  if (!$('#auth-email').validity.valid) {
    $('#auth-email-error').textContent = 'Enter a valid email address.';
    valid = false;
  }
  if ($('#auth-password').value.length < 8) {
    $('#auth-password-error').textContent = 'Password must be at least 8 characters.';
    valid = false;
  }
  return valid;
}

async function checkHealth() {
  try {
    const status = await api('/api/system/status');
    const ready = status.backend === 'UP' && status.aiReady;
    $('#service-state').classList.toggle('ready', ready);
    $('#service-state span:last-child').textContent = ready ? 'All systems ready' : 'Planning temporarily unavailable';
  } catch {
    $('#service-state').classList.remove('ready');
    $('#service-state span:last-child').textContent = 'Some features unavailable';
  }
}

async function loadResource(key, path) {
  try {
    const data = await api(path);
    state[key] = data;
    delete state.loadErrors[key];
  } catch (error) {
    state.loadErrors[key] = error.message;
  }
}

async function loadWorkspace() {
  if (!state.session) return;
  const user = encodeURIComponent(state.session.userId);
  await Promise.allSettled([
    loadResource('collections', `/api/collections?userId=${user}`),
    loadResource('memories', `/api/memories?userId=${user}`),
    loadResource('plans', `/api/plans?userId=${user}`)
  ]);
  renderAll();
}

function renderAll() {
  renderMetrics();
  renderCollectionPicker();
  renderCollections();
  renderMemoryFilters();
  renderMemoryLibrary();
  renderSavedPlans();
  renderHome();
}

function renderMetrics() {
  $('#memory-count').textContent = state.loadErrors.memories ? '!' : state.memories.length;
  $('#collection-count').textContent = state.loadErrors.collections ? '!' : state.collections.length;
  $('#plan-count').textContent = state.loadErrors.plans ? '!' : state.plans.length;
}

function emptyState(title, copy, href, label) {
  return `<div class="empty-card"><span class="empty-mark">I</span><h3>${escapeHtml(title)}</h3><p>${escapeHtml(copy)}</p>
    ${href ? `<a class="button primary compact" href="${href}">${escapeHtml(label)}</a>` : ''}</div>`;
}

function thumbnail(url, title, className = 'source-thumbnail', decorative = false) {
  return url
    ? `<img class="${className}" src="${escapeHtml(url)}" alt="${decorative ? '' : escapeHtml(`${title || 'Saved source'} thumbnail`)}" loading="lazy">`
    : `<div class="${className} fallback" aria-hidden="true">${escapeHtml((title || 'M').slice(0, 1).toUpperCase())}</div>`;
}

function platformFromUrl(url) {
  try {
    const host = new URL(url).hostname.replace(/^www\./, '');
    if (host.includes('youtube') || host === 'youtu.be') return 'YouTube';
    if (host.includes('instagram')) return 'Instagram';
    return host;
  } catch { return 'Web'; }
}

function memoryCard(memory) {
  return `<article class="memory-card">
    <a class="memory-media" href="${escapeHtml(memory.sourceUrl)}" target="_blank" rel="noopener noreferrer">
      ${thumbnail(memory.thumbnailUrl, memory.title, 'memory-image')}
      <span class="platform-badge">${escapeHtml(platformFromUrl(memory.sourceUrl))}</span>
    </a>
    <div class="memory-copy">
      <span class="ready-label">Ready</span>
      <h3>${escapeHtml(memory.title)}</h3>
      <p>${escapeHtml(memory.summary)}</p>
      <div class="memory-meta"><span>${escapeHtml(memory.category || 'Saved')}</span><a href="${escapeHtml(memory.sourceUrl)}" target="_blank" rel="noopener noreferrer">Open original ↗</a></div>
    </div>
  </article>`;
}

function renderHome() {
  const active = state.plans.find(plan => plan.status === 'ACTIVE');
  const activeSection = $('#active-plan-section');
  activeSection.classList.toggle('hidden', !active);
  if (active) $('#active-plan').innerHTML = savedPlanRow(active);

  const memories = $('#recent-memories');
  memories.classList.remove('skeleton-region');
  memories.innerHTML = state.loadErrors.memories
    ? feedback(state.loadErrors.memories, 'error', '<button class="inline-retry" data-retry="memories">Retry</button>')
    : state.memories.length ? state.memories.slice(0, 4).map(memoryCard).join('')
      : emptyState('No memories yet', 'Save one useful link to start building personal context.', '#capture', 'Save your first link');

  const plans = $('#recent-plans');
  plans.classList.remove('skeleton-region');
  plans.innerHTML = state.loadErrors.plans
    ? feedback(state.loadErrors.plans, 'error', '<button class="inline-retry" data-retry="plans">Retry</button>')
    : state.plans.length ? state.plans.slice(0, 3).map(savedPlanRow).join('')
      : emptyState('No saved plans', 'Create a plan grounded in your memories.', '#plans', 'Create a plan');
}

function renderCollectionPicker() {
  const picker = $('#capture-collection');
  if (state.loadErrors.collections) {
    picker.innerHTML = '<option value="">Collections unavailable</option>';
    return;
  }
  const all = state.collections.find(item => item.name.toUpperCase() === 'ALL');
  picker.innerHTML = state.collections.map(collection =>
    `<option value="${collection.id}" ${collection.id === all?.id ? 'selected' : ''}>${escapeHtml(collection.name)}</option>`
  ).join('');
}

function collectionCover(collection) {
  const byId = Object.fromEntries(state.memories.map(memory => [memory.id, memory]));
  const covers = collection.sources.map(source => byId[source.memoryId]).filter(Boolean).slice(0, 4);
  if (!covers.length) return `<div class="collection-cover fallback-cover"><span>${escapeHtml(collection.name.slice(0, 1))}</span></div>`;
  return `<div class="collection-cover collage count-${covers.length}">${covers.map(memory => thumbnail(memory.thumbnailUrl, memory.title, 'collection-image', true)).join('')}</div>`;
}

function renderCollection(collection) {
  const isAll = collection.name.toUpperCase() === 'ALL';
  const sources = collection.sources.map(source => `
    <div class="source-row">
      <div><span class="status ${source.status.toLowerCase()}">${source.status === 'PROCESSED' ? 'READY' : escapeHtml(source.status.replaceAll('_', ' '))}</span>
      <a href="${escapeHtml(source.url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(platformFromUrl(source.url))} · ${escapeHtml(source.url)}</a>
      ${source.errorMessage ? `<span class="error-copy">${escapeHtml(source.errorMessage)}</span>` : ''}</div>
      <button class="button ghost compact remove-source" type="button" data-collection-id="${collection.id}" data-source-id="${source.id}">Remove</button>
    </div>`).join('');
  return `<article class="collection-card visual-card">
    <div class="collection-visual">${collectionCover(collection)}
      <div class="collection-overlay"><div><h2>${escapeHtml(collection.name)}</h2><p>${collection.totalSources} saved · ${collection.processedSources} ready</p></div>
      ${isAll ? '<span class="protected-label">Default</span>' : `<button class="icon-button edit-collection" type="button" data-id="${collection.id}" aria-label="Edit ${escapeHtml(collection.name)}">•••</button>`}</div>
    </div>
    ${collection.description ? `<p class="collection-description">${escapeHtml(collection.description)}</p>` : ''}
    <details><summary>View ${collection.totalSources} source${collection.totalSources === 1 ? '' : 's'}</summary>
      <div class="source-list">${sources || '<p class="empty-state">No sources in this collection.</p>'}</div>
    </details>
  </article>`;
}

function renderCollections() {
  const container = $('#collections');
  container.classList.remove('skeleton-region');
  if (state.loadErrors.collections) {
    container.innerHTML = feedback(state.loadErrors.collections, 'error', '<button class="inline-retry" data-retry="collections">Retry</button>');
  } else {
    container.innerHTML = state.collections.length
      ? state.collections.map(renderCollection).join('')
      : emptyState('No collections yet', 'Save a link and Impulse will create your default ALL collection.', '#capture', 'Save a link');
  }
}

function renderMemoryFilters() {
  const categories = ['All', ...new Set(state.memories.map(memory => memory.category).filter(Boolean))];
  $('#memory-filter-row').innerHTML = categories.map(category =>
    `<button type="button" class="${state.memoryFilter === category ? 'active' : ''}" data-filter="${escapeHtml(category)}">${escapeHtml(category)}</button>`
  ).join('');
}

function renderMemoryLibrary() {
  const container = $('#memory-library');
  container.classList.remove('skeleton-region');
  if (state.loadErrors.memories) {
    container.innerHTML = feedback(state.loadErrors.memories, 'error', '<button class="inline-retry" data-retry="memories">Retry</button>');
    return;
  }
  const filtered = state.memoryFilter === 'All' ? state.memories : state.memories.filter(memory => memory.category === state.memoryFilter);
  container.innerHTML = filtered.length ? filtered.map(memoryCard).join('')
    : emptyState(state.memories.length ? 'Nothing in this filter' : 'Your memory library is empty',
      state.memories.length ? 'Choose another category to keep browsing.' : 'Save your first useful source and it will appear here.',
      state.memories.length ? '' : '#capture', 'Save a link');
}

function savedPlanRow(plan) {
  const progress = plan.plan.filter(step => step.completed).length;
  return `<button class="saved-plan-row" type="button" data-plan-id="${plan.id}">
    <span class="saved-plan-icon">I</span><span><small>${plan.status || 'SAVED'} · ${new Date(plan.createdAt).toLocaleDateString()}</small><strong>${escapeHtml(plan.goal)}</strong>
    <span>${progress}/${plan.plan.length} complete · ${plan.retrievedMemoryIds.length} memories</span></span><b>→</b></button>`;
}

function renderSavedPlans() {
  const container = $('#saved-plans');
  container.classList.remove('skeleton-region');
  if (state.loadErrors.plans) {
    container.innerHTML = feedback(state.loadErrors.plans, 'error', '<button class="inline-retry" data-retry="plans">Retry</button>');
  } else {
    container.innerHTML = state.plans.length ? state.plans.map(savedPlanRow).join('')
      : emptyState('No saved plans yet', 'Create a personalized plan, then save it for later.', '', '');
  }
}

function renderSearchMemory(memory) {
  const copy = { ...memory, summary: memory.summary || 'No summary available.' };
  return `<div class="search-memory-wrap">${memoryCard(copy)}<span class="match-label">${Math.round(memory.score * 100)}% match</span></div>`;
}

function renderPlan(result, saved = false) {
  const memoryById = Object.fromEntries((result.groundingMemories || []).map(memory => [memory.id, memory]));
  const groundingLabel = {
    STRONG_GROUNDING: `Strongly grounded · ${(result.groundingMemories || []).length} memories`,
    PARTIAL_GROUNDING: `Partially grounded · ${(result.groundingMemories || []).length} memories`,
    NO_GROUNDING: 'No matching saved memories'
  }[result.groundingStatus] || `Based on ${(result.groundingMemories || []).length} memories`;
  const needsStarterChoice = !saved && result.groundingStatus === 'NO_GROUNDING' && !result.plan.length;
  const completedCount = saved ? result.plan.filter(step => step.completed).length : 0;
  const allStepsCompleted = result.plan.length > 0 && completedCount === result.plan.length;
  $('#plan-result').innerHTML = `<section class="plan-detail">
    <header class="plan-hero"><p class="section-kicker">${saved ? 'Saved plan' : groundingLabel}</p>
      <div class="modal-heading"><h2>${escapeHtml(result.goal)}</h2>
        ${saved ? '<div><button id="edit-plan" class="icon-button" type="button" aria-label="Edit plan goal">✎</button><button id="delete-plan" class="icon-button" type="button" aria-label="Delete plan">⌫</button></div>' : ''}
      </div><p>${escapeHtml(result.explanation)}</p>
      <div class="plan-progress"><span><strong id="completed-count">${completedCount}</strong> / ${result.plan.length} complete</span><div class="progress-track"><span id="progress-fill"></span></div></div>
      ${needsStarterChoice ? `<div class="empty-state">
        ${(result.missingContext || []).length ? `<p><strong>Helpful details:</strong> ${escapeHtml(result.missingContext.join(', '))}</p>` : ''}
        ${(result.suggestedSources || []).length ? `<p><strong>Save next:</strong> ${escapeHtml(result.suggestedSources.join(', '))}</p>` : ''}
        <button id="create-starter-plan" class="button dark-save" type="button">Create general starter plan</button>
        <small>General guidance will be labelled separately from saved memory.</small>
      </div>` : saved ? `<span class="saved-label">${result.status === 'COMPLETED' ? 'Completed ✓' : result.status === 'ACTIVE' ? 'Active plan' : 'Saved ✓'}</span>
        ${result.status === 'SAVED' ? '<button id="activate-plan" class="button dark-save" type="button">Activate plan</button>' : ''}
        ${result.status === 'ACTIVE' && allStepsCompleted ? '<button id="complete-plan" class="button dark-save" type="button">Mark plan done</button>' : ''}`
        : '<button id="save-plan" class="button dark-save" type="button">Save this plan</button>'}
    </header>
    <div class="plan-steps">${result.plan.map((step, index) => `<article class="plan-step interactive ${step.completed ? 'complete' : ''}">
      <button class="step-check" type="button" data-step-id="${escapeHtml(step.id || '')}" data-step-number="${index + 1}" aria-label="Mark step ${index + 1} complete" aria-pressed="${step.completed ? 'true' : 'false'}" ${saved && result.status === 'ACTIVE' ? '' : 'disabled'}>${step.completed ? '✓' : index + 1}</button>
      <div class="step-body"><strong>${escapeHtml(step.step)}</strong>
        <div class="step-meta">${step.durationMinutes ? `<span>${step.durationMinutes} min</span>` : ''}<span>${step.sourceType === 'GENERAL' ? 'General guidance' : `${step.memoryIds.length} source${step.memoryIds.length === 1 ? '' : 's'}`}</span></div>
        ${step.reason ? `<details><summary>Why this step?</summary><p>${escapeHtml(step.reason)}</p></details>` : ''}
        <div class="step-sources">${step.memoryIds.map(id => {
          const memory = memoryById[id];
          return memory ? `<a href="${escapeHtml(memory.sourceUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(memory.title)} ↗</a>` : '';
        }).join('')}</div>
      </div></article>`).join('')}</div>
    ${(result.groundingMemories || []).length ? `<aside class="grounding-panel"><p class="section-kicker">Sources behind this plan</p><h3>Grounded in what you saved</h3>
      ${result.groundingMemories.map(memory => `<a class="grounding-card" href="${escapeHtml(memory.sourceUrl)}" target="_blank" rel="noopener noreferrer">
        ${thumbnail(memory.thumbnailUrl, memory.title)}<span><small>${escapeHtml(memory.platform.replaceAll('_', ' '))}</small><strong>${escapeHtml(memory.title)}</strong></span><b>↗</b></a>`).join('')}</aside>` : ''}
  </section>`;
  bindPlanChecks(result, saved);
  $('#progress-fill').style.width = `${result.plan.length ? completedCount / result.plan.length * 100 : 0}%`;
  $('#create-starter-plan')?.addEventListener('click', () => createPlan(result.goal, $('#plan-form'), true));
}

function bindPlanChecks(result, saved) {
  const checks = $$('#plan-result .step-check');
  const update = () => {
    const count = checks.filter(button => button.getAttribute('aria-pressed') === 'true').length;
    $('#completed-count').textContent = count;
    $('#progress-fill').style.width = `${checks.length ? count / checks.length * 100 : 0}%`;
  };
  checks.forEach(button => button.addEventListener('click', async () => {
    if (!saved || result.status !== 'ACTIVE') return;
    const checked = button.getAttribute('aria-pressed') === 'true';
    if (saved) {
      button.disabled = true;
      try {
        const updated = await api(`/api/plans/${encodeURIComponent(result.id)}/steps/${encodeURIComponent(button.dataset.stepId)}?userId=${encodeURIComponent(state.session.userId)}`, {
          method: 'PATCH',
          body: JSON.stringify({ completed: !checked })
        });
        state.plans = state.plans.map(plan => plan.id === updated.id ? updated : plan);
        renderPlan(updated, true);
      } catch (error) { button.disabled = false; toast(error.message); }
      return;
    }
    button.setAttribute('aria-pressed', String(!checked));
    button.textContent = checked ? button.dataset.stepNumber : '✓';
    button.closest('.plan-step').classList.toggle('complete', !checked);
    update();
  }));
}

function confirmAction(title, message, label = 'Remove') {
  const dialog = $('#confirm-dialog');
  $('#confirm-title').textContent = title;
  $('#confirm-message').textContent = message;
  $('#confirm-action').textContent = label;
  dialog.showModal();
  return new Promise(resolve => {
    dialog.addEventListener('close', () => resolve(dialog.returnValue === 'confirm'), { once: true });
  });
}

async function retryResource(key) {
  const user = encodeURIComponent(state.session.userId);
  const paths = { collections: `/api/collections?userId=${user}`, memories: `/api/memories?userId=${user}`, plans: `/api/plans?userId=${user}` };
  await loadResource(key, paths[key]);
  renderAll();
}

$('#auth-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (!validateAuth()) return;
  const form = event.currentTarget;
  setBusy(form, true, state.authMode === 'register' ? 'Creating account…' : 'Signing in…');
  try {
    const body = { email: $('#auth-email').value.trim(), password: $('#auth-password').value };
    if (state.authMode === 'register') body.displayName = $('#display-name').value.trim();
    const session = await api(`/api/auth/${state.authMode}`, { method: 'POST', body: JSON.stringify(body) });
    applySession(session);
    form.reset();
    location.hash = 'home';
    await loadWorkspace();
    toast(state.authMode === 'register' ? 'Your account is ready' : 'Welcome back');
  } catch (error) {
    $('#auth-result').innerHTML = feedback(error.message);
  } finally { setBusy(form, false); }
});

$('#toggle-password').addEventListener('click', () => {
  const input = $('#auth-password');
  const visible = input.type === 'text';
  input.type = visible ? 'password' : 'text';
  $('#toggle-password').textContent = visible ? 'Show' : 'Hide';
  $('#toggle-password').setAttribute('aria-label', visible ? 'Show password' : 'Hide password');
});

async function logout() {
  try { await api('/api/auth/logout', { method: 'POST' }); } catch {}
  applySession(null);
  state.collections = []; state.memories = []; state.plans = []; state.lastPlan = null;
  location.hash = 'login';
}
$('#logout').addEventListener('click', logout);
$('#account-logout').addEventListener('click', logout);

$('#capture-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const input = $('#capture-url');
  $('#capture-url-error').textContent = '';
  try { new URL(input.value); } catch { $('#capture-url-error').textContent = 'Enter a complete HTTP or HTTPS URL.'; return; }
  if (!/^https?:\/\//i.test(input.value)) { $('#capture-url-error').textContent = 'Only HTTP and HTTPS links are supported.'; return; }
  if (!$('#capture-collection').value) { $('#capture-result').innerHTML = feedback('Choose a collection before saving.'); return; }
  setBusy(form, true, 'Saving memory…');
  $('#capture-result').innerHTML = feedback('Reading your source and creating memory…', 'success');
  try {
    const collection = await api('/api/collections/sources', {
      method: 'POST',
      body: JSON.stringify({
        userId: state.session.userId,
        collectionId: $('#capture-collection').value,
        url: input.value.trim(),
        userNote: $('#capture-note').value.trim() || null
      })
    });
    form.reset();
    $('#capture-result').innerHTML = feedback(`Saved to ${collection.name}. ${collection.processedSources} source${collection.processedSources === 1 ? ' is' : 's are'} ready.`, 'success',
      '<a href="#collections">View collection →</a>');
    await loadWorkspace();
    toast('Memory saved');
  } catch (error) { $('#capture-result').innerHTML = feedback(error.message); }
  finally { setBusy(form, false); }
});

$('#collections').addEventListener('click', async event => {
  const edit = event.target.closest('.edit-collection');
  if (edit) {
    const collection = state.collections.find(item => item.id === edit.dataset.id);
    if (!collection) return;
    $('#edit-collection-id').value = collection.id;
    $('#edit-collection-name').value = collection.name;
    $('#edit-collection-description').value = collection.description || '';
    $('#collection-edit-result').innerHTML = '';
    $('#collection-dialog').showModal();
    return;
  }
  const remove = event.target.closest('.remove-source');
  if (!remove || !await confirmAction('Remove this source?', 'It will leave this collection, but its generated memory will remain available.', 'Remove source')) return;
  try {
    await api(`/api/collections/${remove.dataset.collectionId}/sources/${remove.dataset.sourceId}?userId=${encodeURIComponent(state.session.userId)}`, { method: 'DELETE' });
    await loadWorkspace(); toast('Source removed from collection');
  } catch (error) { toast(error.message); }
});

$('.close-dialog').addEventListener('click', () => $('#collection-dialog').close());
$('#collection-edit-form').addEventListener('submit', async event => {
  event.preventDefault(); const form = event.currentTarget; setBusy(form, true, 'Saving…');
  try {
    await api(`/api/collections/${$('#edit-collection-id').value}`, { method: 'PATCH', body: JSON.stringify({
      userId: state.session.userId, name: $('#edit-collection-name').value.trim(), description: $('#edit-collection-description').value.trim() || null
    }) });
    $('#collection-dialog').close(); await loadWorkspace(); toast('Collection updated');
  } catch (error) { $('#collection-edit-result').innerHTML = feedback(error.message); }
  finally { setBusy(form, false); }
});

$('#delete-collection').addEventListener('click', async () => {
  if (!await confirmAction('Delete this collection?', 'The collection will be removed. Its generated memories will remain in your library.', 'Delete collection')) return;
  try {
    await api(`/api/collections/${$('#edit-collection-id').value}?userId=${encodeURIComponent(state.session.userId)}`, { method: 'DELETE' });
    $('#collection-dialog').close(); await loadWorkspace(); toast('Collection deleted');
  } catch (error) { $('#collection-edit-result').innerHTML = feedback(error.message); }
});

$('#search-form').addEventListener('submit', async event => {
  event.preventDefault(); const form = event.currentTarget; const target = $('#search-result');
  setBusy(form, true, 'Searching…'); target.innerHTML = feedback('Finding your best matching memories…', 'success');
  try {
    const results = await api('/api/memories/search', { method: 'POST', body: JSON.stringify({
      userId: state.session.userId, query: $('#search-query').value.trim(), limit: 12
    }) });
    target.innerHTML = results.length ? results.map(renderSearchMemory).join('') : emptyState('No matching memories', 'Try different words or save another useful source.', '#capture', 'Add a source');
    $('#memory-library').classList.toggle('hidden', results.length > 0);
  } catch (error) { target.innerHTML = feedback(error.message); }
  finally { setBusy(form, false); }
});

$('#search-query').addEventListener('input', event => {
  if (!event.target.value) { $('#search-result').innerHTML = ''; $('#memory-library').classList.remove('hidden'); }
});
$('#memory-filter-row').addEventListener('click', event => {
  const button = event.target.closest('[data-filter]'); if (!button) return;
  state.memoryFilter = button.dataset.filter; renderMemoryFilters(); renderMemoryLibrary();
});

async function createPlan(query, form, allowGeneralKnowledge = false) {
  location.hash = 'plans';
  $('#plan-query').value = query;
  $('#plan-loading').classList.remove('hidden');
  setBusy(form, true, 'Creating plan…');
  try {
    state.lastPlan = await api('/api/impulse/plan', { method: 'POST', body: JSON.stringify({
      userId: state.session.userId, query, constraints: {}, allowGeneralKnowledge
    }) });
    renderPlan(state.lastPlan);
    $('#plan-result').scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (error) { $('#plan-result').innerHTML = feedback(error.message); }
  finally { $('#plan-loading').classList.add('hidden'); setBusy(form, false); }
}

$('#plan-form').addEventListener('submit', event => {
  event.preventDefault(); createPlan($('#plan-query').value.trim(), event.currentTarget);
});
$('#home-plan-form').addEventListener('submit', event => {
  event.preventDefault(); createPlan($('#home-plan-query').value.trim(), $('#plan-form'));
});
$$('[data-prompt]').forEach(button => button.addEventListener('click', () => {
  $('#home-plan-query').value = button.dataset.prompt; $('#home-plan-query').focus();
}));

$('#plan-result').addEventListener('click', async event => {
  const edit = event.target.closest('#edit-plan');
  if (edit) {
    const current = state.plans.find(plan => plan.id === state.lastSavedPlanId);
    if (!current) return;
    $('#plan-update-goal').value = current.goal;
    $('#plan-update-dialog').showModal();
    return;
  }
  const remove = event.target.closest('#delete-plan');
  if (remove) {
    if (!await confirmAction('Delete this plan and its progress? Your memories will stay saved.')) return;
    try {
      await api(`/api/plans/${encodeURIComponent(state.lastSavedPlanId)}?userId=${encodeURIComponent(state.session.userId)}`, { method: 'DELETE' });
      state.plans = state.plans.filter(plan => plan.id !== state.lastSavedPlanId);
      state.lastSavedPlanId = null;
      $('#plan-result').innerHTML = '';
      renderSavedPlans(); renderHome(); toast('Plan deleted');
    } catch (error) { toast(error.message); }
    return;
  }
  const activate = event.target.closest('#activate-plan');
  if (activate) {
    activate.disabled = true;
    try {
      const updated = await api(`/api/plans/${encodeURIComponent(state.lastSavedPlanId)}/activate?userId=${encodeURIComponent(state.session.userId)}`, { method: 'PATCH' });
      state.plans = state.plans.map(plan => plan.id === updated.id ? updated : plan); renderPlan(updated, true);
    } catch (error) { activate.disabled = false; toast(error.message); }
    return;
  }
  const complete = event.target.closest('#complete-plan');
  if (complete) {
    const current = state.plans.find(plan => plan.id === state.lastSavedPlanId);
    if (!current) return;
    complete.disabled = true;
    try {
      const updated = await api(`/api/plans/${encodeURIComponent(current.id)}/complete?userId=${encodeURIComponent(state.session.userId)}`, { method: 'PATCH' });
      state.plans = state.plans.map(plan => plan.id === updated.id ? updated : plan); renderPlan(updated, true); toast('Plan completed');
    } catch (error) { complete.disabled = false; toast(error.message); }
    return;
  }
  const button = event.target.closest('#save-plan'); if (!button || !state.lastPlan) return;
  button.disabled = true; button.textContent = 'Saving…';
  try {
    const saved = await api('/api/plans', { method: 'POST', body: JSON.stringify({
      userId: state.session.userId, goal: state.lastPlan.goal, explanation: state.lastPlan.explanation,
      plan: state.lastPlan.plan, retrievedMemoryIds: state.lastPlan.retrievedMemoryIds
    }) });
    state.plans.unshift(saved); renderSavedPlans(); renderHome(); button.textContent = 'Saved ✓'; toast('Plan saved');
  } catch (error) { button.disabled = false; button.textContent = 'Save this plan'; toast(error.message); }
});

$('#close-plan-update').addEventListener('click', () => $('#plan-update-dialog').close());
$('#cancel-plan-update').addEventListener('click', () => $('#plan-update-dialog').close());
$('#plan-update-form').addEventListener('submit', async event => {
  event.preventDefault();
  const goal = $('#plan-update-goal').value.trim();
  if (!goal || !state.lastSavedPlanId) return;
  setBusy(event.currentTarget, true, 'Replacing plan…');
  try {
    const updated = await api(`/api/plans/${encodeURIComponent(state.lastSavedPlanId)}/regenerate`, {
      method: 'POST',
      body: JSON.stringify({ userId: state.session.userId, constraints: {}, query: goal })
    });
    state.plans = state.plans.map(plan => plan.id === updated.id ? updated : plan);
    $('#plan-update-dialog').close();
    renderPlan(updated, true);
    renderSavedPlans();
    renderHome();
    toast('Plan updated');
  } catch (error) { toast(error.message); }
  finally { setBusy(event.currentTarget, false); }
});

function openSavedPlan(id) {
  const plan = state.plans.find(item => item.id === id); if (!plan) return;
  state.lastSavedPlanId = id;
  location.hash = 'plans'; renderPlan(plan, true); $('#plan-result').scrollIntoView({ behavior: 'smooth', block: 'start' });
}
$('#saved-plans').addEventListener('click', event => { const card = event.target.closest('[data-plan-id]'); if (card) openSavedPlan(card.dataset.planId); });
$('#recent-plans').addEventListener('click', event => { const card = event.target.closest('[data-plan-id]'); if (card) openSavedPlan(card.dataset.planId); });
$('#active-plan').addEventListener('click', event => { const card = event.target.closest('[data-plan-id]'); if (card) openSavedPlan(card.dataset.planId); });
$('#refresh-plans').addEventListener('click', () => retryResource('plans'));

document.addEventListener('click', event => {
  const retry = event.target.closest('[data-retry]'); if (retry) retryResource(retry.dataset.retry);
});
window.addEventListener('hashchange', route);
window.addEventListener('online', () => { toast('You are back online'); checkHealth(); });
window.addEventListener('offline', () => toast('You are offline. Saved content may be unavailable.'));

applySession(state.session);
route();
checkHealth();
if (state.session) loadWorkspace();
