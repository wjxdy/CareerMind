interface Api {
  message: any
  dialog: any
  notification: any
  loadingBar: any
}

let api: Api | null = null

export const setDiscreteApi = (a: Api) => { api = a }

const fallback = (name: string) => (...args: any[]) => {
  console.warn(`[naive-discrete] ${name} called before installer mounted`, args)
}

const safe = <K extends keyof Api>(key: K, method: string) => {
  return (...args: any[]) => {
    if (!api) return fallback(`${String(key)}.${method}`)(...args)
    return (api[key] as any)[method]?.(...args)
  }
}

// simple proxies for common usage patterns
export const message = {
  success: (content: string, opts?: any) => safe('message', 'success')(content, opts),
  error:   (content: string, opts?: any) => safe('message', 'error')(content, opts),
  info:    (content: string, opts?: any) => safe('message', 'info')(content, opts),
  warning: (content: string, opts?: any) => safe('message', 'warning')(content, opts),
  loading: (content: string, opts?: any) => safe('message', 'loading')(content, opts),
}

export const dialog = {
  warning: (opts: any) => safe('dialog', 'warning')(opts),
  error:   (opts: any) => safe('dialog', 'error')(opts),
  info:    (opts: any) => safe('dialog', 'info')(opts),
  success: (opts: any) => safe('dialog', 'success')(opts),
  confirm: (title: string, content?: string): Promise<boolean> =>
    new Promise((resolve) => {
      if (!api) { resolve(false); return }
      api.dialog.warning({
        title,
        content: content || '',
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: () => resolve(true),
        onNegativeClick: () => resolve(false),
        onClose: () => resolve(false),
        onMaskClick: () => resolve(false),
      })
    }),
}

export const loadingBar = {
  start:  () => safe('loadingBar', 'start')(),
  finish: () => safe('loadingBar', 'finish')(),
  error:  () => safe('loadingBar', 'error')(),
}
