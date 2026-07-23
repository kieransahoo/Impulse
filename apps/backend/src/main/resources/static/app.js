const state = {
  userId: localStorage.getItem('impulse_test_user_id') || crypto.randomUUID()
};

const $ = selector => document.querySelector(selector);
const escapeHtml = value => String(value ?? '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#039;');

function setUserId(value) {
  state.userId = value;
  localStorage.setItem('impulse_test_user_id', value);
  $('#user-id').value = value;
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
  });
  if (!response.ok) {
    let message = `Request failed (HTTP ${response.status})`;
    try {
      const body = await response.json();
      message = body.message || body.detail || message;
    } catch (ignored) {}
    throw new Error(message);
  }
  return response.json();
}

function toast(message) {
  const element = $('#toast');
  element.textContent = message;
  element.classList.add('visible');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => element.classList.remove('visible'), 3200);
}

function setBusy(form, busy) {
  form.querySelectorAll('button, input, textarea').forEach(element => {
    element.disabled = busy;
  });
}

function feedback(message, type = 'error') {
  return `<div class="feedback ${type}">${escapeHtml(message)}</div>`;
}

function thumbnail(url, title) {
  return url
    ? `<img class="source-thumbnail" src="${escapeHtml(url)}" alt="" loading="lazy">`
    : `<div class="source-thumbnail fallback" aria-hidden="true">${escapeHtml((title || 'M').slice(0, 1).toUpperCase())}</div>`;
}

async function checkHealth() {
  try {
    const status = await api('/api/system/status');
    if (status.backend !== 'UP' || !status.aiReady) throw new Error();
    $('#service-state').classList.add('ready');
    $('#service-state span:last-child').textContent = 'Backend + AI ready';
  } catch (error) {
    $('#service-state').classList.remove('ready');
    $('#service-state span:last-child').textContent = 'AI setup needs attention';
  }
}

function parseUrls(value) {
  return [...new Set(
    value.split(/\s+/)
      .map(item => item.trim())
      .filter(item => /^https?:\/\//i.test(item))
  )].slice(0, 20);
}

function renderCollection(collection) {
  const sourceRows = collection.sources.map(source => `
    <div class="source-row">
      <span class="status ${source.status.toLowerCase()}">${escapeHtml(source.status)}</span>
      ${escapeHtml(source.url)}
      ${source.errorMessage ? `<br><span class="error-copy">${escapeHtml(source.errorMessage)}</span>` : ''}
    </div>
  `).join('');
  return `
    <article class="collection-card">
      <h3>${escapeHtml(collection.name)}</h3>
      <div class="collection-meta">${collection.processedSources} memories created · ${collection.failedSources} failed · ${new Date(collection.createdAt).toLocaleString()}</div>
      <div class="source-list">${sourceRows}</div>
    </article>
  `;
}

async function loadCollections() {
  const container = $('#collections');
  try {
    const collections = await api(`/api/collections?userId=${encodeURIComponent(state.userId)}`);
    container.classList.toggle('empty-state', collections.length === 0);
    container.innerHTML = collections.length
      ? collections.map(renderCollection).join('')
      : '<p>No collections yet. Add your first links to begin.</p>';
  } catch (error) {
    container.innerHTML = feedback(error.message);
  }
}

$('#collection-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const urls = parseUrls($('#source-urls').value);
  if (!urls.length) {
    $('#capture-result').innerHTML = feedback('Add at least one valid HTTP or HTTPS URL.');
    return;
  }
  setBusy(form, true);
  $('#capture-result').innerHTML = feedback(`Processing ${urls.length} shared source${urls.length === 1 ? '' : 's'}…`, 'success');
  try {
    const collection = await api('/api/collections', {
      method: 'POST',
      body: JSON.stringify({
        userId: state.userId,
        name: $('#collection-name').value.trim(),
        description: $('#collection-description').value.trim() || null,
        sources: urls.map(url => ({ url }))
      })
    });
    $('#capture-result').innerHTML = feedback(
      `${collection.processedSources} memories created; ${collection.failedSources} sources need attention.`,
      collection.failedSources ? 'error' : 'success'
    );
    $('#source-urls').value = '';
    await loadCollections();
    toast('Collection processing finished');
  } catch (error) {
    $('#capture-result').innerHTML = feedback(error.message);
  } finally {
    setBusy(form, false);
  }
});

