use axum::{http::StatusCode, response::Json, routing::get, Router};
use serde_json::json;

pub fn router() -> Router {
    Router::new().route("/health", get(health_check))
}

async fn health_check() -> (StatusCode, Json<serde_json::Value>) {
    (
        StatusCode::OK,
        Json(json!({
            "status": "healthy",
            "version": env!("CARGO_PKG_VERSION"),
        })),
    )
}
