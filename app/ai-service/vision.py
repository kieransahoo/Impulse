import json
import time
import requests
from io import BytesIO
from PIL import Image

from PIL import Image
from google.genai.errors import ServerError

from config import client, GEMINI_MODEL

VISION_PROMPT = """
Analyze this Instagram image.

Return ONLY valid JSON.

{
    "objects": [],
    "scene": "",
    "activities": [],
    "ocr_text": "",
    "description": ""
}
"""


def describe_image(image_url):

    response = requests.get(image_url)
    response.raise_for_status()

    image = Image.open(BytesIO(response.content))

    for attempt in range(5):

        try:

            response = client.models.generate_content(
                model=GEMINI_MODEL,
                contents=[image, VISION_PROMPT]
            )

            text = response.text.strip()
            text = text.replace("```json", "").replace("```", "").strip()

            return json.loads(text)

        except ServerError:

            wait = 2 ** attempt
            print(f"Vision API busy... retrying in {wait}s")
            time.sleep(wait)

    raise Exception("Vision API unavailable.")