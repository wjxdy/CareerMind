mod chunking;
mod config;
mod db;
mod embedding;
mod error;
mod models;
mod parser;
mod qdrant;
mod routes;
mod services;

use crate::{
    config::Config,
    db::DbPool,
    embedding::{EmbeddingClient, GeminiClient, QwenClient},
    qdrant::QdrantClient,
    services::{DocService, KbService, RetrieveService},
};
use axum::Router;
use std::net::SocketAddr;
use tower_http::cors::{Any, CorsLayer};
use tracing::{info, Level};
use tracing_subscriber::FmtSubscriber;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Initialize logging
    let subscriber = FmtSubscriber::builder()
        .with_max_level(Level::INFO)
        .finish();
    tracing::subscriber::set_global_default(subscriber)?;

    // Load configuration
    let config = Config::from_env()?;
    info!("Starting CareerMind RAG Service on {}:{}", config.server_host, config.server_port);

    // Initialize database
    let db = DbPool::new(&config.database_url).await?;
    info!("Database connected");

    // Initialize embedding client based on provider
    let embedding_client = match config.embedding_provider.as_str() {
        "gemini" => {
            info!("Using Gemini embedding provider");
            EmbeddingClient::Gemini(GeminiClient::new(
                config.gemini_api_key.clone(),
                config.gemini_model.clone(),
                config.gemini_dimension,
            ))
        }
        _ => {
            info!("Using Qwen embedding provider");
            EmbeddingClient::Qwen(QwenClient::new(
                config.qwen_api_key.clone(),
                config.qwen_base_url.clone(),
                config.qwen_model.clone(),
                config.qwen_dimension,
            ))
        }
    };

    let vector_dimension = match &embedding_client {
        EmbeddingClient::Gemini(_) => config.gemini_dimension,
        EmbeddingClient::Qwen(_) => config.qwen_dimension,
    };

    // Initialize Qdrant
    let qdrant = QdrantClient::new(
        config.qdrant_url.clone(),
        config.qdrant_collection.clone(),
    );
    qdrant.ensure_collection(vector_dimension).await?;
    info!("Qdrant connected");

    // Create upload directory
    tokio::fs::create_dir_all(&config.upload_dir).await?;

    // Initialize services
    let kb_service = KbService::new(db.clone());
    let doc_service = DocService::new(
        db.clone(),
        qdrant.clone(),
        embedding_client.clone(),
        config.upload_dir.clone(),
        config.max_file_size,
    );
    let retrieve_service = RetrieveService::new(db.clone(), qdrant.clone(), embedding_client.clone());

    // Build router
    let app = Router::new()
        .merge(routes::health_router())
        .merge(routes::kb_router(kb_service))
        .merge(routes::doc_router(doc_service))
        .merge(routes::query_router(retrieve_service))
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods(Any)
                .allow_headers(Any),
        );

    // Start server
    let addr = SocketAddr::from((
        config.server_host.parse::<std::net::IpAddr>()?,
        config.server_port,
    ));

    info!("Server listening on http://{}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app).await?;

    Ok(())
}
