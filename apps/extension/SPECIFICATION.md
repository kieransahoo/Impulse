# Product Specification: Impulse Chrome Extension (Manifest V3)

## 1. Overview & Objectives

The **Impulse Chrome Extension** is the primary ingestion frontend for the **CreatorBrain + Impulse** platform. Its core objective is to allow users to effortlessly capture internet content—including web articles, YouTube videos/Shorts, and Instagram Reels/posts—and send structured URL metadata and optional user notes to the Impulse backend system.

### Key Goals
- **Single-Click Capture**: Instantly extract and save current tab metadata (URL, title, thumbnail, platform type).
- **Specialized Social Media Parsing**: Specialized link extraction for YouTube (Videos & Shorts) and Instagram (Reels & Posts).
- **Batch Link Extraction**: Extract embedded or list items from YouTube/Instagram pages or saved collections.
- **Offline & Resilient Sync**: Cache saved links in `chrome.storage.local` when offline and dispatch to the Impulse backend (`POST /api/memories/import`).

---

## 2. User Stories

### US-1: One-Click Quick Save
> **As a** user browsing the web,  
> **I want to** click the Impulse extension icon to save the current web page,  
> **So that** I can store articles, blog posts, or product pages into my second brain without interrupting my workflow.

### US-2: YouTube Content Extraction (Videos & Shorts)
> **As a** user watching YouTube,  
> **I want to** automatically detect whether I am viewing a standard video or a YouTube Short,  
> **So that** correct metadata (video ID, canonical URL, thumbnail URL, channel title) is captured accurately.

### US-3: Instagram Content Extraction (Reels & Posts)
> **As a** user browsing Instagram,  
> **I want to** extract Instagram Reel and Post URLs (`/reel/{id}` or `/p/{id}`),  
> **So that** I can save recommendations and content links even if Instagram limits full page scraping.

### US-4: Page Link Discovery / Batch Extraction
> **As a** user viewing a page with multiple YouTube or Instagram links (e.g. bookmarks or curations),  
> **I want to** run a "Scan & Extract Links" operation,  
> **So that** I can select and save multiple content links at once.

### US-5: User Annotations & Tags
> **As a** user saving content,  
> **I want to** attach a custom note or tag before saving,  
> **So that** Impulse AI has contextual hints when retrieving or generating recommendations later.

---

## 3. Extension Architecture (Manifest V3)

### 3.1 Component Architecture Diagram

```mermaid
flowchart TD
    subgraph Chrome Extension (app/extension)
        PopupUI["Popup UI (popup.html / popup.js)\n- Quick Save & Notes\n- Batch Link List\n- Settings"]
        BackgroundWorker["Background Service Worker (background.js)\n- Event Listeners\n- Context Menu\n- Storage Sync & API Client"]
        ContentScript["Content Script (content.js)\n- DOM Parser\n- YouTube/Instagram Link Scraper\n- Metadata Extractor"]
        Storage["Chrome Local Storage (chrome.storage.local)\n- Drafts & Queue\n- Backend Endpoint Settings"]
    end

    subgraph Impulse Backend API
        API["Spring Boot API\n(POST /api/memories/import)"]
    end

    PopupUI <--> BackgroundWorker
    BackgroundWorker <--> ContentScript
    BackgroundWorker <--> Storage
    BackgroundWorker -->|HTTP POST| API
```

### 3.2 File Structure Layout

```text
app/extension/
├── manifest.json            # Manifest V3 configuration
├── popup/
│   ├── popup.html           # UI layout
│   ├── popup.css            # Extension styling
│   └── popup.js             # UI logic & messaging
├── scripts/
│   ├── content.js           # DOM content script for metadata & link extraction
│   └── background.js        # Service worker for background jobs & API communication
├── utils/
│   ├── extractors.js        # Domain-specific regex & DOM selectors (YouTube, IG, Web)
│   └── api.js               # Backend API integration client
├── icons/                   # Extension icons (16x16, 48x48, 128x128)
└── SPECIFICATION.md         # This product specification
```

---

## 4. Technical Specifications & Extraction Rules

### 4.1 Manifest V3 (`manifest.json`)
- `manifest_version`: 3
- `name`: "Impulse - AI Second Brain Collector"
- `permissions`:
  - `activeTab`
  - `scripting`
  - `storage`
  - `contextMenus`
- `host_permissions`:
  - `https://www.youtube.com/*`
  - `https://www.instagram.com/*`
  - `http://localhost:*/*`
  - `https://*/*`

### 4.2 Metadata Schema (Ingestion Request Payload)
The extension first captures this local UI payload:

```json
{
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "title": "Rick Astley - Never Gonna Give You Up",
  "platform": "youtube",
  "contentType": "video",
  "thumbnailUrl": "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
  "userNote": "Great classic video",
  "tags": ["music", "classic"],
  "extractedAt": "2026-07-23T13:07:25Z"
}
```

