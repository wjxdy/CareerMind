use crate::{
    error::{AppError, Result},
    models::*,
    services::DocService,
};
use axum::{
    extract::{DefaultBodyLimit, Multipart, Path, Query, State},
    http::StatusCode,
    response::Json,
    routing::{delete, get, post},
    Router,
};
use serde::Deserialize;
use std::sync::Arc;

#[derive(Deserialize)]
struct ListParams {
    #[serde(default = "default_page")]
    page: i32,
    #[serde(default = "default_size")]
    size: i32,
}

fn default_page() -> i32 { 1 }
fn default_size() -> i32 { 20 }

pub fn router(doc_service: DocService, max_file_size: usize) -> Router {
    Router::new()
        .route("/api/kb/:id/documents", post(upload_document).get(list_documents))
        .route("/api/kb/:id/documents/:doc_id", delete(delete_document))
        .layer(DefaultBodyLimit::max(max_file_size))
        .with_state(Arc::new(doc_service))
}

async fn upload_document(
    State(service): State<Arc<DocService>>,
    Path(kb_id): Path<i64>,
    mut multipart: Multipart,
) -> Result<Json<Document>> {
    let mut filename = String::new();
    let mut content = Vec::new();

    while let Some(field) = multipart.next_field().await
        .map_err(|e| AppError::BadRequest(format!("Multipart error: {}", e)))?
    {
        let name = field.name().unwrap_or("").to_string();

        if name == "file" {
            filename = field.file_name().unwrap_or("unknown").to_string();
            content = field.bytes().await
                .map_err(|e| AppError::BadRequest(format!("Read error: {}", e)))?
                .to_vec();
        }
    }

    if filename.is_empty() || content.is_empty() {
        return Err(AppError::BadRequest("No file uploaded".to_string()));
    }

    let doc = service.upload(kb_id, filename, content, usize::MAX).await?;
    Ok(Json(doc))
}

async fn list_documents(
    State(service): State<Arc<DocService>>,
    Path(kb_id): Path<i64>,
    Query(params): Query<ListParams>,
) -> Result<Json<DocumentListResponse>> {
    let list = service.list(kb_id, params.page, params.size).await?;
    Ok(Json(list))
}

async fn delete_document(
    State(service): State<Arc<DocService>>,
    Path((kb_id, doc_id)): Path<(i64, i64)>,
) -> Result<StatusCode> {
    let _ = kb_id;
    service.delete(doc_id).await?;
    Ok(StatusCode::NO_CONTENT)
}
