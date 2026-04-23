// @ts-ignore html2pdf has no official types
import html2pdf from 'html2pdf.js'

export async function exportElementToPdf(el: HTMLElement, filename: string): Promise<void> {
  await html2pdf()
    .set({
      margin: [10, 10, 10, 10],
      filename,
      image: { type: 'jpeg', quality: 0.96 },
      html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff' },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
      pagebreak: { mode: ['css', 'legacy'] },
    })
    .from(el)
    .save()
}
