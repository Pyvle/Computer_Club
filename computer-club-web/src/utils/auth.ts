export interface TokenPayload {
  sub: string
  gr?: string // globalRole: GLOBAL_ADMIN | null
}

export function decodeToken(token: string): TokenPayload {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload))
  } catch {
    return { sub: '' }
  }
}

export function getCurrentUser(): TokenPayload | null {
  const token = localStorage.getItem('accessToken')
  if (!token) return null
  return decodeToken(token)
}
