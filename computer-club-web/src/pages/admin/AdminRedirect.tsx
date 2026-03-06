import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

export default function AdminRedirect() {
  const { user, loadContext } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    async function redirect() {
      const ctx = user ?? await loadContext()

      if (ctx.globalRole === 'GLOBAL_ADMIN') {
        navigate('/admin/platform/applications', { replace: true })
        return
      }
      navigate('/admin/my-clubs', { replace: true })
    }
    redirect()
  }, [])

  return null
}
