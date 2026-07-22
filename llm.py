import json
import time

from google.genai.errors import ServerError

from Impulse.config import client, GEMINI_MODEL

SYSTEM_PROMPT = """
You are an AI assistant.

Given an Instagram caption and image description,
generate metadata.

Return ONLY valid JSON.

{
    "summary": "",
    "topics": [],
    "keywords": [],
    "category": ""
}
"""


def summarize_post(caption, image_description):

    prompt = f"""
Caption:

{caption}

Image Description:

{image_description}
"""

    for attempt in range(5):

        try:

            response = client.models.generate_content(
                model=GEMINI_MODEL,
                contents=SYSTEM_PROMPT + prompt
            )

            text = response.text.strip()
            text = text.replace("```json", "").replace("```", "").strip()

            return json.loads(text)

        except ServerError:

            wait = 2 ** attempt
            print(f"LLM busy... retrying in {wait}s")
            time.sleep(wait)

    raise Exception("LLM API unavailable.")