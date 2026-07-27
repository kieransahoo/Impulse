from config import COLLECTION
from embeddings import create_embedding

SIMILARITY_THRESHOLD = 0.75


def retrieve(query, where=None):

    query_embedding = create_embedding(query)

    results = COLLECTION.query(
        query_embeddings=[query_embedding],
        n_results=10,
        where=where
    )

    filtered = []

    for document, metadata, distance in zip(
        results["documents"][0],
        results["metadatas"][0],
        results["distances"][0]
    ):

        similarity = 1 - distance

        if similarity >= SIMILARITY_THRESHOLD:

            filtered.append(
                {
                    "similarity": similarity,
                    "document": document,
                    "metadata": metadata
                }
            )

    filtered.sort(
        key=lambda x: x["similarity"],
        reverse=True
    )

    return filtered

results = retrieve(query)

if not results:

    print(
        "No relevant saved posts found."
    )