use anyhow::{Context, Result};

#[derive(Debug, Clone)]
pub struct Config {
    pub server_port: u16,
    pub server_host: String,
    pub database_url: String,
    pub qdrant_url: String,
    pub qdrant_collection: String,
    pub embedding_provider: String,
    pub gemini_api_key: String,
    pub gemini_model: String,
    pub gemini_dimension: usize,
    pub qwen_api_key: String,
    pub qwen_base_url: String,
    pub qwen_model: String,
    pub qwen_dimension: usize,
    pub upload_dir: String,
    pub max_file_size: usize,
}

impl Config {
    pub fn from_env() -> Result<Self> {
        dotenvy::dotenv().ok();

        let embedding_provider = std::env::var("EMBEDDING_PROVIDER")
            .unwrap_or_else(|_| "qwen".to_string())
            .to_lowercase();

        Ok(Self {
            server_port: std::env::var("RAG_PORT")
                .unwrap_or_else(|_| "3000".to_string())
                .parse()
                .context("Invalid RAG_PORT")?,
            server_host: std::env::var("RAG_HOST")
                .unwrap_or_else(|_| "0.0.0.0".to_string()),
            database_url: std::env::var("DATABASE_URL")
                .context("DATABASE_URL not set")?,
            qdrant_url: std::env::var("QDRANT_URL")
                .unwrap_or_else(|_| "http://localhost:6333".to_string()),
            qdrant_collection: std::env::var("QDRANT_COLLECTION")
                .unwrap_or_else(|_| "careermind_kb".to_string()),
            embedding_provider,
            gemini_api_key: std::env::var("GEMINI_API_KEY")
                .unwrap_or_else(|_| "".to_string()),
            gemini_model: std::env::var("GEMINI_MODEL")
                .unwrap_or_else(|_| "gemini-embedding-001".to_string()),
            gemini_dimension: std::env::var("GEMINI_DIMENSION")
                .unwrap_or_else(|_| "768".to_string())
                .parse()
                .context("Invalid GEMINI_DIMENSION")?,
            qwen_api_key: std::env::var("QWEN_API_KEY")
                .unwrap_or_else(|_| "".to_string()),
            qwen_base_url: std::env::var("QWEN_BASE_URL")
                .unwrap_or_else(|_| "https://dashscope.aliyuncs.com/compatible-mode/v1".to_string()),
            qwen_model: std::env::var("QWEN_MODEL")
                .unwrap_or_else(|_| "text-embedding-v3".to_string()),
            qwen_dimension: std::env::var("QWEN_DIMENSION")
                .unwrap_or_else(|_| "1024".to_string())
                .parse()
                .context("Invalid QWEN_DIMENSION")?,
            upload_dir: std::env::var("UPLOAD_DIR")
                .unwrap_or_else(|_| "./uploads".to_string()),
            max_file_size: std::env::var("MAX_FILE_SIZE")
                .unwrap_or_else(|_| "104857600".to_string())
                .parse()
                .context("Invalid MAX_FILE_SIZE")?,
        })
    }
}
