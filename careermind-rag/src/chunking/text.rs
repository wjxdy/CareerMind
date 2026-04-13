pub fn chunk_text(text: &str, chunk_size: usize, chunk_overlap: usize) -> Vec<String> {
    if text.is_empty() {
        return vec![];
    }

    let text = text.split_whitespace().collect::<Vec<_>>().join(" ");

    if text.len() <= chunk_size {
        return vec![text];
    }

    let mut chunks = Vec::new();
    let step = chunk_size - chunk_overlap;
    let chars: Vec<char> = text.chars().collect();
    let mut start = 0;

    while start < chars.len() {
        let end = (start + chunk_size).min(chars.len());
        let chunk: String = chars[start..end].iter().collect();

        if chunk.len() > chunk_overlap / 2 {
            chunks.push(chunk);
        }

        start += step;

        if step == 0 {
            break;
        }
    }

    chunks
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_chunk_text() {
        let text = "This is a test sentence. It has multiple words. ".repeat(10);
        let chunks = chunk_text(&text, 50, 10);

        assert!(!chunks.is_empty());
        for chunk in &chunks {
            assert!(chunk.len() <= 50);
        }
    }

    #[test]
    fn test_short_text() {
        let text = "Short";
        let chunks = chunk_text(text, 100, 10);
        assert_eq!(chunks.len(), 1);
        assert_eq!(chunks[0], "Short");
    }

    #[test]
    fn test_empty_text() {
        let chunks = chunk_text("", 100, 10);
        assert!(chunks.is_empty());
    }
}
