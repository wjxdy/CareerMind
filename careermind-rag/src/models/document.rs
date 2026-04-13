use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct Document {
    pub id: i64,
    pub kb_id: i64,
    pub filename: String,
    pub file_type: String,
    pub file_size: i64,
    pub storage_path: String,
    pub status: String,
    pub chunk_count: i32,
    pub error_message: Option<String>,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
pub struct DocumentResponse {
    pub id: i64,
    pub filename: String,
    pub file_type: String,
    pub file_size: i64,
    pub status: String,
    pub chunk_count: i32,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
pub struct DocumentListResponse {
    pub items: Vec<DocumentResponse>,
    pub total: i64,
    pub page: i32,
    pub size: i32,
}

#[derive(Debug, Clone)]
pub enum FileType {
    Pdf,
    Word,
    Excel,
    Ppt,
    Txt,
    Markdown,
    Html,
}

impl FileType {
    pub fn from_extension(ext: &str) -> Option<Self> {
        match ext.to_lowercase().as_str() {
            "pdf" => Some(Self::Pdf),
            "docx" | "doc" => Some(Self::Word),
            "xlsx" | "xls" => Some(Self::Excel),
            "pptx" | "ppt" => Some(Self::Ppt),
            "txt" => Some(Self::Txt),
            "md" | "markdown" => Some(Self::Markdown),
            "html" | "htm" => Some(Self::Html),
            _ => None,
        }
    }

    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Pdf => "PDF",
            Self::Word => "WORD",
            Self::Excel => "EXCEL",
            Self::Ppt => "PPT",
            Self::Txt => "TXT",
            Self::Markdown => "MARKDOWN",
            Self::Html => "HTML",
        }
    }
}
