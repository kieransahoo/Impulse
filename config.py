import os

from dotenv import load_dotenv
from google import genai
from sentence_transformers import SentenceTransformer

load_dotenv()

client = genai.Client(
    api_key=os.getenv("GEMINI_API_KEY")
)

GEMINI_MODEL = os.getenv(
    "GEMINI_MODEL",
    "gemini-2.5-flash-lite"
)

embedding_model = SentenceTransformer(
    "all-MiniLM-L6-v2"
)

import chromadb

CHROMA_CLIENT = chromadb.PersistentClient(
    path="./chroma_db"
)

COLLECTION = CHROMA_CLIENT.get_or_create_collection(
    name="creatorbrain"
)