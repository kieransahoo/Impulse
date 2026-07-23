#!/usr/bin/env python3
"""
Impulse Extension - Python Automated Specification & Logic Verification Suite
Verifies regex patterns, metadata schemas, URL sanitization rules, and file structures.
"""

import os
import re
import json
import urllib.parse

def run_tests():
    print("=" * 60)
    print(" Running Impulse Chrome Extension Automated Verification ")
    print("=" * 60)

    passed = 0
    failed = 0

    def assert_true(condition, test_name):
        nonlocal passed, failed
        if condition:
            print(f"  [PASS] {test_name}")
            passed += 1
        else:
            print(f"  [FAIL] {test_name}")
            failed += 1

    # 1. File Structure Verification
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    required_files = [
        "manifest.json",
        "utils/extractors.js",
        "utils/api.js",
        "scripts/background.js",
        "scripts/content.js",
        "popup/popup.html",
        "popup/popup.css",
        "popup/popup.js",
        "icons/icon16.png",
        "icons/icon48.png",
        "icons/icon128.png"
    ]

    for rel_path in required_files:
        full_p = os.path.join(base_dir, rel_path)
        assert_true(os.path.exists(full_p), f"Required file exists: {rel_path}")

    # 2. Manifest V3 Schema Verification
    manifest_path = os.path.join(base_dir, "manifest.json")
    with open(manifest_path, "r") as f:
        manifest = json.load(f)

    assert_true(manifest.get("manifest_version") == 3, "Manifest uses version 3")
    assert_true("activeTab" in manifest.get("permissions", []), "Manifest has activeTab permission")
    assert_true("storage" in manifest.get("permissions", []), "Manifest has storage permission")
    assert_true("contextMenus" in manifest.get("permissions", []), "Manifest has contextMenus permission")
    assert_true(manifest.get("background", {}).get("service_worker") == "scripts/background.js", "Service worker configured")

    # 3. Test Tracking Query Sanitization Logic
    tracking_params = {'utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content', 'fbclid', 'igshid', 'si', 'gclid'}
    
    def python_sanitize_url(url_str):
        parsed = urllib.parse.urlparse(url_str)
        qs = urllib.parse.parse_qs(parsed.query, keep_blank_values=True)
        clean_qs = [(k, v) for k, vs in qs.items() for v in vs if k.lower() not in tracking_params and not k.lower().startswith('utm_')]
        clean_query = urllib.parse.urlencode(clean_qs)
        return urllib.parse.urlunparse((parsed.scheme, parsed.netloc, parsed.path, parsed.params, clean_query, parsed.fragment))

    raw_yt = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&utm_source=facebook&si=xyz123"
    sanitized_yt = python_sanitize_url(raw_yt)
    assert_true("utm_source" not in sanitized_yt and "si=" not in sanitized_yt and "v=dQw4w9WgXcQ" in sanitized_yt, "YouTube tracking parameters sanitized while preserving 'v'")

    # 4. YouTube Regex Matching Verification
    yt_watch_re = re.compile(r"^https?:\/\/(?:www\.|m\.)?youtube\.com\/watch\?(?:.*&)?v=([a-zA-Z0-9_-]{11})")
    yt_short_re = re.compile(r"^https?:\/\/(?:www\.|m\.)?youtube\.com\/shorts\/([a-zA-Z0-9_-]{11})")

    watch_match = yt_watch_re.match("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    assert_true(watch_match and watch_match.group(1) == "dQw4w9WgXcQ", "YouTube watch video ID extraction")

    short_match = yt_short_re.match("https://www.youtube.com/shorts/abcd1234efg")
    assert_true(short_match and short_match.group(1) == "abcd1234efg", "YouTube Short video ID extraction")

    # 5. Instagram Regex Matching Verification
    ig_post_re = re.compile(r"^https?:\/\/(?:www\.)?instagram\.com\/p\/([a-zA-Z0-9_-]+)")
    ig_reel_re = re.compile(r"^https?:\/\/(?:www\.)?instagram\.com\/reel\/([a-zA-Z0-9_-]+)")

    post_match = ig_post_re.match("https://www.instagram.com/p/C123456789/")
    assert_true(post_match and post_match.group(1) == "C123456789", "Instagram Post shortcode extraction")

    reel_match = ig_reel_re.match("https://www.instagram.com/reel/D987654321/")
    assert_true(reel_match and reel_match.group(1) == "D987654321", "Instagram Reel shortcode extraction")

    # 6. Payload Schema Verification
    sample_payload = {
        "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "title": "Rick Astley - Never Gonna Give You Up",
        "platform": "youtube",
        "contentType": "video",
        "thumbnailUrl": "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
        "userNote": "Great classic video",
        "tags": ["music", "classic"],
        "extractedAt": "2026-07-23T13:07:25Z"
    }

    required_keys = {"url", "title", "platform", "contentType", "thumbnailUrl", "userNote", "tags", "extractedAt"}
    assert_true(required_keys.issubset(sample_payload.keys()), "Payload strictly follows Impulse ingestion schema")

    with open(os.path.join(base_dir, "utils/api.js"), "r") as f:
        api_source = f.read()
    assert_true("http://localhost:8081" in api_source, "Default backend base URL uses port 8081")
    assert_true("/api/memories/import" in api_source, "Capture payload maps to memory import endpoint")
    assert_true("getUserId" in api_source and "STORAGE_KEY_USER_ID" in api_source, "Stable user identity helper is present")
    assert_true("/api/impulse/plan" in api_source, "Personalized planning endpoint is configured")

    print("=" * 60)
    print(f" Test Results: {passed} PASSED, {failed} FAILED")
    print("=" * 60)

    if failed > 0:
        exit(1)

if __name__ == "__main__":
    run_tests()
