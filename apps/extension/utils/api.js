/**
 * Impulse Extension - API & Local Queue Storage Integration
 * Handles backend HTTP communication and chrome.storage.local offline queuing.
 */

export const DEFAULT_BACKEND_URL = 'http://localhost:8081';
const STORAGE_KEY_BACKEND_URL = 'impulse_backend_url';
const STORAGE_KEY_PLAN_BACKEND_URL = 'impulse_plan_backend_url';
const STORAGE_KEY_PENDING_QUEUE = 'pending_sync';
const STORAGE_KEY_USER_ID = 'impulse_user_id';

/**
 * Storage wrapper to handle Chrome Storage API or localStorage fallback.
 */
const storage = {
  async get(keys) {
    if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
      return new Promise((resolve) => chrome.storage.local.get(keys, resolve));
    }
    const result = {};
    const keyArray = Array.isArray(keys) ? keys : [keys];
    for (const key of keyArray) {
      const val = typeof localStorage !== 'undefined' ? localStorage.getItem(key) : null;
      if (val !== null) {
        try {
          result[key] = JSON.parse(val);
        } catch (e) {
          result[key] = val;
        }
      }
    }
    return result;
  },

  async set(items) {
    if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
      return new Promise((resolve) => chrome.storage.local.set(items, resolve));
    }
    if (typeof localStorage !== 'undefined') {
      for (const [key, val] of Object.entries(items)) {
        localStorage.setItem(key, typeof val === 'string' ? val : JSON.stringify(val));
      }
    }
  }
};

/**
 * Retrieves the currently configured Impulse backend endpoint URL.
 * @returns {Promise<string>}
 */
export async function getBackendUrl() {
  const result = await storage.get(STORAGE_KEY_BACKEND_URL);
  return normalizeBackendUrl(result[STORAGE_KEY_BACKEND_URL] || DEFAULT_BACKEND_URL);
}

/**
 * Updates the Impulse backend endpoint URL.
 * @param {string} url 
 * @returns {Promise<void>}
 */
export async function setBackendUrl(url) {
  const cleanUrl = normalizeBackendUrl(url);
  await storage.set({ [STORAGE_KEY_BACKEND_URL]: cleanUrl || DEFAULT_BACKEND_URL });
}

function normalizeBackendUrl(url) {
  return (url || '').trim().replace(/\/+$/, '')
    .replace(/\/contents$/, '')
    .replace(/\/api\/memories\/import$/, '');
}

export async function getUserId() {
  const result = await storage.get(STORAGE_KEY_USER_ID);
  if (result[STORAGE_KEY_USER_ID]) return result[STORAGE_KEY_USER_ID];
  const userId = crypto.randomUUID();
  await storage.set({ [STORAGE_KEY_USER_ID]: userId });
  return userId;
}

export async function setUserId(userId) {
  const value = (userId || '').trim();
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)) {
    throw new Error('User ID must be a valid UUID.');
  }
  await storage.set({ [STORAGE_KEY_USER_ID]: value });
}

/**
 * Gets the optional Impulse planning endpoint. An empty value keeps planning local.
 * @returns {Promise<string>}
 */
export async function getPlanBackendUrl() {
  const result = await storage.get(STORAGE_KEY_PLAN_BACKEND_URL);
  if (Object.prototype.hasOwnProperty.call(result, STORAGE_KEY_PLAN_BACKEND_URL)) {
    return result[STORAGE_KEY_PLAN_BACKEND_URL];
  }
  return `${await getBackendUrl()}/api/impulse/plan`;
}

/**
 * Stores the endpoint used for POST /impulse/plan requests from the Query & Plan tab.
 * @param {string} url
 * @returns {Promise<void>}
 */
export async function setPlanBackendUrl(url) {
  await storage.set({ [STORAGE_KEY_PLAN_BACKEND_URL]: (url || '').trim() });
}

const STORAGE_KEY_SAVED_LIBRARY = 'impulse_saved_library';

/**
 * Gets all locally saved content items in the library.
 * @returns {Promise<Array>}
 */
export async function getSavedLibrary() {
  const result = await storage.get(STORAGE_KEY_SAVED_LIBRARY);
  return Array.isArray(result[STORAGE_KEY_SAVED_LIBRARY]) ? result[STORAGE_KEY_SAVED_LIBRARY] : [];
}

/**
 * Adds or updates an item in the local saved library.
 * @param {Object} item 
 * @returns {Promise<Array>}
 */
export async function addToSavedLibrary(item) {
  const library = await getSavedLibrary();
  const existingIdx = library.findIndex(l => l.url === item.url);
  const now = new Date().toISOString();
  
  if (existingIdx >= 0) {
    library[existingIdx] = { ...library[existingIdx], ...item, updatedAt: now };
  } else {
    library.unshift({ ...item, savedAt: now });
  }

  await storage.set({ [STORAGE_KEY_SAVED_LIBRARY]: library });
  return library;
}

/**
 * Deletes an item from the local saved library by URL.
 * @param {string} url 
 * @returns {Promise<Array>}
 */
export async function deleteFromSavedLibrary(url) {
  const library = await getSavedLibrary();
  const updated = library.filter(l => l.url !== url);
  await storage.set({ [STORAGE_KEY_SAVED_LIBRARY]: updated });
  return updated;
}

