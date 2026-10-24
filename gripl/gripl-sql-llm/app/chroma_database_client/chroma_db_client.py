from pathlib import Path
import chromadb
import traceback
from dotenv import load_dotenv
import os
from app.llm_fallback_manager_component.llm_fallback_manager import LLMFallBackManager

load_dotenv()




class ChromaDatabaseClient:
    def __init__(self,
                 embedding_model_handler: LLMFallBackManager,
                 dictionary_name: str,
                 collection_name: str,
                 ):
        self.db_path = Path(__file__).resolve().parent / "embeddings" / dictionary_name
        self.db_path.mkdir(parents=True, exist_ok=True)
        self.embedding_model_handler = embedding_model_handler
        self.collection_name = collection_name
        self.batch_size = 16
        self.top_k = 5


    def get_collection(self):
        client = chromadb.PersistentClient(path=str(self.db_path))

        return client.get_or_create_collection(
            name=self.collection_name,
        )

    def fill_collection(self,documents: list[str] ):
        collection = self.get_collection()

        embeddings = self.embedding_model_handler.get_embedding_of_current_embedding_model(documents)

        ids = [
            str(i)
            for i in range(len(documents))
        ]

        collection.add(
            ids=ids,
            documents=documents,
            embeddings=embeddings,
        )

    def get_top_k_results(self, query):

        try:
            collection = self.get_collection()

            embedded_query = self.embedding_model_handler.get_embedding_of_current_embedding_model([query])[0]

            results = collection.query(
                query_embeddings=[embedded_query],
                n_results=self.top_k,
            )

            return results["documents"][0]

        except Exception as e:
            print(traceback.format_exc())
            print(e)
            return []

    def fill_collection_with_meta_data(self, document_with_metadata: list[dict]):
        try:

            collection = self.get_collection()

            only_documents = [item.get("content") for item in document_with_metadata]

            only_metadata = [item.get("meta_data") for item in document_with_metadata]



            for start in range(0, len(only_documents), self.batch_size):

                batch = only_documents[start:start + self.batch_size]


                embeddings = self.embedding_model_handler.get_embedding_of_current_embedding_model(batch)

                ids = [
                        str(i)
                        for i in range(start, start + len(batch))
                    ]

                batch_metadata = only_metadata[start:start + self.batch_size]

                collection.add(
                        ids=ids,
                        documents=batch,
                        embeddings=embeddings,
                        metadatas=batch_metadata
                )

        except Exception as e:
            print(traceback.format_exc())

    def get_top_k_results_only_meta_data(self, query):
        try:
            collection = self.get_collection()

            embedded_query = self.embedding_model_handler.get_embedding_of_current_embedding_model([query])[0]

            results = collection.query(
                        query_embeddings=[embedded_query],
                        n_results=self.top_k
                    )

            documents = results["documents"][0]
            metadatas = results["metadatas"][0]
            return list(zip(documents, metadatas))

        except Exception as e:
                print(traceback.format_exc())
                return []



        except Exception as e:
            print(traceback.format_exc())



