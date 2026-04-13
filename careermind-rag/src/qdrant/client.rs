use crate::error::{AppError, Result};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::json;

pub struct QdrantClient {
    base_url: String,
    collection: String,
    client: Client,
}

#[derive(Debug, Serialize)]
struct Point {
    id: String,
    vector: Vec<f32>,
    payload: serde_json::Value,
}

#[derive(Debug, Deserialize)]
struct SearchResponse {
    result: Vec<ScoredPoint>,
}

#[derive(Debug, Deserialize)]
struct ScoredPoint {
    id: String,
    score: f32,
    payload: serde_json::Value,
}

impl QdrantClient {
    pub fn new(base_url: String, collection: String) -> Self {
        Self {
            base_url,
            collection,
            client: Client::new(),
        }
    }

    pub async fn ensure_collection(&self, dimension: usize) -> Result<()> {
        let url = format!("{}/collections/{}?timeout=60", self.base_url, self.collection);

        // Check if collection exists
        let response = self.client.get(&url).send().await
            .map_err(|e| AppError::Qdrant(format!("Connection error: {}", e)))?;

        if response.status().is_success() {
            return Ok(());
        }

        // Create collection
        let create_url = format!("{}/collections/{}", self.base_url, self.collection);
        let body = json!({
            "vectors": {
                "size": dimension,
                "distance": "Cosine"
            }
        });

        let response = self.client
            .put(&create_url)
            .json(&body)
            .send()
            .await
            .map_err(|e| AppError::Qdrant(format!("Create collection failed: {}", e)))?;

        if !response.status().is_success() {
            let error = response.text().await.unwrap_or_default();
            return Err(AppError::Qdrant(format!("Create collection failed: {}", error)));
        }

        Ok(())
    }

    pub async fn upsert_points(&self, points: Vec<(String, Vec<f32>, serde_json::Value)>) -> Result<()> {
        let url = format!(
            "{}/collections/{}/points?wait=true",
            self.base_url, self.collection
        );

        let points: Vec<Point> = points.into_iter()
            .map(|(id, vector, payload)| Point { id, vector, payload })
            .collect();

        let body = json!({ "points": points });

        let response = self.client
            .put(&url)
            .json(&body)
            .send()
            .await
            .map_err(|e| AppError::Qdrant(format!("Upsert failed: {}", e)))?;

        if !response.status().is_success() {
            let error = response.text().await.unwrap_or_default();
            return Err(AppError::Qdrant(format!("Upsert failed: {}", error)));
        }

        Ok(())
    }

    pub async fn search(
        &self,
        vector: Vec<f32>,
        filter_kb_id: i64,
        top_k: usize,
    ) -> Result<Vec<(String, f32, serde_json::Value)>> {
        let url = format!(
            "{}/collections/{}/points/search",
            self.base_url, self.collection
        );

        let body = json!({
            "vector": vector,
            "limit": top_k,
            "filter": {
                "must": [
                    {
                        "key": "kb_id",
                        "match": { "value": filter_kb_id }
                    }
                ]
            },
            "with_payload": true
        });

        let response = self.client
            .post(&url)
            .json(&body)
            .send()
            .await
            .map_err(|e| AppError::Qdrant(format!("Search failed: {}", e)))?;

        if !response.status().is_success() {
            let error = response.text().await.unwrap_or_default();
            return Err(AppError::Qdrant(format!("Search failed: {}", error)));
        }

        let result: SearchResponse = response.json().await
            .map_err(|e| AppError::Qdrant(format!("Parse error: {}", e)))?;

        Ok(result.result.into_iter()
            .map(|p| (p.id, p.score, p.payload))
            .collect())
    }

    pub async fn delete_by_kb_id(&self, kb_id: i64) -> Result<()> {
        let url = format!(
            "{}/collections/{}/points/delete?wait=true",
            self.base_url, self.collection
        );

        let body = json!({
            "filter": {
                "must": [
                    {
                        "key": "kb_id",
                        "match": { "value": kb_id }
                    }
                ]
            }
        });

        let response = self.client
            .post(&url)
            .json(&body)
            .send()
            .await
            .map_err(|e| AppError::Qdrant(format!("Delete failed: {}", e)))?;

        if !response.status().is_success() {
            let error = response.text().await.unwrap_or_default();
            return Err(AppError::Qdrant(format!("Delete failed: {}", error)));
        }

        Ok(())
    }

    pub async fn delete_by_document_id(&self, document_id: i64) -> Result<()> {
        let url = format!(
            "{}/collections/{}/points/delete?wait=true",
            self.base_url, self.collection
        );

        let body = json!({
            "filter": {
                "must": [
                    {
                        "key": "document_id",
                        "match": { "value": document_id }
                    }
                ]
            }
        });

        let response = self.client
            .post(&url)
            .json(&body)
            .send()
            .await
            .map_err(|e| AppError::Qdrant(format!("Delete failed: {}", e)))?;

        if !response.status().is_success() {
            let error = response.text().await.unwrap_or_default();
            return Err(AppError::Qdrant(format!("Delete failed: {}", error)));
        }

        Ok(())
    }
}
