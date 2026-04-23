// @ts-ignore html2pdf has no official types
import html2pdf from 'html2pdf.js'

export async function exportElementToPdf(el: HTMLElement, filename: string): Promise<void> {
  await html2pdf()
    .set({
      margin: [12, 12, 14, 12], // mm (top, right, bottom, left)
      filename,
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: {
        scale: 2,
        useCORS: true,
        backgroundColor: '#ffffff',
        letterRendering: true,
        scrollY: 0,
        windowWidth: 820,
      },
      jsPDF: {
        unit: 'mm', format: 'a4', orientation: 'portrait', compress: true,
        putOnlyUsedFonts: true,
      },
      pagebreak: {
        mode: ['css', 'legacy'],
        // 在这些元素前强制翻新页
        before: ['.pdf-break-before', '.cover', '.plans', '.ap', '.back'],
        // 这些元素内部禁止被切开
        avoid: [
          '.page-break-avoid',
          '.round',
          '.agent-row',
          '.plan',
          '.col',
          '.blind-row',
          '.r-head',
          'h1', 'h2', 'h3', 'h4', 'h5',
          'img', 'svg',
        ],
      },
    })
    .from(el)
    .save()
}
