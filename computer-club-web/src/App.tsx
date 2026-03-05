import { App as AntApp, ConfigProvider } from 'antd'
import ruRU from 'antd/locale/ru_RU'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import PlatformLayout from './components/PlatformLayout'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/login/LoginPage'
import ClubApplicationsPage from './pages/club-applications/ClubApplicationsPage'
import GlobalCatalogPage from './pages/global-catalog/GlobalCatalogPage'
import UsersPage from './pages/users/UsersPage'

const Placeholder = ({ name }: { name: string }) => (
  <div style={{ padding: 24, color: '#999' }}>{name} — в разработке</div>
)

export default function App() {
  return (
    <ConfigProvider locale={ruRU}>
      <AntApp>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<ProtectedRoute />}>
                {/* Платформенная панель (GLOBAL_ADMIN) */}
                <Route element={<PlatformLayout />}>
                  <Route path="/admin/platform/applications" element={<ClubApplicationsPage />} />
                  <Route path="/admin/platform/catalog" element={<GlobalCatalogPage />} />
                  <Route path="/admin/platform/users" element={<UsersPage />} />
                </Route>
                {/* Будущие разделы */}
                <Route path="/admin/onboarding" element={<Placeholder name="Онбординг" />} />
                <Route path="/admin/club/:clubId/*" element={<Placeholder name="Панель клуба" />} />
              </Route>
              <Route path="/" element={<Navigate to="/login" replace />} />
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </AntApp>
    </ConfigProvider>
  )
}
