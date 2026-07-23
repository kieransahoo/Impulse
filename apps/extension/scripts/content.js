/**
 * Impulse Extension - Content Script
 * Injected into active pages to extract metadata and scan YouTube & Instagram links.
 */

(function () {
  /**
   * Reads standard HTML meta tags from the page DOM.
   */
  function extractDomMeta() {
    const getMeta = (selectors) => {
      for (const sel of selectors) {
        const el = document.querySelector(sel);
        if (el) {
          const content = el.getAttribute('content') || el.getAttribute('value');
          if (content && content.trim()) return content.trim();
        }
      }
      return null;
    };

    const title = getMeta([
      'meta[property="og:title"]',
      'meta[name="twitter:title"]',
      'meta[name="title"]'
    ]) || document.title || '';

    const ogImage = getMeta([
      'meta[property="og:image"]',
      'meta[property="og:image:secure_url"]',
      'meta[name="twitter:image"]',
      'meta[name="twitter:image:src"]',
      'link[rel="image_src"]'
    ]) || '';

    const description = getMeta([
      'meta[property="og:description"]',
      'meta[name="description"]',
      'meta[name="twitter:description"]'
    ]) || '';

    const contentRoot = document.querySelector('article, ytd-watch-metadata, ytd-playlist-header-renderer');
    const content = (contentRoot?.innerText || '').trim().slice(0, 40000);
    return { title, ogImage, description, content };
  }

  /**
   * Resolves the extractors module from global window or dynamic ES module import.
   */
  async function getExtractors() {
    if (typeof globalThis !== 'undefined' && globalThis.ImpulseExtractors) {
      return globalThis.ImpulseExtractors;
    }
    if (typeof window !== 'undefined' && window.ImpulseExtractors) {
      return window.ImpulseExtractors;
    }
    try {
      const src = chrome.runtime.getURL('utils/extractors.js');
      const module = await import(src);
      return module;
    } catch (e) {
      console.warn('[Impulse ContentScript] Failed to load extractors module dynamically:', e);
      return null;
    }
  }

  /**
   * Handles metadata extraction for current page.
   */
  async function handleExtractTabMetadata() {
    const meta = extractDomMeta();
    const extractors = await getExtractors();

    if (extractors && typeof extractors.extractMetadata === 'function') {
      return extractors.extractMetadata(window.location.href, {
        title: meta.title,
        ogImage: meta.ogImage,
        description: meta.description,
        content: meta.content
      });
    }

    // Direct fallback if extractor helper unavailable
    return {
      url: window.location.href,
      title: meta.title || document.title,
      platform: 'web',
      contentType: 'article',
      thumbnailUrl: meta.ogImage || '',
      userNote: '',
      tags: [],
      extractedAt: new Date().toISOString()
    };
  }

  /**
   * Handles batch link scanning for current page DOM.
   */
  async function handleScanPageLinks() {
    const extractors = await getExtractors();

    if (extractors && typeof extractors.scanPageForLinks === 'function') {
      return extractors.scanPageForLinks(document);
    }

    return [];
  }

  /**
   * Message listener for commands from Popup or Background worker.
   */
  if (typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.onMessage) {
    chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
      if (request.action === 'EXTRACT_TAB_METADATA') {
        handleExtractTabMetadata().then(sendResponse);
        return true; // Keep async response channel open
      } else if (request.action === 'SCAN_PAGE_LINKS') {
        handleScanPageLinks().then(sendResponse);
        return true;
      } else if (request.action === 'PING') {
        sendResponse({ status: 'pong', ready: true });
        return false;
      }
    });
  }

})();