$('#search-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const target = $('#search-result');
  setBusy(form, true);
  target.innerHTML = feedback('Retrieving the best matching memories…', 'success');
  try {
    const memories = await api('/api/memories/search', {
      method: 'POST',
      body: JSON.stringify({
        userId: state.userId,
        query: $('#search-query').value.trim(),
        limit: 8
      })
    });
    target.innerHTML = memories.length
      ? memories.map(memory => `
          <article class="memory-result">
            <div class="memory-result-layout">
              ${thumbnail(memory.thumbnailUrl, memory.title)}
              <div>
                <h4>${escapeHtml(memory.title)}</h4>
                <div>${escapeHtml(memory.summary)}</div>
                <div class="score-line">
                  <span>Match ${(memory.score * 100).toFixed(0)}%</span>
                  <span>${escapeHtml(memory.category)}</span>
                  <a href="${escapeHtml(memory.sourceUrl)}" target="_blank" rel="noopener noreferrer">Open source ↗</a>
                </div>
              </div>
            </div>
          </article>
        `).join('')
      : feedback('No memories matched this query.');
  } catch (error) {
    target.innerHTML = feedback(error.message);
  } finally {
    setBusy(form, false);
  }
});

$('#plan-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const target = $('#plan-result');
  setBusy(form, true);
  target.innerHTML = feedback('Retrieving memory and building your plan…', 'success');
  try {
    const result = await api('/api/impulse/plan', {
      method: 'POST',
      body: JSON.stringify({
        userId: state.userId,
        query: $('#plan-query').value.trim(),
        constraints: {}
      })
    });
    const memoryById = Object.fromEntries(
      (result.groundingMemories || []).map(memory => [memory.id, memory])
    );
    target.innerHTML = `
      <section class="plan-hero">
        <p class="section-kicker">Your personalized route</p>
        <h3>${escapeHtml(result.goal)}</h3>
        <p>${escapeHtml(result.explanation)}</p>
        <div class="plan-progress">
          <span><strong id="completed-count">0</strong> / ${result.plan.length} complete</span>
          <div class="progress-track"><span id="progress-fill"></span></div>
        </div>
      </section>
      ${result.plan.map((step, index) => `
        <article class="plan-step interactive">
          <button class="step-check" type="button" aria-label="Mark step ${index + 1} complete" aria-pressed="false">${index + 1}</button>
          <div class="step-body">
            <strong>${escapeHtml(step.step)}</strong>
            <div class="step-meta">
              ${step.durationMinutes ? `<span>${step.durationMinutes} min</span>` : ''}
              <span>${step.memoryIds.length} memory source${step.memoryIds.length === 1 ? '' : 's'}</span>
            </div>
            ${step.reason ? `<details><summary>Why this step?</summary><p>${escapeHtml(step.reason)}</p></details>` : ''}
            <div class="step-sources">
              ${step.memoryIds.map(id => {
                const memory = memoryById[id];
                return memory ? `<a href="${escapeHtml(memory.sourceUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(memory.title)} ↗</a>` : '';
              }).join('')}
            </div>
          </div>
        </article>
      `).join('')}
      <section class="grounding-section">
        <div class="grounding-heading">
          <h3>Memories behind this plan</h3>
          <span>${result.groundingMemories?.length || 0} retrieved</span>
        </div>
        <div class="source-card-grid">
          ${(result.groundingMemories || []).map(memory => `
            <a class="source-card" href="${escapeHtml(memory.sourceUrl)}" target="_blank" rel="noopener noreferrer">
              ${thumbnail(memory.thumbnailUrl, memory.title)}
              <span class="source-card-copy">
                <small>${escapeHtml(memory.platform.replaceAll('_', ' '))}</small>
                <strong>${escapeHtml(memory.title)}</strong>
                <span>${escapeHtml(memory.summary)}</span>
              </span>
              <b aria-hidden="true">↗</b>
            </a>
          `).join('')}
        </div>
      </section>
    `;
    const checks = [...target.querySelectorAll('.step-check')];
    const updateProgress = () => {
      const complete = checks.filter(button => button.getAttribute('aria-pressed') === 'true').length;
      $('#completed-count').textContent = complete;
      $('#progress-fill').style.width = `${result.plan.length ? complete / result.plan.length * 100 : 0}%`;
    };
    checks.forEach(button => button.addEventListener('click', () => {
      const pressed = button.getAttribute('aria-pressed') === 'true';
      button.setAttribute('aria-pressed', String(!pressed));
      button.closest('.plan-step').classList.toggle('complete', !pressed);
      updateProgress();
    }));
  } catch (error) {
    target.innerHTML = feedback(error.message);
  } finally {
    setBusy(form, false);
  }
});

$('#new-user').addEventListener('click', () => {
  setUserId(crypto.randomUUID());
  loadCollections();
  $('#capture-result').innerHTML = '';
  $('#search-result').innerHTML = '';
  $('#plan-result').innerHTML = '';
  toast('Created a fresh test user');
});

$('#user-id').addEventListener('change', event => {
  const value = event.target.value.trim();
  if (/^[0-9a-f-]{36}$/i.test(value)) {
    setUserId(value);
    loadCollections();
  } else {
    event.target.value = state.userId;
    toast('Enter a valid UUID');
  }
});

$('#refresh-collections').addEventListener('click', loadCollections);

setUserId(state.userId);
checkHealth();
loadCollections();
