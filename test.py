from posts import POSTS

from vision import describe_image
from llm import summarize_post
from document_builder import build_document

from embeddings import create_embedding
from chroma_store import store_post, delete_all_posts


def ingest_posts():

    print("=" * 60)
    print("CreatorBrain Ingestion Started")
    print("=" * 60)

    # Remove previously stored vectors
    delete_all_posts()

    print("\nOld ChromaDB collection cleared.\n")

    total_posts = len(POSTS)

    for index, post in enumerate(POSTS, start=1):

        print(f"[{index}/{total_posts}] Processing {post['id']}")

        # -----------------------------
        # Vision
        # -----------------------------
        vision = describe_image(post["image"])

        # -----------------------------
        # LLM Extraction
        # -----------------------------
        data = summarize_post(
            caption=post["caption"],
            hashtags=post["hashtags"],
            image_description=vision["description"]
        )

        if data is None:
            print(f"❌ Failed to process {post['id']}")
            continue

        # -----------------------------
        # Build document
        # -----------------------------
        document = build_document(
            caption=post["caption"],
            hashtags=post["hashtags"],
            vision=vision,
            data=data
        )

        # -----------------------------
        # Embedding
        # -----------------------------
        embedding = create_embedding(document)

        # -----------------------------
        # Metadata
        # -----------------------------
        metadata = {

            "domain": data["domain"],

            "summary": data["summary"],

            "budget": data["budget"],

            "occasion": ",".join(data["occasion"]),

            "hashtags": ",".join(post["hashtags"]),

            "cities": ",".join(
                data["entities"]["cities"]
            ),

            "places": ",".join(
                data["entities"]["places"]
            ),

            "cafes": ",".join(
                data["entities"]["cafes"]
            ),

            "restaurants": ",".join(
                data["entities"]["restaurants"]
            ),

            "brands": ",".join(
                data["entities"]["brands"]
            ),

            "caption": post["caption"]
        }

        # -----------------------------
        # Store
        # -----------------------------
        store_post(
            post_id=post["id"],
            document=document,
            embedding=embedding,
            metadata=metadata
        )

        print(f"✅ Stored {post['id']}\n")

    print("=" * 60)
    print("🎉 All posts successfully ingested!")
    print("=" * 60)


if __name__ == "__main__":
    ingest_posts()