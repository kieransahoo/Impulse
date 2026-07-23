import json
import time
from config import client, GEMINI_MODEL

def summarize_post(caption, hashtags, image_description):

    prompt = f"""
You are an information extraction system.

Given an Instagram caption and image description, extract structured information
that can be used later by an AI assistant for recommendations.

Possible domains:
- cafe
- restaurant
- travel
- shopping
- recipe
- fitness
- technology
- fashion
- movie
- book
- event
- general

Return ONLY valid JSON.

JSON Schema:

{{
  "domain":"",
  "summary":"",
  "topics":[],
  "keywords":[],

  "entities":{{
      "places":[],
      "restaurants":[],
      "cafes":[],
      "cities":[],
      "countries":[],
      "brands":[],
      "products":[],
      "people":[]
  }},

  "recommendations":[
      {{
          "name":"",
          "type":"",
          "location":"",
          "budget":"",
          "best_for":[],
          "features":[]
      }}
  ],

  "activities":[],
  "budget":"",
  "occasion":[],
  "searchable_facts":[],
  "hashtags":[],
  "sentiment":""
}}

Caption:
{caption}

Image Description:
{image_description}

Do not invent facts.

If something is unavailable use "" or [].
"""

    for attempt in range(3):

        try:

            response = client.models.generate_content(
                model=GEMINI_MODEL,
                contents=prompt
            )

            text = response.text.strip()

            if text.startswith("```"):
                text = text.split("```")[1]
                if text.startswith("json"):
                    text = text[4:]
                text = text.strip()

            return json.loads(text)

        except Exception as e:

            print(e)

            if attempt < 2:
                time.sleep(2 ** attempt)

    return None