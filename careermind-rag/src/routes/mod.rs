pub mod kb;
pub mod document;
pub mod health;
pub mod query;

pub use kb::router as kb_router;
pub use document::router as doc_router;
pub use health::router as health_router;
pub use query::router as query_router;
