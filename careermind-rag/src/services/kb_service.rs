use crate::{
    db::DbPool,
    error::{AppError, Result},
    models::*,
};

pub struct KbService {
    db: DbPool,
}

impl KbService {
    pub fn new(db: DbPool) -> Self {
        Self { db }
    }

    pub async fn create(&self, req: CreateKbRequest, owner_id: Option<i64>) -> Result<KnowledgeBase> {
        let kb_type = if req.kb_type.as_str() == "PUBLIC" {
            "PUBLIC"
        } else {
            "PERSONAL"
        };

        let row = sqlx::query(
            r#"
            INSERT INTO knowledge_bases
            (name, description, kb_type, owner_user_id, chunk_size, chunk_overlap)
            VALUES (?, ?, ?, ?, ?, ?)
            "#
        )
        .bind(&req.name)
        .bind(&req.description)
        .bind(kb_type)
        .bind(owner_id)
        .bind(req.chunk_size.unwrap_or(512))
        .bind(req.chunk_overlap.unwrap_or(50))
        .execute(&self.db.pool)
        .await
        .map_err(AppError::Database)?;

        let id = row.last_insert_id() as i64;

        self.get_by_id(id).await
            .ok_or_else(|| AppError::NotFound("Knowledge base not found".to_string()))
    }

    pub async fn get_by_id(&self, id: i64) -> Option<KnowledgeBase> {
        sqlx::query_as::<_, KnowledgeBase>(
            "SELECT * FROM knowledge_bases WHERE id = ?"
        )
        .bind(id)
        .fetch_optional(&self.db.pool)
        .await
        .ok()?
    }

    pub async fn list(&self, kb_type: Option<&str>, owner_id: Option<i64>, page: i32, size: i32) -> Result<KbListResponse> {
        let offset = (page - 1) * size;

        let mut count_query = String::from("SELECT COUNT(*) FROM knowledge_bases WHERE 1=1");
        let mut query = String::from("SELECT * FROM knowledge_bases WHERE 1=1");

        if let Some(_t) = kb_type {
            query.push_str(" AND kb_type = ?");
            count_query.push_str(" AND kb_type = ?");
        }

        if owner_id.is_some() {
            query.push_str(" AND (owner_user_id = ? OR kb_type = 'PUBLIC')");
            count_query.push_str(" AND (owner_user_id = ? OR kb_type = 'PUBLIC')");
        }

        query.push_str(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

        // Get total count
        let mut count_q = sqlx::query_scalar::<_, i64>(&count_query);
        if let Some(t) = kb_type {
            count_q = count_q.bind(t);
        }
        if let Some(id) = owner_id {
            count_q = count_q.bind(id);
        }
        let total = count_q.fetch_one(&self.db.pool).await.map_err(AppError::Database)?;

        // Get paginated knowledge bases
        let mut q = sqlx::query_as::<_, KnowledgeBase>(&query);
        if let Some(t) = kb_type {
            q = q.bind(t);
        }
        if let Some(id) = owner_id {
            q = q.bind(id);
        }
        let kbs = q
            .bind(size)
            .bind(offset)
            .fetch_all(&self.db.pool)
            .await
            .map_err(AppError::Database)?;

        let mut items = Vec::new();
        for kb in kbs {
            let doc_count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM documents WHERE kb_id = ?")
                .bind(kb.id)
                .fetch_one(&self.db.pool)
                .await
                .map_err(AppError::Database)?;
            let chunk_count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM document_chunks WHERE kb_id = ?")
                .bind(kb.id)
                .fetch_one(&self.db.pool)
                .await
                .map_err(AppError::Database)?;

            items.push(KbResponse {
                id: kb.id,
                name: kb.name,
                description: kb.description,
                kb_type: kb.kb_type,
                document_count: doc_count,
                chunk_count,
                created_at: kb.created_at,
            });
        }

        Ok(KbListResponse {
            items,
            total,
            page,
            size,
        })
    }

    pub async fn delete(&self, id: i64) -> Result<()> {
        sqlx::query("DELETE FROM knowledge_bases WHERE id = ?")
            .bind(id)
            .execute(&self.db.pool)
            .await
            .map_err(AppError::Database)?;

        Ok(())
    }
}
