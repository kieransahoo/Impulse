/**
 * Impulse Extension - Metadata and Link Extraction Utilities
 * Standardized extractors for YouTube, Instagram, and Generic Web URLs.
 */

// Tracking query parameters to sanitize
const TRACKING_PARAMS = new Set([
  'utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content',
  'fbclid', 'igshid', 'si', 'gclid', 'msclkid', 'ref', '_hsenc', '_hsmi',
  'mkt_tok', 'mc_cid', 'mc_eid', 'action_object_map', 'action_type_map',
  'action_ref_map'
]);

/**
 * Sanitizes a URL by removing known tracking parameters while preserving structural params.
 * @param {string} urlString 
 * @returns {string} Sanitized URL string
 */
export function sanitizeUrl(urlString) {
  if (!urlString) return '';
  try {
    const url = new URL(urlString);
    const searchParams = new URLSearchParams(url.search);
    const keysToDelete = [];

    for (const key of searchParams.keys()) {
      if (TRACKING_PARAMS.has(key.toLowerCase()) || key.toLowerCase().startsWith('utm_')) {
        keysToDelete.push(key);
      }
    }

    keysToDelete.forEach(key => searchParams.delete(key));

    url.search = searchParams.toString() ? `?${searchParams.toString()}` : '';
    return url.toString();
  } catch (e) {
    return urlString;
  }
}

/**
 * Regex rules for YouTube & Instagram
 */
const YT_WATCH_REGEX = /^https?:\/\/(?:www\.|m\.)?youtube\.com\/watch\?(?:.*&)?v=([a-zA-Z0-9_-]{11})/;
const YT_SHORT_REGEX = /^https?:\/\/(?:www\.|m\.)?youtube\.com\/shorts\/([a-zA-Z0-9_-]{11})/;
const YT_BE_REGEX = /^https?:\/\/youtu\.be\/([a-zA-Z0-9_-]{11})/;
const YT_EMBED_REGEX = /^https?:\/\/(?:www\.|m\.)?youtube\.com\/embed\/([a-zA-Z0-9_-]{11})/;

const IG_POST_REGEX = /^https?:\/\/(?:www\.)?instagram\.com\/p\/([a-zA-Z0-9_-]+)/;
const IG_REEL_REGEX = /^https?:\/\/(?:www\.)?instagram\.com\/reel\/([a-zA-Z0-9_-]+)/;

/**
 * Extracts metadata for YouTube URLs.
 * @param {string} urlStr 
 * @param {Object} [metaData] - Optional extracted DOM meta (title, description, ogImage)
 * @returns {Object|null}
 */
export function extractYouTubeMetadata(urlStr, metaData = {}) {
  let videoId = null;
  let contentType = 'video';

  const watchMatch = urlStr.match(YT_WATCH_REGEX);
  const shortMatch = urlStr.match(YT_SHORT_REGEX);
  const beMatch = urlStr.match(YT_BE_REGEX);
  const embedMatch = urlStr.match(YT_EMBED_REGEX);

  if (shortMatch) {
    videoId = shortMatch[1];
    contentType = 'short';
  } else if (watchMatch) {
    videoId = watchMatch[1];
    contentType = 'video';
  } else if (beMatch) {
    videoId = beMatch[1];
    contentType = 'video';
  } else if (embedMatch) {
    videoId = embedMatch[1];
    contentType = 'video';
  }

  if (!videoId) {
    try {
      const parsed = new URL(urlStr);
      if (parsed.hostname.includes('youtube.com') && parsed.pathname.startsWith('/shorts/')) {
        const parts = parsed.pathname.split('/');
        if (parts[2] && parts[2].length >= 11) {
          videoId = parts[2].substring(0, 11);
          contentType = 'short';
        }
      } else if (parsed.hostname.includes('youtube.com') && parsed.searchParams.has('v')) {
        videoId = parsed.searchParams.get('v');
        contentType = 'video';
      }
    } catch (e) {}
  }

  if (!videoId) return null;

  const canonicalUrl = contentType === 'short' 
    ? `https://www.youtube.com/shorts/${videoId}` 
    : `https://www.youtube.com/watch?v=${videoId}`;

  return {
    url: canonicalUrl,
    title: metaData.title || metaData.ogTitle || `YouTube ${contentType === 'short' ? 'Short' : 'Video'} (${videoId})`,
    platform: 'youtube',
    contentType,
    thumbnailUrl: `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`,
    description: metaData.description || '',
    content: metaData.content || '',
    userNote: metaData.userNote || '',
    tags: Array.isArray(metaData.tags) ? metaData.tags : [],
    extractedAt: new Date().toISOString()
  };
}

