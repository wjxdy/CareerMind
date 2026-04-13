use super::DocumentParser;
use crate::error::{AppError, Result};

pub struct PdfParser;

impl DocumentParser for PdfParser {
    fn parse(&self, content: &[u8]) -> Result<String> {
        pdf_extract::extract_text_from_mem(content)
            .map_err(|e| AppError::DocumentParse(format!("PDF parse error: {}", e)))
    }
}
