from config import COLLECTION


def store_post(
    post_id,
    document,
    embedding,
    metadata
):

    COLLECTION.add(
        ids=[post_id],
        documents=[document],
        embeddings=[embedding],
        metadatas=[metadata]
    )

    print(f"✅ Stored {post_id}")


def get_all_posts():

    return COLLECTION.get()


def delete_all_posts():

    ids = COLLECTION.get()["ids"]

    if ids:
        COLLECTION.delete(ids=ids)
        print(f"Deleted {len(ids)} posts.")
    else:
        print("Collection already empty.")