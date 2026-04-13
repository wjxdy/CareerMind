use crate::{
    chunking::chunk_text,
    db::DbPool,
    embedding::GeminiClient,
    error::{AppError, Result},
    models::*,
    parser::get_parser,
    qdrant::QdrantClient,
};
use std::path::Path;
use tokio::fs;
use uuid::Uuid;

#[derive(Clone)]
pub struct DocService {
    db: DbPool,
    qdrant: QdrantClient,
    gemini: GeminiClient,
    upload_dir: String,
    max_file_size: usize,
}

impl DocService {
    pub fn new(
        db: DbPool,
        qdrant: QdrantClient,
        gemini: GeminiClient,
        upload_dir: String,
        max_file_size: usize,
    ) -> Self {
        Self {
            db,
            qdrant,
            gemini,
            upload_dir,
            max_file_size,
        }
    }

    pub async fn upload(
        &self,
        kb_id: i64,
        filename: String,
        content: Vec<u8>,
    ) -> Result<Document> {
        if content.len() > self.max_file_size {
            return Err(AppError::FileTooLarge {
                size: content.len() as u64,
                max: self.max_file_size as u64,
            });
        }

        let ext = Path::new(&filename)
            .extension()
            .and_then(|e| e.to_str())
            .unwrap_or("");

        let file_type = FileType::from_extension(ext)
            .ok_or_else(|| AppError::UnsupportedFileType(ext.to_string()))?;

        // Save file
        let file_id = Uuid::new_v4().to_string();
        let storage_path = format!("{}/{}/{}", self.upload_dir, kb_id, file_id);
        fs::create_dir_all(Path::new(&storage_path).parent().unwrap())
            .await
            .map_err(|e| AppError::Internal(format!("Create dir failed: {}", e)))?;
        fs::write(&storage_path, &content)
            .await
            .map_err(|e| AppError::Internal(format!("Write file failed: {}", e)))?;

        // Insert document record
        let row = sqlx::query(
            "INSERT INTO documents (kb_id, filename, file_type, file_size, storage_path, status)
             VALUES (?, ?, ?, ?, ?, 'PENDING')"
        )
        .bind(kb_id)
        .bind(&filename)
        .bind(file_type.as_str())
        .bind(content.len() as i64)
        .bind(&storage_path)
        .execute(&self.db.pool)
        .await
        .map_err(AppError::Database)?;

        let doc_id = row.last_insert_id() as i64;

        // Spawn background processing so upload returns immediately
        let service = self.clone();
        let file_type_str = file_type.as_str().to_string();
        tokio::spawn(async move {
            if let Err(e) = service.process_document(doc_id, kb_id, content, &file_type_str).await {
                tracing::error!("Document {} processing failed: {}", doc_id, e);
                let _ = sqlx::query("UPDATE documents SET status = 'FAILED', error_message = ? WHERE id = ?")
                    .bind(e.to_string())
                    .bind(doc_id)
                    .execute(&service.db.pool)
                    .await;
            } else {
                tracing::info!("Document {} processed successfully", doc_id);
            }
        });

        self.get_by_id(doc_id).await
            .ok_or_else(|| AppError::NotFound("Document not found".to_string()))
    }

