import { App as AntApp, ConfigProvider } from 'antd'
import ruRU from 'antd/locale/ru_RU'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import PublicLayout from './components/PublicLayout'
import PlatformLayout from './components/PlatformLayout'
import OwnerLayout from './components/OwnerLayout'
import AdminGuard from './components/AdminGuard'
import LoginPage from './pages/login/LoginPage'
import HomePage from './pages/home/HomePage'
import AdminRedirect from './pages/admin/AdminRedirect'
import MyClubsPage from './pages/my-clubs/MyClubsPage'
import ClubCatalogPage from './pages/club-catalog/ClubCatalogPage'
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
              {/* Публичная зона */}
              <Route element={<PublicLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
              </Route>

              {/* Административная зона */}
              <Route element={<AdminGuard />}>
                <Route path="/admin" element={<AdminRedirect />} />

                {/* Платформенная панель (GLOBAL_ADMIN) */}
                <Route element={<PlatformLayout />}>
                  <Route path="/admin/platform/applications" element={<ClubApplicationsPage />} />
                  <Route path="/admin/platform/catalog" element={<GlobalCatalogPage />} />
                  <Route path="/admin/platform/users" element={<UsersPage />} />
                </Route>

                {/* Панель партнёра (OWNER/ADMIN) */}
                <Route element={<OwnerLayout />}>
                  <Route path="/admin/my-clubs" element={<MyClubsPage />} />
                  <Route path="/admin/club/:clubId/catalog" element={<ClubCatalogPage />} />
                  <Route path="/admin/club/:clubId/*" element={<Placeholder name="Панель клуба" />} />
                </Route>
              </Route>

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </AntApp>
    </ConfigProvider>
  )
}
