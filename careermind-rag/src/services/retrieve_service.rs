use crate::{
    db::DbPool,
    embedding::GeminiClient,
    error::{AppError, Result},
    models::*,
    qdrant::QdrantClient,
};

pub struct RetrieveService {
    db: DbPool,
    qdrant: QdrantClient,
    gemini: GeminiClient,
}

impl RetrieveService {
    pub fn new(db: DbPool, qdrant: QdrantClient, gemini: GeminiClient) -> Self {
        Self { db, qdrant, gemini }
    }

    pub async fn search(
        &self,
        kb_id: i64,
        query: String,
        top_k: usize,
        score_threshold: f32,
    ) -> Result<QueryResponse> {
        // Get embedding for query
        let query_vector = self.gemini.embed_query(&query).await?;

        // Search Qdrant
        let results = self.qdrant.search(query_vector, kb_id, top_k).await?;

        // Filter by score threshold and fetch document info
        let mut retrieval_results = Vec::new();

        for (_vector_id, score, payload) in results {
            if score < score_threshold {
                continue;
            }

            let content = payload
                .get("content")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let doc_id = payload
                .get("document_id")
                .and_then(|v| v.as_i64())
                .unwrap_or(0);

            // Get document info from DB
            let doc = sqlx::query_as::<_, (String, String)>(
                "SELECT filename, file_type FROM documents WHERE id = ?"
            )
            .bind(doc_id)
            .fetch_one(&self.db.pool)
            .await
            .map_err(AppError::Database)?;

            retrieval_results.push(RetrievalResult {
                content,
                score,
                document: DocumentInfo {
                    id: doc_id,
                    filename: doc.0,
                    file_type: doc.1,
                },
                metadata: Some(payload),
            });
        }

        Ok(QueryResponse {
            query,
            results: retrieval_results,
        })
    }
}