    async fn process_document(
        &self,
        doc_id: i64,
        kb_id: i64,
        content: Vec<u8>,
        file_type: &str,
    ) -> Result<()> {
        // Update status to PROCESSING
        sqlx::query("UPDATE documents SET status = 'PROCESSING' WHERE id = ?")
            .bind(doc_id)
            .execute(&self.db.pool)
            .await
            .map_err(AppError::Database)?;

        let parser = get_parser(file_type)
            .ok_or_else(|| AppError::UnsupportedFileType(file_type.to_string()))?;

        let text = parser.parse(&content)?;

        if text.is_empty() {
            sqlx::query("UPDATE documents SET status = 'FAILED', error_message = ? WHERE id = ?")
                .bind("No text content extracted")
                .bind(doc_id)
                .execute(&self.db.pool)
                .await
                .map_err(AppError::Database)?;
            return Err(AppError::DocumentParse("No text extracted".to_string()));
        }

        // Get KB settings
        let kb = sqlx::query_as::<_, KnowledgeBase>("SELECT * FROM knowledge_bases WHERE id = ?")
            .bind(kb_id)
            .fetch_one(&self.db.pool)
            .await
            .map_err(AppError::Database)?;

        // Chunk text
        let chunks = chunk_text(&text, kb.chunk_size as usize, kb.chunk_overlap as usize);

        // Get embeddings for all chunks
        let embeddings = self.gemini.embed_batch(&chunks, "RETRIEVAL_DOCUMENT").await?;

        // Prepare points for Qdrant
        let mut qdrant_points = Vec::new();
        let mut chunk_records = Vec::new();

        for (idx, (chunk, embedding)) in chunks.iter().zip(embeddings.iter()).enumerate() {
            let vector_id = Uuid::new_v4().to_string();

            let payload = serde_json::json!({
                "kb_id": kb_id,
                "document_id": doc_id,
                "chunk_id": idx,
                "content": chunk,
            });

            qdrant_points.push((vector_id.clone(), embedding.clone(), payload));
            chunk_records.push((chunk.clone(), idx as i32, vector_id));
        }

        // Insert into Qdrant
        self.qdrant.upsert_points(qdrant_points).await?;

        // Insert chunk records
        for (content, chunk_idx, vector_id) in chunk_records {
            sqlx::query(
                "INSERT INTO document_chunks (document_id, kb_id, content, chunk_index, vector_id)
                 VALUES (?, ?, ?, ?, ?)"
            )
            .bind(doc_id)
            .bind(kb_id)
            .bind(content)
            .bind(chunk_idx)
            .bind(vector_id)
            .execute(&self.db.pool)
            .await
            .map_err(AppError::Database)?;
        }

        // Update document status
        sqlx::query("UPDATE documents SET status = 'COMPLETED', chunk_count = ? WHERE id = ?")
            .bind(chunks.len() as i32)
            .bind(doc_id)
            .execute(&self.db.pool)
            .await
            .map_err(AppError::Database)?;

        Ok(())
    }

    pub async fn get_by_id(&self, id: i64) -> Option<Document> {
        sqlx::query_as::<_, Document>("SELECT * FROM documents WHERE id = ?")
            .bind(id)
            .fetch_optional(&self.db.pool)
            .await
            .ok()?
    }

    pub async fn list(&self, kb_id: i64, page: i32, size: i32) -> Result<DocumentListResponse> {
        let offset = (page - 1) * size;

        let items: Vec<DocumentResponse> = sqlx::query_as::<_, Document>(
            "SELECT * FROM documents WHERE kb_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?"
        )
        .bind(kb_id)
        .bind(size)
        .bind(offset)
        .fetch_all(&self.db.pool)
        .await
        .map_err(AppError::Database)?
        .into_iter()
        .map(|d| DocumentResponse {
            id: d.id,
            filename: d.filename,
            file_type: d.file_type,
            file_size: d.file_size,
            status: d.status,
            chunk_count: d.chunk_count,
            created_at: d.created_at,
        })
        .collect();

        let total: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM documents WHERE kb_id = ?")
            .bind(kb_id)
            .fetch_one(&self.db.pool)
            .await
            .map_err(AppError::Database)?;

        Ok(DocumentListResponse {
            items,
            total,
            page,
            size,
        })
    }

    pub async fn delete(&self, doc_id: i64) -> Result<()> {
        // Delete from Qdrant
        self.qdrant.delete_by_document_id(doc_id).await?;

        // Delete from MySQL (cascade will handle chunks)
        sqlx::query("DELETE FROM documents WHERE id = ?")
            .bind(doc_id)
            .execute(&self.db.pool)
            .await
            .map_err(AppError::Database)?;

        Ok(())
    }
}
