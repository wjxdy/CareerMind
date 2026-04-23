import { chromium } from 'playwright'
const b = await chromium.launch()
const p = await b.newPage({ viewport: { width: 1440, height: 900 } })

await p.goto('http://localhost:5174/', { waitUntil: 'networkidle' })
await p.screenshot({ path: '/tmp/p1-screens/home.png', fullPage: true })

await p.goto('http://localhost:5174/login', { waitUntil: 'networkidle' })
await p.screenshot({ path: '/tmp/p1-screens/login.png', fullPage: true })

// Dark mode
await p.evaluate(() => { document.documentElement.setAttribute('data-theme', 'dark') })
await p.goto('http://localhost:5174/', { waitUntil: 'networkidle' })
await p.screenshot({ path: '/tmp/p1-screens/home-dark.png', fullPage: true })

// Discussion page (no task — empty state)
await p.evaluate(() => { document.documentElement.setAttribute('data-theme', 'light') })
await p.goto('http://localhost:5174/discussions/999', { waitUntil: 'networkidle' }).catch(() => {})
await p.waitForTimeout(500)
await p.screenshot({ path: '/tmp/p1-screens/discussion-empty.png', fullPage: true })

await b.close()
console.log('OK')
