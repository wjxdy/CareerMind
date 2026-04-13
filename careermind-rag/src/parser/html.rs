use super::DocumentParser;
use crate::error::Result;
use scraper::{Html, Selector};

pub struct HtmlParser;

impl DocumentParser for HtmlParser {
    fn parse(&self, content: &[u8]) -> Result<String> {
        let html = String::from_utf8_lossy(content);
        let document = Html::parse_document(&html);

        let selectors = [
            "main",
            "article",
            "[role='main']",
            ".content",
            "#content",
            "body",
        ];

        for sel_str in &selectors {
            if let Ok(selector) = Selector::parse(sel_str) {
                if let Some(elem) = document.select(&selector).next() {
                    return Ok(elem.text().collect::<Vec<_>>().join(" "));
                }
            }
        }

        Ok(document.root_element().text().collect::<Vec<_>>().join(" "))
    }
}
