/**
 * Impulse Extension - Background Service Worker (Manifest V3)
 * Handles extension lifecycle, context menu, background offline queue syncing, and IPC messaging.
 */

import { extractMetadata } from '../utils/extractors.js';
import { saveContent, syncPendingQueue, getPendingQueue } from '../utils/api.js';

const CONTEXT_MENU_ID = 'impulse_save_to_brain';

/**
 * Initialize background listeners and extension context menu
 */
chrome.runtime.onInstalled.addListener(async (details) => {
  console.log('[Impulse Background] Extension lifecycle event:', details.reason);
  
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: CONTEXT_MENU_ID,
      title: 'Save to Impulse',
      contexts: ['page', 'link', 'selection']
    });
  });

  // Setup periodic sync alarm (runs every 5 minutes)
  chrome.alarms.create('syncPendingQueue', { periodInMinutes: 5 });

  // Initial sync attempt if queue has pending items
  try {
    const queue = await getPendingQueue();
    if (queue.length > 0) {
      await syncPendingQueue();
    }
  } catch (e) {
    console.warn('[Impulse Background] Initial queue sync check error:', e);
  }
});

/**
 * Context Menu Click Listener
 */
chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== CONTEXT_MENU_ID) return;

  const targetUrl = info.linkUrl || info.pageUrl || tab?.url;
  if (!targetUrl) return;

  const selectionText = info.selectionText ? info.selectionText.trim() : '';
  const pageTitle = tab?.title || '';

  const metadata = extractMetadata(targetUrl, {
    title: pageTitle,
    userNote: selectionText ? `Selection: "${selectionText}"` : ''
  });

  try {
    if (metadata && ['youtube', 'instagram'].includes(metadata.platform)) {
      const result = await saveContent(metadata);
      updateBadge(result.success, result.queued);
    } else {
      updateBadge(false, false);
    }
  } catch (error) {
    console.error('[Impulse Background] Context-menu save failed:', error);
    updateBadge(false, false);
  }
});

/**
 * Alarm Listener for background queue sync
 */
chrome.alarms.onAlarm.addListener(async (alarm) => {
  if (alarm.name === 'syncPendingQueue') {
    const result = await syncPendingQueue();
    console.log('[Impulse Background] Periodic queue sync result:', result);
  }
});

/**
 * Visual badge feedback helper
 */
function updateBadge(success, queued) {
  if (queued) {
    chrome.action.setBadgeText({ text: 'Q' });
    chrome.action.setBadgeBackgroundColor({ color: '#f59e0b' });
  } else if (success) {
    chrome.action.setBadgeText({ text: '✓' });
    chrome.action.setBadgeBackgroundColor({ color: '#10b981' });
  } else {
    chrome.action.setBadgeText({ text: '!' });
    chrome.action.setBadgeBackgroundColor({ color: '#ef4444' });
  }

  setTimeout(() => {
    chrome.action.setBadgeText({ text: '' });
  }, 3000);
}

/**
 * IPC Message Dispatcher
 */
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'SAVE_CONTENT') {
    saveContent(request.payload).then(res => {
      updateBadge(res.success, res.queued);
      sendResponse(res);
    });
    return true;
  }

  if (request.action === 'SAVE_BATCH_CONTENTS') {
    (async () => {
      const items = request.items || [];
      const results = [];
      let successCount = 0;
      let queuedCount = 0;

      for (const item of items) {
        const res = await saveContent(item);
        results.push(res);
        if (res.success) successCount++;
        if (res.queued) queuedCount++;
      }

      sendResponse({
        total: items.length,
        successCount,
        queuedCount,
        results
      });
    })();
    return true;
  }

  if (request.action === 'SYNC_QUEUE') {
    syncPendingQueue().then(sendResponse);
    return true;
  }

  if (request.action === 'GET_PENDING_QUEUE') {
    getPendingQueue().then(queue => sendResponse({ queue }));
    return true;
  }

  if (request.action === 'GET_ACTIVE_TAB_METADATA') {
    (async () => {
      try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (!tab || !tab.id) {
          sendResponse({ error: 'No active tab found' });
          return;
        }

        try {
          const response = await chrome.tabs.sendMessage(tab.id, { action: 'EXTRACT_TAB_METADATA' });
          if (response) {
            sendResponse({ metadata: response, tab });
            return;
          }
        } catch (e) {
          // Fall through to tab-based extraction or scripting injection
        }

        // Inject content scripts dynamically if message failed
        try {
          await chrome.scripting.executeScript({
            target: { tabId: tab.id },
            files: ['scripts/content.js']
          });
          const response = await chrome.tabs.sendMessage(tab.id, { action: 'EXTRACT_TAB_METADATA' });
          if (response) {
            sendResponse({ metadata: response, tab });
            return;
          }
        } catch (injectErr) {
          console.warn('[Impulse Background] Dynamic script injection fallback:', injectErr);
        }

        // Final fallback: metadata from tab object
        const fallbackMeta = extractMetadata(tab.url, { title: tab.title });
        sendResponse({ metadata: fallbackMeta, tab });

      } catch (err) {
        sendResponse({ error: err.message });
      }
    })();
    return true;
  }

  if (request.action === 'SCAN_ACTIVE_TAB_LINKS') {
    (async () => {
      try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (!tab || !tab.id) {
          sendResponse({ links: [] });
          return;
        }

        try {
          const links = await chrome.tabs.sendMessage(tab.id, { action: 'SCAN_PAGE_LINKS' });
          sendResponse({ links: links || [] });
        } catch (err) {
          try {
            await chrome.scripting.executeScript({
              target: { tabId: tab.id },
              files: ['scripts/content.js']
            });
            const links = await chrome.tabs.sendMessage(tab.id, { action: 'SCAN_PAGE_LINKS' });
            sendResponse({ links: links || [] });
          } catch (injectErr) {
            console.warn('[Impulse Background] Link scan failed:', injectErr);
            sendResponse({ links: [], error: injectErr.message });
          }
        }
      } catch (err) {
        sendResponse({ links: [], error: err.message });
      }
    })();
    return true;
  }
});