`utils/api.js` then maps it to the backend contract:

```json
{
  "userId": "stable UUID stored by the extension",
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "userNote": "Great classic video",
  "content": "Title, description, visible text, content type, and tags"
}
```

Planning uses the same UUID with `POST /api/impulse/plan`.

### 4.3 Link Extraction Rules

#### A. Generic Web Pages
- **URL**: `window.location.href` (stripped of tracking params like `utm_*`, `fbclid`).
- **Title**: `document.title` or `og:title` meta tag.
- **Platform**: `"web"`.
- **ContentType**: `"article"`.
- **Thumbnail**: `og:image` meta tag content.

#### B. YouTube Extractor Rules
- **Regex Patterns**:
  - Standard Video: `/^https?:\/\/(www\.)?youtube\.com\/watch\?v=([a-zA-Z0-9_-]{11})/`
  - Shortened URL: `/^https?:\/\/youtu\.be\/([a-zA-Z0-9_-]{11})/`
  - YouTube Short: `/^https?:\/\/(www\.)?youtube\.com\/shorts\/([a-zA-Z0-9_-]{11})/`
- **Metadata Extraction**:
  - `contentType`: If path contains `/shorts/`, set `"short"`, else `"video"`.
  - `thumbnailUrl`: `https://img.youtube.com/vi/{videoId}/hqdefault.jpg`.
  - `platform`: `"youtube"`.

#### C. Instagram Extractor Rules
- **Regex Patterns**:
  - Instagram Post: `/^https?:\/\/(www\.)?instagram\.com\/p\/([a-zA-Z0-9_-]+)/`
  - Instagram Reel: `/^https?:\/\/(www\.)?instagram\.com\/reel\/([a-zA-Z0-9_-]+)/`
- **Metadata Extraction**:
  - `contentType`: If path contains `/reel/`, set `"reel"`, else `"post"`.
  - Canonical URL clean: `https://www.instagram.com/{p|reel}/{shortcode}/`.
  - `platform`: `"instagram"`.
  - `title`: `og:title` or header text if accessible.

#### D. Batch Page Link Extraction (DOM Scanner)
- Scans `document.querySelectorAll('a[href]')` on the active page.
- Filters anchors against YouTube and Instagram regex patterns.
- Returns deduplicated list of links with title preview for bulk user selection in the popup UI.

---

## 5. Acceptance Criteria (AC)

### AC-1: Manifest V3 Compliance
- [ ] Extension uses `manifest_version: 3` and background service worker (`background.js`).
- [ ] Loads cleanly in Chrome (`chrome://extensions`) without warnings or permissions errors.

### AC-2: Current Tab Metadata Extraction
- [ ] Clicking "Save Current Page" in popup correctly extracts URL, Title, and Thumbnail for standard web pages.
- [ ] Canonical URLs are sanitized (stripping tracking query parameters).

### AC-3: YouTube & Instagram Specialized Scrapers
- [ ] YouTube watch URLs extract `videoId` and classify as `contentType: "video"`.
- [ ] YouTube shorts URLs (`/shorts/id`) extract `videoId` and classify as `contentType: "short"`.
- [ ] Instagram post URLs (`/p/code`) and reel URLs (`/reel/code`) are correctly recognized and classified as `"post"` and `"reel"`.

### AC-4: Batch Link Extraction
- [ ] Popup includes an "Extract Links" button.
- [ ] Content script scans page for all embedded/anchor links matching YouTube or Instagram rules and displays a checkable list in the popup.

### AC-5: Note Input & API Dispatch
- [ ] User can type an optional `userNote` in the popup UI.
- [ ] Clicking "Save" dispatches HTTP POST request to configured backend endpoint (e.g. `http://localhost:8080/contents` or fallback local storage).
- [ ] Shows clear visual feedback (Success checkmark or Error toast).

### AC-6: Offline & Failure Handling
- [ ] If network request fails or API is unreachable, saved item is stored in `chrome.storage.local` queue under key `pending_sync`.

---

## 6. Developer Guidelines for `dev` Agent

1. **Modular Code Structure**:
   - Keep URL extractors in `utils/extractors.js` with pure exported functions for testability.
   - Separate API service calls into `utils/api.js`.
2. **Chrome Extension Security & CSP**:
   - Avoid `eval()` or inline execution script injections.
   - Use `chrome.runtime.sendMessage` and `chrome.tabs.sendMessage` for asynchronous IPC between components.
3. **No External Heavy Dependencies**:
   - Build UI using standard HTML5/CSS3 and native JS (ES6+) for minimal footprint and maximum speed.
4. **Testing Setup**:
   - Provide unit tests or test scripts in `scripts/` or `tests/` to validate URL regex matching and payload schemas.
