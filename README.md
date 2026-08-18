## Local Semantic RAG Assistant

This project is the first version of a small case demonstration of a containerized AI chat assistant written in Java.
It uses RAG with vector embeddings to achieve semantic memory over past conversations. This way
it ensures context awareness in its responses.

### Tech used in implementation
* Java 26
* PostgreSQL and pgvector
* Docker compose
* Ollama
* LLMs: 'llama3.2' (Chat and Summarization), 'nomic-embed-text' (Vector Embeddings)

### Features
* Semantic Search: Uses Vector Embeddings to retrieve relevant data based on semantics and not just syntax.
* Completely Local: No data leaves the machine. Everything is in local logs and database.
* Automated Background Processing: Chats are summarized and vectors are calculated without stopping the chat flow.
* Data Infrastructure: Automated database setup via Docker Compose.

### How to Run?
1. Install [Docker](https://www.docker.com/) and [Ollama](https://ollama.com/)
2. Pull the required models:
```shell
ollama pull llama3.2
ollama pull nomic-embed-text
```
3. Run Ollama
```shell
ollama serve
```
4. Run docker-compose in the top directory
```shell
docker-compose up -d
```
5. Open your IDE of choice and run the ```main``` method in ```LocalAIChat.java```

Keep in mind that ```docker-compose down -v``` resets the memory state in the database.