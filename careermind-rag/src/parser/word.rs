use super::DocumentParser;
use crate::error::{AppError, Result};
use std::io::Cursor;

pub struct WordParser;

impl DocumentParser for WordParser {
    fn parse(&self, content: &[u8]) -> Result<String> {
        let cursor = Cursor::new(content);
        let doc = docx_rs::read_docx(cursor)
            .map_err(|e| AppError::DocumentParse(format!("Word parse error: {:?}", e)))?;

        let mut text = String::new();
        for child in doc.document.children {
            if let docx_rs::document::DocumentChild::Paragraph(p) = child {
                for child in p.children {
                    if let docx_rs::paragraph::ParagraphChild::Run(r) = child {
                        for child in r.children {
                            if let docx_rs::run::RunChild::Text(t) = child {
                                text.push_str(&t.text);
                            }
                        }
                    }
                }
                text.push('\n');
            }
        }

        Ok(text)
    }
}
