from Impulse.config import embedding_model


def create_embedding(text):

    embedding = embedding_model.encode(
        text,
        convert_to_numpy=True
    )

    return embedding.tolist()