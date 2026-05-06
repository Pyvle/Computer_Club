import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

export default function AdminRedirect() {
  const { user, loadContext } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    async function redirect() {
      try {
        const ctx = user ?? await loadContext()
        if (ctx.globalRole === 'GLOBAL_ADMIN') {
          navigate('/admin/platform/applications', { replace: true })
        } else {
          navigate('/admin/my-clubs', { replace: true })
        }
      } catch {
        navigate('/login', { replace: true })
      }
    }
    redirect()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return null
}
