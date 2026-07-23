/**
 * Impulse Extension - Popup UI Script
 * Handles UI tab switching, tab metadata display, quick saving, batch link selection, and settings.
 */

import { extractMetadata } from '../utils/extractors.js';
import { 
  getBackendUrl, 
  setBackendUrl, 
  getPlanBackendUrl,
  setPlanBackendUrl,
  getUserId,
  setUserId,
  saveContent, 
  getPendingQueue, 
  syncPendingQueue, 
  clearPendingQueue,
  getSavedLibrary,
  deleteFromSavedLibrary,
  clearSavedLibrary
} from '../utils/api.js';

document.addEventListener('DOMContentLoaded', async () => {
  // DOM Handles
  const tabSaveBtn = document.getElementById('tab-save-btn');
  const tabQueryBtn = document.getElementById('tab-query-btn');
  const tabLibraryBtn = document.getElementById('tab-library-btn');
  const settingsToggleBtn = document.getElementById('settings-toggle-btn');
  const queueBadgeBtn = document.getElementById('queue-badge-btn');
  const queueCountEl = document.getElementById('queue-count');

  const viewSave = document.getElementById('view-save');
  const viewQuery = document.getElementById('view-query');
  const viewLibrary = document.getElementById('view-library');
  const viewSettings = document.getElementById('view-settings');

  // Library UI
  const librarySearch = document.getElementById('library-search');
  const libraryItemsContainer = document.getElementById('library-items-container');
  const libraryEmpty = document.getElementById('library-empty');
  const libraryQueueCount = document.getElementById('library-queue-count');
  const libraryQueueItems = document.getElementById('library-queue-items');
  const countAll = document.getElementById('count-all');
  const countYt = document.getElementById('count-yt');
  const countIg = document.getElementById('count-ig');
  const countWeb = document.getElementById('count-web');
  const filterPills = document.querySelectorAll('.filter-pill');

  const previewLoading = document.getElementById('preview-loading');
  const previewCard = document.getElementById('preview-card');
  const platformBadge = document.getElementById('platform-badge');
  const platformIcon = document.getElementById('platform-icon');
  const platformName = document.getElementById('platform-name');
  const contentTypeBadge = document.getElementById('content-type-badge');
  const contentTypeName = document.getElementById('content-type-name');

  const thumbnailImg = document.getElementById('thumbnail-img');
  const thumbnailFallback = document.getElementById('thumbnail-fallback');

  const inputTitle = document.getElementById('input-title');
  const inputUrl = document.getElementById('input-url');
  const inputNote = document.getElementById('input-note');
  const inputTags = document.getElementById('input-tags');
  const btnSave = document.getElementById('btn-save');
  const unsupportedPageMessage = document.getElementById('unsupported-page-message');

  // Query & Plan UI
  const queryBackendUrl = document.getElementById('query-backend-url');
  const queryPrompt = document.getElementById('query-prompt');
  const btnGeneratePlan = document.getElementById('btn-generate-plan');
  const planResults = document.getElementById('plan-results');
  const planEmpty = document.getElementById('plan-empty');

  // Settings UI
  const inputApiUrl = document.getElementById('input-api-url');
  const inputUserId = document.getElementById('input-user-id');
  const btnSaveSettings = document.getElementById('btn-save-settings');
  const settingsQueueCount = document.getElementById('settings-queue-count');
  const btnSyncQueue = document.getElementById('btn-sync-queue');
  const btnClearQueue = document.getElementById('btn-clear-queue');

  // Toast
  const toast = document.getElementById('toast');
  const toastMessage = document.getElementById('toast-message');

  // State Variables
  let currentMetadata = null;
  let libraryItems = [];
  let currentFilter = 'all';

  // --- Initial Setup ---
  await refreshQueueBadge();
  await loadCurrentTabMetadata();
  await loadSettings();
  await loadPlanBackendUrl();

  // --- Navigation View Switcher ---
  function switchView(targetView) {
    const views = [viewSave, viewQuery, viewLibrary, viewSettings];
    views.forEach(v => {
      if (!v) return;
      v.classList.add('hidden');
      v.classList.remove('active');
    });
    [tabSaveBtn, tabQueryBtn, tabLibraryBtn].forEach(t => t && t.classList.remove('active'));

    if (targetView === 'save') {
      viewSave.classList.remove('hidden');
      viewSave.classList.add('active');
      tabSaveBtn.classList.add('active');
    } else if (targetView === 'query') {
      viewQuery.classList.remove('hidden');
      viewQuery.classList.add('active');
      tabQueryBtn.classList.add('active');
    } else if (targetView === 'library') {
      viewLibrary.classList.remove('hidden');
      viewLibrary.classList.add('active');
      tabLibraryBtn.classList.add('active');
      loadAndRenderLibrary();
    } else if (targetView === 'settings') {
      viewSettings.classList.remove('hidden');
      viewSettings.classList.add('active');
      refreshSettingsQueueCount();
    }
  }

  tabSaveBtn.addEventListener('click', () => switchView('save'));
  tabQueryBtn.addEventListener('click', () => switchView('query'));
  if (tabLibraryBtn) {
    tabLibraryBtn.addEventListener('click', () => switchView('library'));
  }
  settingsToggleBtn.addEventListener('click', () => switchView('settings'));
  queueBadgeBtn.addEventListener('click', () => switchView('settings'));

  // --- Notification Toast Helper ---
  function showToast(message, type = 'success') {
    toastMessage.textContent = message;
    toast.className = `toast toast-${type}`;
    toast.classList.remove('hidden');
    setTimeout(() => {
      toast.classList.add('hidden');
    }, 3500);
  }

  // --- Offline Queue Badge Status ---
  async function refreshQueueBadge() {
    try {
      const queue = await getPendingQueue();
      const count = queue.length;
      if (count > 0) {
        if (queueCountEl) queueCountEl.textContent = count;
        if (queueBadgeBtn) queueBadgeBtn.classList.remove('hidden');
        if (settingsQueueCount) settingsQueueCount.textContent = count;
      } else {
        if (queueCountEl) queueCountEl.textContent = '0';
        if (queueBadgeBtn) queueBadgeBtn.classList.add('hidden');
        if (settingsQueueCount) settingsQueueCount.textContent = '0';
      }
    } catch (e) {
      console.warn('Queue badge refresh error:', e);
    }
  }

  async function refreshSettingsQueueCount() {
    const queue = await getPendingQueue();
    if (settingsQueueCount) settingsQueueCount.textContent = queue.length;

    const listEl = document.getElementById('pending-items-list');
    if (listEl) {
      listEl.innerHTML = '';
      if (queue.length > 0) {
        queue.forEach(item => {
          const div = document.createElement('div');
          div.className = 'pending-item';
          div.innerHTML = `
            <div class="pending-item-title">${escapeHtml(item.title || 'Untitled')}</div>
            <div class="pending-item-url">${escapeHtml(item.url)}</div>
          `;
          listEl.appendChild(div);
        });
      }
    }
  }

  // --- Extract & Render Active Tab Metadata ---
  async function loadCurrentTabMetadata() {
    previewLoading.classList.remove('hidden');
    previewCard.classList.add('hidden');

    try {
      let meta = null;

      if (typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.sendMessage) {
        const response = await new Promise((resolve) => {
          chrome.runtime.sendMessage({ action: 'GET_ACTIVE_TAB_METADATA' }, resolve);
        });

        if (response && response.metadata) {
          meta = response.metadata;
        }
      }

      if (!meta && typeof chrome !== 'undefined' && chrome.tabs) {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tab && tab.url) {
          meta = extractMetadata(tab.url, { title: tab.title });
        }
      }

      if (meta) {
        renderMetadataPreview(meta);
      } else {
        renderUnsupportedPage('No YouTube or Instagram links found on this page.');
      }
    } catch (err) {
      console.error('Error loading tab metadata:', err);
      renderUnsupportedPage('This page cannot be read. No YouTube or Instagram links found.');
      showToast('Could not inspect the active page.', 'error');
    } finally {
      previewLoading.classList.add('hidden');
      previewCard.classList.remove('hidden');
    }
  }

  function renderMetadataPreview(meta) {
    currentMetadata = meta;
    const isSupportedPage = isYouTubeOrInstagramUrl(meta.url);
    inputTitle.value = meta.title || '';
    inputUrl.value = meta.url || '';
    inputNote.value = meta.userNote || '';
    inputTags.value = Array.isArray(meta.tags) ? meta.tags.join(', ') : '';
    const saveButtonText = btnSave.querySelector('.btn-text');
    if (saveButtonText) {
      saveButtonText.textContent = isSupportedPage
        ? 'Save Links Found on Page'
        : 'No Supported Links Found';
    }
    btnSave.disabled = !isSupportedPage;
    unsupportedPageMessage.classList.toggle('hidden', isSupportedPage);

    // Render Platform Badge
    platformBadge.className = 'badge';
    if (meta.platform === 'youtube') {
      platformBadge.classList.add('badge-youtube');
      platformIcon.textContent = '▶';
      platformName.textContent = 'YOUTUBE';
    } else if (meta.platform === 'instagram') {
      platformBadge.classList.add('badge-instagram');
      platformIcon.textContent = '📷';
      platformName.textContent = 'INSTAGRAM';
    } else {
      platformBadge.classList.add('badge-web');
      platformIcon.textContent = '🌐';
      platformName.textContent = 'WEB';
    }

    // Render Content Type Badge
    contentTypeName.textContent = (meta.contentType || 'ARTICLE').toUpperCase();

    // Render Thumbnail Preview
    if (meta.thumbnailUrl) {
      thumbnailImg.src = meta.thumbnailUrl;
      thumbnailImg.classList.remove('hidden');
      thumbnailFallback.classList.add('hidden');

      thumbnailImg.onerror = () => {
        thumbnailImg.classList.add('hidden');
        thumbnailFallback.classList.remove('hidden');
      };
    } else {
      thumbnailImg.classList.add('hidden');
      thumbnailFallback.classList.remove('hidden');
    }
  }

  function renderUnsupportedPage(message) {
    currentMetadata = null;
    inputTitle.value = '';
    inputUrl.value = '';
    inputNote.value = '';
    inputTags.value = '';
    btnSave.disabled = true;
    btnSave.querySelector('.btn-text').textContent = 'No Supported Links Found';
    unsupportedPageMessage.textContent = message;
    unsupportedPageMessage.classList.remove('hidden');
    platformBadge.className = 'badge badge-web';
    platformIcon.textContent = '🌐';
    platformName.textContent = 'UNSUPPORTED';
    contentTypeName.textContent = 'NO LINKS';
    thumbnailImg.classList.add('hidden');
    thumbnailFallback.classList.remove('hidden');
  }

  // --- Save Content Action ---
  btnSave.addEventListener('click', async () => {
    if (!currentMetadata || !inputUrl.value) return;

    const tagsArray = inputTags.value
      .split(',')
      .map(t => t.trim())
      .filter(t => t.length > 0);

    const payload = {
      ...currentMetadata,
      title: inputTitle.value.trim() || currentMetadata.title,
      url: inputUrl.value,
      userNote: inputNote.value.trim(),
      tags: tagsArray,
      extractedAt: new Date().toISOString()
    };

    setButtonLoading(btnSave, true);

    try {
      // Social pages are collections: save the Reel/video URLs found in the DOM,
      // rather than saving the collection, profile, or playlist page URL.
      const isSocialPage = isYouTubeOrInstagramUrl(payload.url);
      const pageLinks = isSocialPage ? await getActiveTabLinks() : [];
      if (isSocialPage && pageLinks.length === 0 && !['youtube', 'instagram'].includes(currentMetadata.platform)) {
        showToast('No Reel or video URLs found. Scroll the page and try again.', 'warning');
        return;
      }

      const itemsToSave = pageLinks.length > 0 ? pageLinks : [payload];

      for (const item of itemsToSave) {
        await saveContent({
          ...item,
          userNote: inputNote.value.trim(),
          tags: tagsArray,
          extractedAt: new Date().toISOString()
        });
      }

      const message = pageLinks.length > 0
        ? `Saved ${pageLinks.length} Reel/video URLs!`
        : 'Link extracted & saved!';
      showToast(message, 'success');
      await refreshQueueBadge();
      switchView('library');
    } catch (err) {
      console.error('Save content error:', err);
      showToast(`Error: ${err.message}`, 'error');
    } finally {
      setButtonLoading(btnSave, false);
    }
  });

  function setButtonLoading(button, isLoading) {
    const btnText = button.querySelector('.btn-text');
    const btnSpinner = button.querySelector('.btn-spinner');
    if (isLoading) {
      button.disabled = true;
      if (btnText) btnText.style.opacity = '0.5';
      if (btnSpinner) btnSpinner.classList.remove('hidden');
    } else {
      button.disabled = false;
      if (btnText) btnText.style.opacity = '1';
      if (btnSpinner) btnSpinner.classList.add('hidden');
    }
  }

  async function getActiveTabLinks() {
    if (!(typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.sendMessage)) {
      return [];
    }

    const response = await new Promise(resolve => {
      chrome.runtime.sendMessage({ action: 'SCAN_ACTIVE_TAB_LINKS' }, message => {
        const runtimeError = chrome.runtime.lastError;
        if (runtimeError) {
          resolve({ error: runtimeError.message });
          return;
        }
        resolve(message);
      });
    });
    if (response?.error) throw new Error(response.error);
    return Array.isArray(response?.links) ? response.links : [];
  }

  function isYouTubeOrInstagramUrl(url) {
    try {
      const hostname = new URL(url).hostname.toLowerCase();
      return hostname === 'youtube.com' || hostname.endsWith('.youtube.com') ||
        hostname === 'youtu.be' || hostname === 'instagram.com' || hostname.endsWith('.instagram.com');
    } catch (error) {
      return false;
    }
  }

  // --- Local Query & Plan ---
  btnGeneratePlan.addEventListener('click', async () => {
    const prompt = queryPrompt.value.trim();
    const backendUrl = queryBackendUrl.value.trim();

    if (!prompt) {
      showToast('Enter a planning prompt first.', 'warning');
      return;
    }

    try {
      const savedItems = await getSavedLibrary();
      await setPlanBackendUrl(backendUrl);

      if (backendUrl) {
        await requestBackendPlan(backendUrl, prompt, savedItems);
      } else {
        renderLocalPlan(prompt, savedItems);
      }
    } catch (error) {
      console.error('Plan setup error:', error);
      showToast('Could not load saved links for planning.', 'error');
    }
  });

  async function requestBackendPlan(backendUrl, prompt, savedItems) {
    btnGeneratePlan.disabled = true;
    btnGeneratePlan.textContent = 'Creating plan...';

    try {
      const response = await fetch(backendUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({
          userId: await getUserId(),
          query: prompt,
          constraints: {}
        })
      });
      if (!response.ok) {
        let message = `HTTP ${response.status}`;
        try {
          const errorBody = await response.json();
          if (errorBody.message || errorBody.detail) message = errorBody.message || errorBody.detail;
        } catch (ignored) {}
        throw new Error(message);
      }

      const result = await response.json();
      renderBackendPlan(prompt, result);
      showToast('Plan created by backend.', 'success');
    } catch (error) {
      console.error('Backend planning error:', error);
      showToast(`Backend unavailable (${error.message}). Showing a local plan instead.`, 'warning');
      renderLocalPlan(prompt, savedItems);
    } finally {
      btnGeneratePlan.disabled = false;
      btnGeneratePlan.textContent = 'Create Plan';
    }
  }

  function renderBackendPlan(prompt, result) {
    planResults.innerHTML = '';
    planEmpty.classList.add('hidden');
    const steps = Array.isArray(result.plan) ? result.plan : [];
    const planItems = steps.map(step => `
      <li>
        ${escapeHtml(step.step || String(step))}
        ${step.durationMinutes ? `<span class="plan-url"> · ${Number(step.durationMinutes)} min</span>` : ''}
        ${step.reason ? `<br><span class="plan-url">${escapeHtml(step.reason)}</span>` : ''}
      </li>
    `).join('');
    const retrievedCount = Array.isArray(result.retrievedMemoryIds)
      ? result.retrievedMemoryIds.length
      : 0;

    planResults.innerHTML = `
      <div class="card plan-card">
        <h3 class="section-title">Backend plan</h3>
        <p class="plan-prompt">${escapeHtml(prompt)}</p>
        ${result.explanation ? `<p class="plan-target">${escapeHtml(result.explanation)}</p>` : ''}
        ${retrievedCount ? `<p class="plan-url">Grounded in ${retrievedCount} saved memories.</p>` : ''}
        <ol class="plan-steps">${planItems || '<li>The backend returned no plan steps.</li>'}</ol>
      </div>
    `;
    planResults.classList.remove('hidden');
  }

  function renderLocalPlan(prompt, savedItems) {
    planResults.innerHTML = '';
    planEmpty.classList.add('hidden');

    if (savedItems.length === 0) {
      planResults.classList.add('hidden');
      planEmpty.classList.remove('hidden');
      return;
    }

    const terms = prompt.toLowerCase().split(/[^a-z0-9]+/).filter(term => term.length > 2);
    const scoredItems = savedItems
      .map(item => {
        const searchable = [item.title, item.url, item.userNote, ...(item.tags || [])].join(' ').toLowerCase();
        const score = terms.reduce((total, term) => total + (searchable.includes(term) ? 1 : 0), 0);
        return { item, score };
      })
      .sort((a, b) => b.score - a.score)
      .slice(0, 5)
      .map(entry => entry.item);

    const steps = scoredItems.map((item, index) => `
      <li><strong>${index === 0 ? 'Start with' : 'Use'}:</strong> ${escapeHtml(item.title || 'Saved link')}<br>
      <span class="plan-url">${escapeHtml(item.url)}</span></li>
    `).join('');

    planResults.innerHTML = `
      <div class="card plan-card">
        <h3 class="section-title">Local plan</h3>
        <p class="plan-prompt">${escapeHtml(prompt)}</p>
        <ol class="plan-steps">
          <li><strong>Define the outcome:</strong> ${escapeHtml(prompt)}</li>
          ${steps}
          <li><strong>Review and choose:</strong> compare the saved links above, then turn the best option into your next action.</li>
        </ol>
      </div>
    `;
    planResults.classList.remove('hidden');
  }

  // --- Settings & Queue Management ---
  async function loadSettings() {
    const [currentUrl, userId] = await Promise.all([getBackendUrl(), getUserId()]);
    inputApiUrl.value = currentUrl;
    if (inputUserId) inputUserId.value = userId;
  }

  async function loadPlanBackendUrl() {
    queryBackendUrl.value = await getPlanBackendUrl();
  }

  btnSaveSettings.addEventListener('click', async () => {
    const newUrl = inputApiUrl.value.trim();
    if (!newUrl) {
      showToast('Please enter a valid endpoint URL.', 'error');
      return;
    }

    await setBackendUrl(newUrl);
    if (inputUserId) await setUserId(inputUserId.value);
    showToast('Backend settings updated!', 'success');
  });

  btnSyncQueue.addEventListener('click', async () => {
    btnSyncQueue.disabled = true;
    btnSyncQueue.textContent = 'Syncing...';

    try {
      let result;
      if (typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.sendMessage) {
        result = await new Promise(resolve => {
          chrome.runtime.sendMessage({ action: 'SYNC_QUEUE' }, resolve);
        });
      } else {
        result = await syncPendingQueue();
      }

      if (result && result.syncedCount > 0) {
        showToast(`Successfully synced ${result.syncedCount} items!`, 'success');
      } else if (result && result.remainingCount > 0) {
        showToast(`Synced ${result.syncedCount || 0} items. ${result.remainingCount} remaining in queue.`, 'warning');
      } else {
        showToast('Queue is empty or backend is unreachable.', 'warning');
      }

      await refreshQueueBadge();
      await refreshSettingsQueueCount();
    } catch (err) {
      showToast(`Sync failed: ${err.message}`, 'error');
    } finally {
      btnSyncQueue.disabled = false;
      btnSyncQueue.textContent = 'Sync Queue Now';
    }
  });

  btnClearQueue.addEventListener('click', async () => {
    await clearQueue();
  });

  const btnClearAllStorage = document.getElementById('btn-clear-all-storage');
  if (btnClearAllStorage) {
    btnClearAllStorage.addEventListener('click', async () => {
      await clearAllLocalData();
    });
  }

  const btnClearLibrary = document.getElementById('btn-clear-library');
  if (btnClearLibrary) {
    btnClearLibrary.addEventListener('click', async () => {
      await clearLibrary();
    });
  }

  // Clear queue from header trash icon
  const btnClearQueueHeader = document.getElementById('btn-clear-queue-header');
  if (btnClearQueueHeader) {
    btnClearQueueHeader.addEventListener('click', async () => {
      await clearQueue();
    });
  }

  // Clear queue button inside Saved Links footer
  const btnClearQueueLibrary = document.getElementById('btn-clear-queue-library');
  if (btnClearQueueLibrary) {
    btnClearQueueLibrary.addEventListener('click', async () => {
      await clearQueue();
    });
  }

  // Clear all data button inside Saved Links footer
  const btnClearAllLibrary = document.getElementById('btn-clear-all-library');
  if (btnClearAllLibrary) {
    btnClearAllLibrary.addEventListener('click', async () => {
      await clearAllLocalData();
    });
  }


  // --- Saved Links Library Functions ---
  async function loadAndRenderLibrary() {
    libraryItems = await getSavedLibrary();
    updateFilterCounts();
    renderLibraryItems();
    await renderLibraryQueue();
  }

  async function renderLibraryQueue() {
    const queue = await getPendingQueue();
    if (libraryQueueCount) libraryQueueCount.textContent = queue.length;
    if (!libraryQueueItems) return;

    libraryQueueItems.innerHTML = '';
    if (queue.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'queue-empty-message';
      empty.textContent = 'No links are waiting to sync.';
      libraryQueueItems.appendChild(empty);
      return;
    }

    queue.forEach(item => {
      const row = document.createElement('div');
      row.className = 'pending-item';
      row.innerHTML = `
        <div class="pending-item-title">${escapeHtml(item.title || 'Untitled Link')}</div>
        <div class="pending-item-url">${escapeHtml(item.url || '')}</div>
      `;
      libraryQueueItems.appendChild(row);
    });
  }

  async function clearQueue() {
    try {
      await clearPendingQueue();
      await refreshQueueBadge();
      await refreshSettingsQueueCount();
      await renderLibraryQueue();
      showToast('Pending sync queue cleared.', 'success');
    } catch (err) {
      console.error('Clear pending queue error:', err);
      showToast('Could not clear the pending queue.', 'error');
    }
  }

  async function clearLibrary() {
    try {
      await clearSavedLibrary();
      await loadAndRenderLibrary();
      showToast('Saved links cleared.', 'success');
    } catch (err) {
      console.error('Clear saved library error:', err);
      showToast('Could not clear saved links.', 'error');
    }
  }

  async function clearAllLocalData() {
    try {
      await Promise.all([clearPendingQueue(), clearSavedLibrary()]);
      await refreshQueueBadge();
      await refreshSettingsQueueCount();
      await loadAndRenderLibrary();
      showToast('All local data cleared.', 'success');
    } catch (err) {
      console.error('Clear all local data error:', err);
      showToast('Could not clear local data.', 'error');
    }
  }

  function updateFilterCounts() {
    if (countAll) countAll.textContent = libraryItems.length;
    if (countYt) countYt.textContent = libraryItems.filter(i => i.platform === 'youtube').length;
    if (countIg) countIg.textContent = libraryItems.filter(i => i.platform === 'instagram').length;
    if (countWeb) countWeb.textContent = libraryItems.filter(i => i.platform === 'web' || !i.platform).length;
  }

  function renderLibraryItems() {
    if (!libraryItemsContainer) return;
    libraryItemsContainer.innerHTML = '';
    const query = (librarySearch?.value || '').toLowerCase().trim();

    const filtered = libraryItems.filter(item => {
      if (currentFilter !== 'all' && item.platform !== currentFilter) return false;
      if (!query) return true;
      const matchTitle = (item.title || '').toLowerCase().includes(query);
      const matchUrl = (item.url || '').toLowerCase().includes(query);
      const matchNote = (item.userNote || '').toLowerCase().includes(query);
      const matchTags = Array.isArray(item.tags) && item.tags.some(t => t.toLowerCase().includes(query));
      return matchTitle || matchUrl || matchNote || matchTags;
    });

    if (filtered.length === 0) {
      if (libraryEmpty) libraryEmpty.classList.remove('hidden');
    } else {
      if (libraryEmpty) libraryEmpty.classList.add('hidden');
      filtered.forEach(item => {
        const card = document.createElement('div');
        card.className = 'library-card';

        const isYt = item.platform === 'youtube';
        const isIg = item.platform === 'instagram';
        const badgeClass = isYt ? 'badge-youtube' : isIg ? 'badge-instagram' : 'badge-web';
        const platformIconStr = isYt ? '▶' : isIg ? '📷' : '🌐';

        card.innerHTML = `
          <div class="library-card-header">
            <span class="badge ${badgeClass}">${platformIconStr} ${(item.platform || 'web').toUpperCase()}</span>
            <span class="badge badge-subtle">${(item.contentType || 'LINK').toUpperCase()}</span>
          </div>
          <div class="library-card-title" title="${escapeHtml(item.title)}">${escapeHtml(item.title || 'Untitled Link')}</div>
          <div class="library-card-url">${escapeHtml(item.url)}</div>
          ${item.userNote ? `<div class="library-card-notes">"${escapeHtml(item.userNote)}"</div>` : ''}
          <div class="library-card-actions">
            <span style="font-size: 10px; color: var(--text-muted);">${item.savedAt ? new Date(item.savedAt).toLocaleDateString() : ''}</span>
            <div class="card-action-group">
              <button class="card-action-btn copy-btn" data-url="${encodeURIComponent(item.url)}">📋 Copy</button>
              <button class="card-action-btn open-btn" data-url="${encodeURIComponent(item.url)}">↗ Open</button>
              <button class="card-action-btn delete-btn" data-url="${encodeURIComponent(item.url)}">🗑</button>
            </div>
          </div>
        `;

        card.querySelector('.copy-btn').addEventListener('click', async (e) => {
          const rawUrl = decodeURIComponent(e.currentTarget.dataset.url);
          await navigator.clipboard.writeText(rawUrl);
          showToast('URL copied to clipboard!', 'success');
        });

        card.querySelector('.open-btn').addEventListener('click', (e) => {
          const rawUrl = decodeURIComponent(e.currentTarget.dataset.url);
          if (typeof chrome !== 'undefined' && chrome.tabs) {
            chrome.tabs.create({ url: rawUrl });
          } else {
            window.open(rawUrl, '_blank');
          }
        });

        card.querySelector('.delete-btn').addEventListener('click', async (e) => {
          const rawUrl = decodeURIComponent(e.currentTarget.dataset.url);
          await deleteFromSavedLibrary(rawUrl);
          showToast('Link removed from library.', 'success');
          await loadAndRenderLibrary();
        });

        libraryItemsContainer.appendChild(card);
      });
    }
  }

  filterPills.forEach(pill => {
    pill.addEventListener('click', (e) => {
      filterPills.forEach(p => p.classList.remove('active'));
      e.currentTarget.classList.add('active');
      currentFilter = e.currentTarget.dataset.filter;
      renderLibraryItems();
    });
  });

  if (librarySearch) {
    librarySearch.addEventListener('input', () => renderLibraryItems());
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
});
