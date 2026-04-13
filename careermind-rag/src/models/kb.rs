use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct KnowledgeBase {
    pub id: i64,
    pub name: String,
    pub description: Option<String>,
    pub kb_type: String,
    pub owner_user_id: Option<i64>,
    pub embedding_model: String,
    pub embedding_dimension: i32,
    pub chunk_size: i32,
    pub chunk_overlap: i32,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

#[derive(Debug, Deserialize)]
pub struct CreateKbRequest {
    pub name: String,
    pub description: Option<String>,
    pub kb_type: String,
    pub chunk_size: Option<i32>,
    pub chunk_overlap: Option<i32>,
}

#[derive(Debug, Serialize)]
pub struct KbResponse {
    pub id: i64,
    pub name: String,
    pub description: Option<String>,
    pub kb_type: String,
    pub document_count: i64,
    pub chunk_count: i64,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
pub struct KbListResponse {
    pub items: Vec<KbResponse>,
    pub total: i64,
    pub page: i32,
    pub size: i32,
}
