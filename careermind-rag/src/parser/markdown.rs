use super::DocumentParser;
use crate::error::Result;
use pulldown_cmark::{Event, Parser};

pub struct MarkdownParser;

impl DocumentParser for MarkdownParser {
    fn parse(&self, content: &[u8]) -> Result<String> {
        let markdown = String::from_utf8_lossy(content);
        let parser = Parser::new(&markdown);

        let mut text = String::new();
        for event in parser {
            if let Event::Text(t) = event {
                text.push_str(&t);
            }
        }

        Ok(text)
    }
}
