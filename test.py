from Impulse.vision import describe_image
from Impulse.llm import summarize_post
from Impulse.embeddings import create_embedding
from Impulse.chroma_store import store_post

caption = """
The flower is so pink and beautiful.

#Flower #Nature #Pink
"""

print("=" * 50)
print("STEP 1 : IMAGE ANALYSIS")
print("=" * 50)

vision = describe_image("images/flower.jpg")

print(vision)

print()

print("=" * 50)
print("STEP 2 : LLM SUMMARY")
print("=" * 50)

summary = summarize_post(
    caption,
    vision["description"]
)

print(summary)

document = f"""
Caption:
{caption}

Image Description:
{vision["description"]}

Summary:
{summary["summary"]}

Topics:
{", ".join(summary["topics"])}

Keywords:
{", ".join(summary["keywords"])}

Category:
{summary["category"]}
"""

print()

print("=" * 50)
print("STEP 3 : EMBEDDING")
print("=" * 50)

embedding = create_embedding(document)

print("Embedding Length:", len(embedding))
metadata = {
    "category": summary["category"],
    "summary": summary["summary"],
    "caption": caption
}

store_post(
    post_id="post_1",
    document=document,
    embedding=embedding,
    metadata=metadata
)

print()

print("=" * 50)
print("DOCUMENT")
print("=" * 50)

print(document)