/**
 * Extracts metadata for Instagram URLs.
 * @param {string} urlStr 
 * @param {Object} [metaData] - Optional extracted DOM meta
 * @returns {Object|null}
 */
export function extractInstagramMetadata(urlStr, metaData = {}) {
  let shortcode = null;
  let contentType = 'post';

  const reelMatch = urlStr.match(IG_REEL_REGEX);
  const postMatch = urlStr.match(IG_POST_REGEX);

  if (reelMatch) {
    shortcode = reelMatch[1];
    contentType = 'reel';
  } else if (postMatch) {
    shortcode = postMatch[1];
    contentType = 'post';
  }

  if (!shortcode) return null;

  const canonicalUrl = `https://www.instagram.com/${contentType === 'reel' ? 'reel' : 'p'}/${shortcode}/`;

  return {
    url: canonicalUrl,
    title: metaData.title || metaData.ogTitle || `Instagram ${contentType === 'reel' ? 'Reel' : 'Post'} (${shortcode})`,
    platform: 'instagram',
    contentType,
    thumbnailUrl: metaData.ogImage || metaData.thumbnailUrl || '',
    description: metaData.description || '',
    content: metaData.content || '',
    userNote: metaData.userNote || '',
    tags: Array.isArray(metaData.tags) ? metaData.tags : [],
    extractedAt: new Date().toISOString()
  };
}

/**
 * Extracts metadata for Generic Web pages.
 * @param {string} urlStr 
 * @param {Object} [metaData] - Extracted DOM meta
 * @returns {Object}
 */
export function extractGenericMetadata(urlStr, metaData = {}) {
  const sanitized = sanitizeUrl(urlStr);

  return {
    url: sanitized,
    title: metaData.title || metaData.ogTitle || sanitized,
    platform: 'web',
    contentType: 'article',
    thumbnailUrl: metaData.ogImage || metaData.twitterImage || metaData.thumbnailUrl || '',
    description: metaData.description || '',
    content: metaData.content || '',
    userNote: metaData.userNote || '',
    tags: Array.isArray(metaData.tags) ? metaData.tags : [],
    extractedAt: new Date().toISOString()
  };
}

/**
 * Main dispatcher to extract metadata based on URL pattern matching.
 * @param {string} urlStr 
 * @param {Object} [metaData] 
 * @returns {Object} Structured metadata payload
 */
export function extractMetadata(urlStr, metaData = {}) {
  if (!urlStr) return null;
  
  const ytData = extractYouTubeMetadata(urlStr, metaData);
  if (ytData) return ytData;

  const igData = extractInstagramMetadata(urlStr, metaData);
  if (igData) return igData;

  return extractGenericMetadata(urlStr, metaData);
}

/**
 * Scans an HTML element or Document for YouTube and Instagram links.
 * @param {Document|HTMLElement} container 
 * @returns {Array<Object>} List of extracted unique content links with details
 */
export function scanPageForLinks(container = document) {
  if (!container || typeof container.querySelectorAll !== 'function') {
    return [];
  }

  const anchors = Array.from(container.querySelectorAll('a[href]'));
  const foundMap = new Map();

  for (const anchor of anchors) {
    const rawHref = anchor.href;
    if (!rawHref || rawHref.startsWith('javascript:') || rawHref.startsWith('#')) continue;

    const sanitized = sanitizeUrl(rawHref);
    
    // Check if matching YouTube or Instagram
    const ytMatch = extractYouTubeMetadata(sanitized, {});
    const igMatch = extractInstagramMetadata(sanitized, {});
    
    const matched = ytMatch || igMatch;
    if (matched) {
      if (!foundMap.has(matched.url)) {
        const linkText = (anchor.textContent || anchor.title || anchor.getAttribute('aria-label') || '').trim();
        if (linkText && linkText.length > 2 && !matched.title.includes(linkText)) {
          matched.title = linkText;
        }
        foundMap.set(matched.url, matched);
      }
    }
  }

  return Array.from(foundMap.values());
}

// Expose on globalThis if in non-module environment
if (typeof globalThis !== 'undefined') {
  globalThis.ImpulseExtractors = {
    sanitizeUrl,
    extractYouTubeMetadata,
    extractInstagramMetadata,
    extractGenericMetadata,
    extractMetadata,
    scanPageForLinks
  };
}
