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
    #[serde(skip_serializing_if = "Option::is_none")]
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
struct Embedding {
    values: Vec<f32>,
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

        let response = self.client
            .post(&url)
            .json(&request)
            .send()
            .await
            .map_err(|e| AppError::Embedding(format!("Request failed: {}", e)))?;

        if !response.status().is_success() {
            let error_text = response.text().await
                .unwrap_or_else(|_| "Unknown error".to_string());
            return Err(AppError::Embedding(format!("API error: {}", error_text)));
        }

        let result: EmbedResponse = response.json().await
            .map_err(|e| AppError::Embedding(format!("Parse error: {}", e)))?;

        Ok(result.embedding.values)
    }

    pub async fn embed_batch(&self, texts: &[String], task_type: &str) -> Result<Vec<Vec<f32>>> {
        let mut results = Vec::new();
        for text in texts {
            let embedding = self.embed(text, task_type).await?;
            results.push(embedding);
        }
        Ok(results)
    }
}
