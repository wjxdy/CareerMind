use axum::{
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;
use thiserror::Error;

#[derive(Error, Debug)]
pub enum AppError {
    #[error("database error: {0}")]
    Database(#[from] sqlx::Error),

    #[error("Qdrant error: {0}")]
    Qdrant(String),

    #[error("Embedding API error: {0}")]
    Embedding(String),

    #[error("document parse error: {0}")]
    DocumentParse(String),

    #[error("file too large: {size} bytes, max {max} bytes")]
    FileTooLarge { size: u64, max: u64 },

    #[error("unsupported file type: {0}")]
    UnsupportedFileType(String),

    #[error("not found: {0}")]
    NotFound(String),

    #[error("bad request: {0}")]
    BadRequest(String),

    #[error("internal error: {0}")]
    Internal(String),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, code, message) = match &self {
            AppError::NotFound(_) => (
                StatusCode::NOT_FOUND,
                "NOT_FOUND",
                self.to_string(),
            ),
            AppError::BadRequest(_) | AppError::UnsupportedFileType(_) => (
                StatusCode::BAD_REQUEST,
                "BAD_REQUEST",
                self.to_string(),
            ),
            AppError::FileTooLarge { .. } => (
                StatusCode::PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                self.to_string(),
            ),
            AppError::DocumentParse(_) => (
                StatusCode::UNPROCESSABLE_ENTITY,
                "DOCUMENT_PARSE_FAILED",
                self.to_string(),
            ),
            _ => (
                StatusCode::INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal server error".to_string(),
            ),
        };

        let body = Json(json!({
            "error": {
                "code": code,
                "message": message
            }
        }));

        (status, body).into_response()
    }
}

pub type Result<T> = std::result::Result<T, AppError>;
