import { Navigate, Outlet, useLocation } from 'react-router-dom'

export default function ClientAuthGuard() {
  const location = useLocation()
  const token = localStorage.getItem('accessToken')

  if (!token) {
    return <Navigate to={`/login?from=${encodeURIComponent(location.pathname)}`} replace />
  }

  return <Outlet />
}
