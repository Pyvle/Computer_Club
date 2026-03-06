import { useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Button, Result } from 'antd'
import { useAuth } from '../contexts/AuthContext'

export default function AdminGuard() {
  const { user, loadContext } = useAuth()
  const location = useLocation()

  const [contextLoaded, setContextLoaded] = useState(!!user)
  const [loadError, setLoadError] = useState(false)

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) { setContextLoaded(true); return }
    if (user) { setContextLoaded(true); return }

    loadContext()
      .catch(() => setLoadError(true))
      .finally(() => setContextLoaded(true))
  }, [])

  if (!contextLoaded) return null

  // apiClient мог очистить токен при failed refresh — проверяем повторно
  if (!localStorage.getItem('accessToken')) return <Navigate to="/login" replace />

  if (!user) {
    // Токен есть, но контекст не загрузился (сетевая ошибка / сервер недоступен).
    // НЕ редиректим на /login — иначе возникнет петля.
    if (loadError) {
      return (
        <Result
          status="500"
          title="Не удалось подключиться к серверу"
          subTitle="Проверьте соединение и попробуйте снова."
          extra={<Button type="primary" onClick={() => window.location.reload()}>Повторить</Button>}
        />
      )
    }
    return <Navigate to="/login" replace />
  }

  if (location.pathname.startsWith('/admin/platform') && user.globalRole !== 'GLOBAL_ADMIN') {
    return <Navigate to="/admin/my-clubs" replace />
  }

  return <Outlet />
}