/**
 * Clears the local saved library.
 * @returns {Promise<void>}
 */
export async function clearSavedLibrary() {
  await storage.set({ [STORAGE_KEY_SAVED_LIBRARY]: [] });
}

/**
 * Enqueues a content item to the pending offline queue.
 * @param {Object} item 
 * @returns {Promise<Array>} Updated queue
 */
export async function addToPendingQueue(item) {
  const result = await storage.get(STORAGE_KEY_PENDING_QUEUE);
  const queue = Array.isArray(result[STORAGE_KEY_PENDING_QUEUE]) ? result[STORAGE_KEY_PENDING_QUEUE] : [];

  const existingIdx = queue.findIndex(q => q.url === item.url);
  if (existingIdx >= 0) {
    queue[existingIdx] = { ...queue[existingIdx], ...item, queuedAt: new Date().toISOString() };
  } else {
    queue.push({ ...item, queuedAt: new Date().toISOString() });
  }

  await storage.set({ [STORAGE_KEY_PENDING_QUEUE]: queue });
  return queue;
}

/**
 * Gets all pending unsynced content items.
 * @returns {Promise<Array>}
 */
export async function getPendingQueue() {
  const result = await storage.get(STORAGE_KEY_PENDING_QUEUE);
  return Array.isArray(result[STORAGE_KEY_PENDING_QUEUE]) ? result[STORAGE_KEY_PENDING_QUEUE] : [];
}

/**
 * Saves a content item locally and optionally syncs to backend.
 * Local save always succeeds regardless of backend availability.
 * @param {Object} contentPayload 
 * @returns {Promise<Object>} Status object
 */
export async function saveContent(contentPayload) {
  const payload = {
    url: contentPayload.url,
    title: contentPayload.title || '',
    platform: contentPayload.platform || 'web',
    contentType: contentPayload.contentType || 'article',
    thumbnailUrl: contentPayload.thumbnailUrl || '',
    userNote: contentPayload.userNote || '',
    tags: Array.isArray(contentPayload.tags) ? contentPayload.tags : [],
    description: contentPayload.description || '',
    content: contentPayload.content || '',
    extractedAt: contentPayload.extractedAt || new Date().toISOString()
  };

  // Step 1: Always save locally first — this is the source of truth
  await addToSavedLibrary(payload);

  // Step 2: Send to backend, which invokes AI and persists the RAG memory.
  try {
    const data = await importMemory(payload);
    await addToSavedLibrary({ ...payload, memoryId: data.id, memoryStatus: 'processed' });
    return { success: true, queued: false, data };
  } catch (error) {
    const queue = await addToPendingQueue(payload);
    return { success: true, queued: true, queueCount: queue.length };
  }
}

function memoryContent(item) {
  return [
    item.title && `Title: ${item.title}`,
    item.description && `Description: ${item.description}`,
    item.content && `Visible content:\n${item.content}`,
    item.contentType && `Content type: ${item.contentType}`,
    item.tags?.length && `User tags: ${item.tags.join(', ')}`
  ].filter(Boolean).join('\n\n').slice(0, 50000);
}

async function importMemory(item) {
  const [backendUrl, userId] = await Promise.all([getBackendUrl(), getUserId()]);
  const response = await fetch(`${backendUrl}/api/memories/import`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
    body: JSON.stringify({
      userId,
      url: item.url,
      userNote: item.userNote || null,
      content: memoryContent(item) || null
    })
  });
  if (!response.ok) throw new Error(`Memory import failed (HTTP ${response.status})`);
  return response.json();
}

/**
 * Flushes and syncs items from the pending queue to the backend.
 * @returns {Promise<Object>} Sync metrics ({ syncedCount, remainingCount, errors })
 */
export async function syncPendingQueue() {
  const queue = await getPendingQueue();
  if (queue.length === 0) {
    return { syncedCount: 0, remainingCount: 0, errors: [] };
  }

  const remaining = [];
  const errors = [];
  let syncedCount = 0;

  for (const item of queue) {
    const { queuedAt, ...payload } = item;

    try {
      const data = await importMemory(payload);
      await addToSavedLibrary({ ...payload, memoryId: data.id, memoryStatus: 'processed' });
      syncedCount++;
    } catch (err) {
      remaining.push(item);
      errors.push(`Failed to sync ${item.url}: ${err.message}`);
    }
  }

  await storage.set({ [STORAGE_KEY_PENDING_QUEUE]: remaining });
  return { syncedCount, remainingCount: remaining.length, errors };
}

/**
 * Clears the pending offline queue.
 * @returns {Promise<void>}
 */
export async function clearPendingQueue() {
  await storage.set({ [STORAGE_KEY_PENDING_QUEUE]: [] });
}

// Global scope export for non-module usage
if (typeof globalThis !== 'undefined') {
  globalThis.ImpulseApi = {
    DEFAULT_BACKEND_URL,
    getBackendUrl,
    setBackendUrl,
    getUserId,
    setUserId,
    getPlanBackendUrl,
    setPlanBackendUrl,
    saveContent,
    getPendingQueue,
    syncPendingQueue,
    clearPendingQueue,
    addToPendingQueue,
    getSavedLibrary,
    addToSavedLibrary,
    deleteFromSavedLibrary,
    clearSavedLibrary
  };
}
