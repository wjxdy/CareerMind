use crate::error::Result;

pub mod html;
pub mod markdown;
pub mod pdf;
pub mod word;

pub trait DocumentParser {
    fn parse(&self, content: &[u8]) -> Result<String>;
}

pub fn get_parser(file_type: &str) -> Option<Box<dyn DocumentParser>> {
    match file_type {
        "PDF" => Some(Box::new(pdf::PdfParser)),
        "WORD" => Some(Box::new(word::WordParser)),
        "MARKDOWN" => Some(Box::new(markdown::MarkdownParser)),
        "HTML" => Some(Box::new(html::HtmlParser)),
        "TXT" => Some(Box::new(TextParser)),
        _ => None,
    }
}

pub struct TextParser;

impl DocumentParser for TextParser {
    fn parse(&self, content: &[u8]) -> Result<String> {
        String::from_utf8(content.to_vec())
            .map_err(|e| crate::error::AppError::DocumentParse(format!("Invalid UTF-8: {}", e)))
    }
}
