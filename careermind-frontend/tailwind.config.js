/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  darkMode: ['class', 'html[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        page:     'var(--bg-page)',
        card:     'var(--bg-card)',
        elevated: 'var(--bg-elevated)',
        inset:    'var(--bg-inset)',
        border:   { subtle: 'var(--border-subtle)', emphasis: 'var(--border-emphasis)', strong: 'var(--border-strong)' },
        text:     { primary: 'var(--text-primary)', secondary: 'var(--text-secondary)', muted: 'var(--text-muted)', inverse: 'var(--text-inverse)' },
        accent:   { DEFAULT: 'var(--accent)', hover: 'var(--accent-hover)', dim: 'var(--accent-dim)', contrast: 'var(--accent-contrast)' },
        success:  'var(--success)',
        warning:  'var(--warning)',
        danger:   'var(--danger)',
        agent:    { DEFAULT: 'var(--agent)', dim: 'var(--agent-dim)' },
      },
      borderRadius: { sm: 'var(--radius-sm)', md: 'var(--radius-md)', lg: 'var(--radius-lg)', full: 'var(--radius-full)' },
      boxShadow:    { sm: 'var(--shadow-sm)', md: 'var(--shadow-md)', lg: 'var(--shadow-lg)' },
      fontFamily:   { sans: 'var(--font-sans)', mono: 'var(--font-mono)', 'serif-zh': 'var(--font-serif-zh)' },
      transitionTimingFunction: { standard: 'var(--ease-standard)', emphasized: 'var(--ease-emphasized)' },
      transitionDuration:       { fast: '120ms', base: '240ms', slow: '480ms' },
    },
  },
  plugins: [],
}
