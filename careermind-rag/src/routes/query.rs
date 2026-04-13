use crate::{
    error::Result,
    models::*,
    services::RetrieveService,
};
use axum::{
    extract::{Path, State},
    response::Json,
    routing::post,
    Router,
};
use std::sync::Arc;

pub fn router(retrieve_service: RetrieveService) -> Router {
    Router::new()
        .route("/api/kb/:id/query", post(query_kb))
        .with_state(Arc::new(retrieve_service))
}

async fn query_kb(
    State(service): State<Arc<RetrieveService>>,
    Path(kb_id): Path<i64>,
    Json(req): Json<QueryRequest>,
) -> Result<Json<QueryResponse>> {
    let result = service.search(
        kb_id,
        req.query,
        req.top_k,
        req.score_threshold,
    ).await?;
    Ok(Json(result))
}
