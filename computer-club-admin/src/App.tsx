import { App as AntApp, ConfigProvider } from 'antd'
import ruRU from 'antd/locale/ru_RU'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import ClubApplicationsPage from './pages/club-applications/ClubApplicationsPage'

const Placeholder = ({ name }: { name: string }) => (
  <div style={{ padding: 24, color: '#999' }}>{name} — в разработке</div>
)

export default function App() {
  return (
    <ConfigProvider locale={ruRU}>
      <AntApp>
        <BrowserRouter>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/club-applications" replace />} />
              <Route path="/club-applications" element={<ClubApplicationsPage />} />
              <Route path="/global-catalog" element={<Placeholder name="Глобальный каталог" />} />
              <Route path="/users" element={<Placeholder name="Пользователи" />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  )
}
