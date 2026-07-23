def build_document(
    caption,
    hashtags,
    vision,
    data
):

    document = f"""
DOMAIN
{data['domain']}

CAPTION
{caption}

IMAGE DESCRIPTION
{vision['description']}

SUMMARY
{data['summary']}

TOPICS
{", ".join(data['topics'])}

KEYWORDS
{", ".join(data['keywords'])}

PLACES
{", ".join(data['entities']['places'])}

CAFES
{", ".join(data['entities']['cafes'])}

RESTAURANTS
{", ".join(data['entities']['restaurants'])}

CITIES
{", ".join(data['entities']['cities'])}

COUNTRIES
{", ".join(data['entities']['countries'])}

BRANDS
{", ".join(data['entities']['brands'])}

PRODUCTS
{", ".join(data['entities']['products'])}

PEOPLE
{", ".join(data['entities']['people'])}

ACTIVITIES
{", ".join(data['activities'])}

BUDGET
{data['budget']}

OCCASION
{", ".join(data['occasion'])}

FACTS
{", ".join(data['searchable_facts'])}

HASHTAGS
{", ".join(data['hashtags'])}

SENTIMENT
{data['sentiment']}
"""

    return document