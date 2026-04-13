use crate::error::{AppError, Result};
use reqwest::Client;
use serde::{Deserialize, Serialize};

#[derive(Clone)]
pub struct QwenClient {
    api_key: String,
    base_url: String,
    model: String,
    dimension: usize,
    client: Client,
}

#[derive(Debug, Serialize)]
struct EmbeddingRequest {
    model: String,
    input: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    dimensions: Option<usize>,
    #[serde(skip_serializing_if = "Option::is_none")]
    encoding_format: Option<String>,
}

#[derive(Debug, Deserialize)]
struct EmbeddingResponse {
    data: Vec<EmbeddingData>,
    usage: Option<Usage>,
}

#[derive(Debug, Deserialize)]
struct EmbeddingData {
    embedding: Vec<f32>,
    index: usize,
}

#[derive(Debug, Deserialize)]
struct Usage {
    prompt_tokens: usize,
    total_tokens: usize,
}

impl QwenClient {
    pub fn new(api_key: String, base_url: String, model: String, dimension: usize) -> Self {
        Self {
            api_key,
            base_url,
            model,
            dimension,
            client: Client::new(),
        }
    }

    pub async fn embed_query(&self, text: &str) -> Result<Vec<f32>> {
        self.embed(&[text.to_string()]).await
            .map(|mut v| v.pop().unwrap_or_default())
    }

    pub async fn embed_document(&self, text: &str) -> Result<Vec<f32>> {
        self.embed_query(text).await
    }

    pub async fn embed_batch(&self, texts: &[String], _task_type: &str) -> Result<Vec<Vec<f32>>> {
        self.embed(texts).await
    }

    async fn embed(&self, texts: &[String]) -> Result<Vec<Vec<f32>>> {
        let url = format!("{}/embeddings", self.base_url.trim_end_matches('/'));
        let mut all_embeddings = Vec::with_capacity(texts.len());

        // Qwen text-embedding-v3 has a max batch size of 10 in OpenAI-compatible mode
        for chunk in texts.chunks(10) {
            let request = EmbeddingRequest {
                model: self.model.clone(),
                input: chunk.to_vec(),
                dimensions: Some(self.dimension),
                encoding_format: Some("float".to_string()),
            };

            let mut retries = 0;
            let max_retries = 3;

            let response = loop {
                let resp = self
                    .client
                    .post(&url)
                    .header("Authorization", format!("Bearer {}", self.api_key))
                    .json(&request)
                    .send()
                    .await
                    .map_err(|e| AppError::Embedding(format!("Qwen request failed: {}", e)))?;

                let status = resp.status();
                if status.is_success() {
                    break resp;
                }

                let error_text = resp
                    .text()
                    .await
                    .unwrap_or_else(|_| "Unknown error".to_string());

                if status.as_u16() == 429 && retries < max_retries {
                    retries += 1;
                    let delay = std::time::Duration::from_secs(2u64.pow(retries));
                    tracing::warn!(
                        "Qwen rate limit hit, retrying in {:?} (attempt {}/{})",
                        delay,
                        retries,
                        max_retries
                    );
                    tokio::time::sleep(delay).await;
                    continue;
                }

                return Err(AppError::Embedding(format!(
                    "Qwen API error (status {}): {}",
                    status, error_text
                )));
            };

            let result: EmbeddingResponse = response
                .json()
                .await
                .map_err(|e| AppError::Embedding(format!("Qwen parse error: {}", e)))?;

            if result.data.len() != chunk.len() {
                return Err(AppError::Embedding(format!(
                    "Qwen embedding count mismatch: expected {}, got {}",
                    chunk.len(),
                    result.data.len()
                )));
            }

            // Ensure results are in the correct order by index (index is within the batch)
            let mut embeddings = vec![Vec::new(); chunk.len()];
            for item in result.data {
                if item.index < chunk.len() {
                    embeddings[item.index] = item.embedding;
                }
            }
            all_embeddings.extend(embeddings);
        }

        Ok(all_embeddings)
    }
}
