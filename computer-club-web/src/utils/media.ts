function defaultMediaOrigin() {
  if (typeof window === 'undefined') return ''

  const { protocol, hostname, port } = window.location

  if (port && port !== '8080') {
    return `${protocol}//${hostname}:8080`
  }

  return ''
}

const MEDIA_ORIGIN = (import.meta.env.VITE_API_ORIGIN ?? defaultMediaOrigin()).replace(/\/$/, '')

export function resolveMediaUrl(url?: string | null) {
  if (!url) return undefined
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  return url.startsWith('/') ? `${MEDIA_ORIGIN}${url}` : url
}
