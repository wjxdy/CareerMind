import { chromium } from 'playwright'
const b = await chromium.launch()
const p = await b.newPage({ viewport: { width: 1440, height: 900 } })
await p.goto('http://localhost:5173/', { waitUntil: 'networkidle' })
await p.screenshot({ path: '/tmp/p1-screens/home-chat.png', fullPage: true })

await p.evaluate(() => document.documentElement.setAttribute('data-theme', 'dark'))
await p.waitForTimeout(300)
await p.screenshot({ path: '/tmp/p1-screens/home-chat-dark.png', fullPage: true })

await b.close()
console.log('OK')
