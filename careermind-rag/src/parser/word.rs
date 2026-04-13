use super::DocumentParser;
use crate::error::{AppError, Result};

pub struct WordParser;

impl DocumentParser for WordParser {
    fn parse(&self, content: &[u8]) -> Result<String> {
        let doc = docx_rs::read_docx(content)
            .map_err(|e| AppError::DocumentParse(format!("Word parse error: {:?}", e)))?;

        let mut text = String::new();
        for child in doc.document.children {
            if let docx_rs::DocumentChild::Paragraph(p) = child {
                for child in p.children {
                    if let docx_rs::ParagraphChild::Run(r) = child {
                        for child in r.children {
                            if let docx_rs::RunChild::Text(t) = child {
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
