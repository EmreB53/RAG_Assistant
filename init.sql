CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE memories (
                          id SERIAL PRIMARY KEY,
                          summary_text TEXT NOT NULL,
                          embedding vector(768),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);