use crate::{
    error::{AppError, Result},
    models::*,
    services::KbService,
};
use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    response::Json,
    routing::{get, post},
    Router,
};
use serde::Deserialize;
use std::sync::Arc;

#[derive(Deserialize)]
struct ListParams {
    #[serde(rename = "type")]
    kb_type: Option<String>,
    owner_id: Option<i64>,
    #[serde(default = "default_page")]
    page: i32,
    #[serde(default = "default_size")]
    size: i32,
}

fn default_page() -> i32 { 1 }
fn default_size() -> i32 { 20 }

pub fn router(kb_service: KbService) -> Router {
    Router::new()
        .route("/api/kb", post(create_kb).get(list_kb))
        .route("/api/kb/:id", get(get_kb).delete(delete_kb))
        .with_state(Arc::new(kb_service))
}

async fn create_kb(
    State(service): State<Arc<KbService>>,
    Json(req): Json<CreateKbRequest>,
) -> Result<Json<KnowledgeBase>> {
    let owner_id: Option<i64> = None;
    let kb = service.create(req, owner_id).await?;
    Ok(Json(kb))
}

async fn list_kb(
    State(service): State<Arc<KbService>>,
    Query(params): Query<ListParams>,
) -> Result<Json<KbListResponse>> {
    let list = service.list(
        params.kb_type.as_deref(),
        params.owner_id,
        params.page,
        params.size,
    ).await?;
    Ok(Json(list))
}

async fn get_kb(
    State(service): State<Arc<KbService>>,
    Path(id): Path<i64>,
) -> Result<Json<KnowledgeBase>> {
    service.get_by_id(id)
        .await
        .map(Json)
        .ok_or_else(|| AppError::NotFound(format!("Knowledge base {} not found", id)))
}

async fn delete_kb(
    State(service): State<Arc<KbService>>,
    Path(id): Path<i64>,
) -> Result<StatusCode> {
    service.delete(id).await?;
    Ok(StatusCode::NO_CONTENT)
}
