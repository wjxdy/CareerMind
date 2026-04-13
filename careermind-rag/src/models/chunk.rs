use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use sqlx::FromRow;

#[derive(Debug, Clone, FromRow)]
pub struct DocumentChunk {
    pub id: i64,
    pub document_id: i64,
    pub kb_id: i64,
    pub content: String,
    pub chunk_index: i32,
    pub vector_id: String,
    pub metadata: Option<Value>,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
pub struct RetrievalResult {
    pub content: String,
    pub score: f32,
    pub document: DocumentInfo,
    pub metadata: Option<Value>,
}

#[derive(Debug, Serialize)]
pub struct DocumentInfo {
    pub id: i64,
    pub filename: String,
    pub file_type: String,
}

#[derive(Debug, Deserialize)]
pub struct QueryRequest {
    pub query: String,
    #[serde(default = "default_top_k")]
    pub top_k: usize,
    #[serde(default = "default_threshold")]
    pub score_threshold: f32,
}

#[derive(Debug, Serialize)]
pub struct QueryResponse {
    pub query: String,
    pub results: Vec<RetrievalResult>,
}

fn default_top_k() -> usize { 5 }
fn default_threshold() -> f32 { 0.7 }
