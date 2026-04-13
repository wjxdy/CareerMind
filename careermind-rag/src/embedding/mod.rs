pub mod gemini;
pub mod qwen;

pub use gemini::GeminiClient;
pub use qwen::QwenClient;

use crate::error::Result;

#[derive(Clone)]
pub enum EmbeddingClient {
    Gemini(GeminiClient),
    Qwen(QwenClient),
}

impl EmbeddingClient {
    pub async fn embed_query(&self, text: &str) -> Result<Vec<f32>> {
        match self {
            EmbeddingClient::Gemini(c) => c.embed_query(text).await,
            EmbeddingClient::Qwen(c) => c.embed_query(text).await,
        }
    }

    pub async fn embed_batch(&self, texts: &[String], task_type: &str) -> Result<Vec<Vec<f32>>> {
        match self {
            EmbeddingClient::Gemini(c) => c.embed_batch(texts, task_type).await,
            EmbeddingClient::Qwen(c) => c.embed_batch(texts, task_type).await,
        }
    }
}
