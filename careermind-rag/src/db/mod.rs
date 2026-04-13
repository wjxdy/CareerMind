pub mod pool;

use sqlx::MySqlPool;

#[derive(Clone)]
pub struct DbPool {
    pub pool: MySqlPool,
}

impl DbPool {
    pub async fn new(database_url: &str) -> anyhow::Result<Self> {
        let pool = sqlx::mysql::MySqlPoolOptions::new()
            .max_connections(20)
            .connect(database_url)
            .await?;

        Ok(Self { pool })
    }
}
