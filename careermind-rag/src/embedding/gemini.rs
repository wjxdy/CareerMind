use crate::error::{AppError, Result};
use reqwest::Client;
use serde::{Deserialize, Serialize};

#[derive(Clone)]
pub struct GeminiClient {
    api_key: String,
    model: String,
    dimension: usize,
    client: Client,
}

#[derive(Debug, Serialize)]
struct EmbedRequest {
    model: String,
    content: Content,
    #[serde(skip_serializing_if = "Option::is_none")]
    task_type: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none", rename = "outputDimensionality")]
    output_dimensionality: Option<usize>,
}

#[derive(Debug, Serialize)]
struct Content {
    parts: Vec<Part>,
}

#[derive(Debug, Serialize)]
struct Part {
    text: String,
}

#[derive(Debug, Deserialize)]
struct EmbedResponse {
    embedding: Embedding,
}

#[derive(Debug, Deserialize)]
struct BatchEmbedResponse {
    embeddings: Vec<Embedding>,
}

#[derive(Debug, Deserialize)]
struct Embedding {
    values: Vec<f32>,
}

#[derive(Debug, Deserialize)]
struct GeminiErrorDetail {
    #[serde(rename = "@type")]
    _type: Option<String>,
    #[serde(rename = "retryDelay")]
    retry_delay: Option<String>,
}

#[derive(Debug, Deserialize)]
struct GeminiErrorBody {
    error: GeminiErrorInner,
}

#[derive(Debug, Deserialize)]
struct GeminiErrorInner {
    code: i32,
    message: String,
    status: String,
    details: Option<Vec<GeminiErrorDetail>>,
}

fn parse_retry_seconds(error_text: &str) -> Option<u64> {
    if let Ok(body) = serde_json::from_str::<GeminiErrorBody>(error_text) {
        for detail in body.error.details.unwrap_or_default() {
            if let Some(delay_str) = detail.retry_delay {
                // Parse "33s", "60s" etc.
                return delay_str
                    .trim_end_matches('s')
                    .parse::<u64>()
                    .ok()
                    .map(|s| s + 2); // add 2s buffer
            }
        }
    }
    None
}

impl GeminiClient {
    pub fn new(api_key: String, model: String, dimension: usize) -> Self {
        Self {
            api_key,
            model,
            dimension,
            client: Client::new(),
        }
    }

    pub async fn embed_document(&self, text: &str) -> Result<Vec<f32>> {
        self.embed(text, "RETRIEVAL_DOCUMENT").await
    }

    pub async fn embed_query(&self, text: &str) -> Result<Vec<f32>> {
        self.embed(text, "RETRIEVAL_QUERY").await
    }

    async fn embed(&self, text: &str, task_type: &str) -> Result<Vec<f32>> {
        let url = format!(
            "https://generativelanguage.googleapis.com/v1beta/models/{}:embedContent?key={}",
            self.model, self.api_key
        );

        let request = EmbedRequest {
            model: format!("models/{}", self.model),
            content: Content {
                parts: vec![Part { text: text.to_string() }],
            },
            task_type: Some(task_type.to_string()),
            output_dimensionality: Some(self.dimension),
        };

        let response = self
            .client
            .post(&url)
            .json(&request)
            .send()
            .await
            .map_err(|e| AppError::Embedding(format!("Request failed: {}", e)))?;

        if !response.status().is_success() {
            let error_text = response
                .text()
                .await
                .unwrap_or_else(|_| "Unknown error".to_string());
            return Err(AppError::Embedding(format!("API error: {}", error_text)));
        }

        let result: EmbedResponse = response
            .json()
            .await
            .map_err(|e| AppError::Embedding(format!("Parse error: {}", e)))?;

        Ok(result.embedding.values)
    }

    pub async fn embed_batch(&self, texts: &[String], task_type: &str) -> Result<Vec<Vec<f32>>> {
        let url = format!(
            "https://generativelanguage.googleapis.com/v1beta/models/{}:batchEmbedContents?key={}",
            self.model, self.api_key
        );

        let model_name = format!("models/{}", self.model);
        let mut results = Vec::with_capacity(texts.len());

        // Gemini batchEmbedContents supports up to 100 requests per call
        for chunk in texts.chunks(100) {
            let requests: Vec<EmbedRequest> = chunk
                .iter()
                .map(|text| EmbedRequest {
                    model: model_name.clone(),
                    content: Content {
                        parts: vec![Part { text: text.to_string() }],
                    },
                    task_type: Some(task_type.to_string()),
                    output_dimensionality: Some(self.dimension),
                })
                .collect();

            let body = serde_json::json!({ "requests": requests });

            let mut retries = 0;
            let max_retries = 5;
            let response = loop {
                let resp = self
                    .client
                    .post(&url)
                    .json(&body)
                    .send()
                    .await
                    .map_err(|e| AppError::Embedding(format!("Batch request failed: {}", e)))?;

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
                    let delay = parse_retry_seconds(&error_text)
                        .map(std::time::Duration::from_secs)
                        .unwrap_or_else(|| std::time::Duration::from_secs(2u64.pow(retries.min(6))));
                    tracing::warn!(
                        "Gemini rate limit hit, retrying batch in {:?} (attempt {}/{})",
                        delay,
                        retries,
                        max_retries
                    );
                    tokio::time::sleep(delay).await;
                    continue;
                }

                return Err(AppError::Embedding(format!(
                    "Batch API error (status {}): {}",
                    status, error_text
                )));
            };

            let result: BatchEmbedResponse = response
                .json()
                .await
                .map_err(|e| AppError::Embedding(format!("Batch parse error: {}", e)))?;

            if result.embeddings.len() != chunk.len() {
                return Err(AppError::Embedding(format!(
                    "Batch embedding count mismatch: expected {}, got {}",
                    chunk.len(),
                    result.embeddings.len()
                )));
            }

            results.extend(result.embeddings.into_iter().map(|e| e.values));
        }

        Ok(results)
    }
}
