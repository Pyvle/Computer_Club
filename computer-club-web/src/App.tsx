import { App as AntApp, ConfigProvider } from 'antd'
import ruRU from 'antd/locale/ru_RU'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AppLayout from './components/AppLayout'
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
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/" element={<Navigate to="/club-applications" replace />} />
                <Route path="/club-applications" element={<ClubApplicationsPage />} />
                <Route path="/global-catalog" element={<GlobalCatalogPage />} />
                <Route path="/users" element={<UsersPage />} />
              </Route>
            </Route>
          </Routes>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  )
}
