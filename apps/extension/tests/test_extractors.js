/**
 * Impulse Extension - Unit Test Suite for Extractors and API
 */

import { 
  sanitizeUrl, 
  extractYouTubeMetadata, 
  extractInstagramMetadata, 
  extractGenericMetadata, 
  extractMetadata 
} from '../utils/extractors.js';

export function runTests() {
  const results = [];
  
  function assertEqual(actual, expected, testName) {
    if (actual === expected) {
      results.push({ name: testName, status: 'PASS' });
    } else {
      results.push({ name: testName, status: 'FAIL', actual, expected });
    }
  }

  // 1. Sanitize URL test
  const rawUrl = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ&utm_source=facebook&fbclid=xyz&si=123';
  const clean = sanitizeUrl(rawUrl);
  assertEqual(clean, 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 'Sanitize YouTube tracking params');

  // 2. YouTube Standard Video
  const ytVideo = extractMetadata('https://www.youtube.com/watch?v=dQw4w9WgXcQ', { title: 'Never Gonna Give You Up' });
  assertEqual(ytVideo.platform, 'youtube', 'YT Video Platform');
  assertEqual(ytVideo.contentType, 'video', 'YT Video Content Type');
  assertEqual(ytVideo.thumbnailUrl, 'https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg', 'YT Video Thumbnail');

  // 3. YouTube Short
  const ytShort = extractMetadata('https://www.youtube.com/shorts/abcd1234efg', { title: 'Funny Short' });
  assertEqual(ytShort.platform, 'youtube', 'YT Short Platform');
  assertEqual(ytShort.contentType, 'short', 'YT Short Content Type');
  assertEqual(ytShort.url, 'https://www.youtube.com/shorts/abcd1234efg', 'YT Short Canonical URL');

  // 4. Instagram Post
  const igPost = extractMetadata('https://www.instagram.com/p/C123456789/', { title: 'Photo Post' });
  assertEqual(igPost.platform, 'instagram', 'IG Post Platform');
  assertEqual(igPost.contentType, 'post', 'IG Post Content Type');
  assertEqual(igPost.url, 'https://www.instagram.com/p/C123456789/', 'IG Post Canonical URL');

  // 5. Instagram Reel
  const igReel = extractMetadata('https://www.instagram.com/reel/D987654321/?igshid=test', { title: 'Cool Reel' });
  assertEqual(igReel.platform, 'instagram', 'IG Reel Platform');
  assertEqual(igReel.contentType, 'reel', 'IG Reel Content Type');
  assertEqual(igReel.url, 'https://www.instagram.com/reel/D987654321/', 'IG Reel Clean Canonical URL');

  // 6. Generic Web Page
  const webPage = extractMetadata('https://example.com/article?utm_medium=email', { title: 'Web Article', ogImage: 'https://example.com/img.jpg' });
  assertEqual(webPage.platform, 'web', 'Web Platform');
  assertEqual(webPage.contentType, 'article', 'Web Content Type');
  assertEqual(webPage.url, 'https://example.com/article', 'Web Sanitized URL');

  return results;
}
