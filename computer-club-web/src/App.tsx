import { App as AntApp, ConfigProvider } from 'antd'
import ruRU from 'antd/locale/ru_RU'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { antdTheme } from './theme/antdTheme'
import './theme/base.css'
import { AuthProvider } from './contexts/AuthContext'
import { ClientProvider } from './contexts/ClientContext'
import PublicLayout from './components/PublicLayout'
import PlatformLayout from './components/PlatformLayout'
import OwnerLayout from './components/OwnerLayout'
import AdminGuard from './components/AdminGuard'
import ClientLayout from './components/ClientLayout'
import ClientAuthGuard from './components/ClientAuthGuard'
import LoginPage from './pages/login/LoginPage'
import AdminRedirect from './pages/admin/AdminRedirect'
import MyClubsPage from './pages/my-clubs/MyClubsPage'
import ClubCatalogPage from './pages/club-catalog/ClubCatalogPage'
import ClubSeatsPage from './pages/club-seats/ClubSeatsPage'
import ClubFloorplansPage from './pages/club-floorplans/ClubFloorplansPage'
import ClubStaffPage from './pages/club-staff/ClubStaffPage'
import ClubBookingsPage from './pages/club-bookings/ClubBookingsPage'
import ClubBookingDetailPage from './pages/club-booking-detail/ClubBookingDetailPage'
import ClubPurchasesPage from './pages/club-purchases/ClubPurchasesPage'
import ClubPurchaseDetailPage from './pages/club-purchase-detail/ClubPurchaseDetailPage'
import ClubDashboardPage from './pages/club-dashboard/ClubDashboardPage'
import ClubSettingsPage from './pages/club-settings/ClubSettingsPage'
import ClubAuditPage from './pages/club-audit/ClubAuditPage'
import ClubUserBlocksPage from './pages/club-user-blocks/ClubUserBlocksPage'
import ClubUsersPage from './pages/club-users/ClubUsersPage'
import ClubUserDetailPage from './pages/club-users/ClubUserDetailPage'
import ClubApplicationsPage from './pages/club-applications/ClubApplicationsPage'
import GlobalCatalogPage from './pages/global-catalog/GlobalCatalogPage'
import UsersPage from './pages/users/UsersPage'
import UserDetailPage from './pages/users/UserDetailPage'
import ClubTimePackagesPage from './pages/club-time-packages/ClubTimePackagesPage'
import GlobalClubsPage from './pages/global-clubs/GlobalClubsPage'
import ClubMessagesPage from './pages/club-messages/ClubMessagesPage'
import ClubsListPage from './pages/client/clubs/ClubsListPage'
import ClubDetailsPage from './pages/client/clubs/ClubDetailsPage'
import ReportProblemPage from './pages/client/clubs/ReportProblemPage'
import BookingSetupPage from './pages/client/booking/BookingSetupPage'
import BookingSeatsPage from './pages/client/booking/BookingSeatsPage'
import ShopPage from './pages/client/shop/ShopPage'
import CartPage from './pages/client/cart/CartPage'
import HistoryPage from './pages/client/history/HistoryPage'
import PurchaseDetailPage from './pages/client/history/PurchaseDetailPage'
import ProfilePage from './pages/client/profile/ProfilePage'

const Placeholder = ({ name }: { name: string }) => (
  <div style={{ padding: 24, color: '#999' }}>{name} — в разработке</div>
)

export default function App() {
  return (
    <ConfigProvider locale={ruRU} theme={antdTheme}>
      <AntApp>
        <AuthProvider>
          <ClientProvider>
            <BrowserRouter>
              <Routes>
                {/* Публичная зона */}
                <Route element={<PublicLayout />}>
                  <Route path="/login" element={<LoginPage />} />
                </Route>

                {/* Клиентская зона */}
                <Route element={<ClientLayout />}>
                  {/* Публичные клиентские страницы */}
                  <Route path="/" element={<ClubsListPage />} />
                  <Route path="/clubs" element={<ClubsListPage />} />
                  <Route path="/clubs/:clubId" element={<ClubDetailsPage />} />
                  <Route path="/clubs/:clubId/shop" element={<ShopPage />} />

                  {/* Защищённые клиентские страницы */}
                  <Route element={<ClientAuthGuard />}>
                    <Route path="/clubs/:clubId/report" element={<ReportProblemPage />} />
                    <Route path="/clubs/:clubId/booking" element={<BookingSetupPage />} />
                    <Route path="/clubs/:clubId/booking/seats" element={<BookingSeatsPage />} />
                    <Route path="/clubs/:clubId/cart" element={<CartPage />} />
                    <Route path="/history" element={<HistoryPage />} />
                    <Route path="/history/:purchaseId" element={<PurchaseDetailPage />} />
                    <Route path="/profile" element={<ProfilePage />} />
                  </Route>
                </Route>

                {/* Административная зона */}
                <Route element={<AdminGuard />}>
                  <Route path="/admin" element={<AdminRedirect />} />

                  {/* Платформенная панель (GLOBAL_ADMIN) */}
                  <Route element={<PlatformLayout />}>
                    <Route path="/admin/platform/applications" element={<ClubApplicationsPage />} />
                    <Route path="/admin/platform/clubs" element={<GlobalClubsPage />} />
                    <Route path="/admin/platform/catalog" element={<GlobalCatalogPage />} />
                    <Route path="/admin/platform/users" element={<UsersPage />} />
                    <Route path="/admin/platform/users/:userId" element={<UserDetailPage />} />
                  </Route>

                  {/* Панель партнёра (OWNER/ADMIN) */}
                  <Route element={<OwnerLayout />}>
                    <Route path="/admin/my-clubs" element={<MyClubsPage />} />
                    <Route path="/admin/club/:clubId/dashboard" element={<ClubDashboardPage />} />
                    <Route path="/admin/club/:clubId/catalog" element={<ClubCatalogPage />} />
                    <Route path="/admin/club/:clubId/seats" element={<ClubSeatsPage />} />
                    <Route path="/admin/club/:clubId/floorplans" element={<ClubFloorplansPage />} />
                    <Route path="/admin/club/:clubId/staff" element={<ClubStaffPage />} />
                    <Route path="/admin/club/:clubId/bookings" element={<ClubBookingsPage />} />
                    <Route path="/admin/club/:clubId/bookings/:bookingId" element={<ClubBookingDetailPage />} />
                    <Route path="/admin/club/:clubId/purchases" element={<ClubPurchasesPage />} />
                    <Route path="/admin/club/:clubId/purchases/:purchaseId" element={<ClubPurchaseDetailPage />} />
                    <Route path="/admin/club/:clubId/user-blocks" element={<ClubUserBlocksPage />} />
                    <Route path="/admin/club/:clubId/users" element={<ClubUsersPage />} />
                    <Route path="/admin/club/:clubId/users/:userId" element={<ClubUserDetailPage />} />
                    <Route path="/admin/club/:clubId/audit" element={<ClubAuditPage />} />
                    <Route path="/admin/club/:clubId/settings" element={<ClubSettingsPage />} />
                    <Route path="/admin/club/:clubId/time-packages" element={<ClubTimePackagesPage />} />
                    <Route path="/admin/club/:clubId/messages" element={<ClubMessagesPage />} />
                    <Route path="/admin/club/:clubId/*" element={<Placeholder name="Панель клуба" />} />
                  </Route>
                </Route>

                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </BrowserRouter>
          </ClientProvider>
        </AuthProvider>
      </AntApp>
    </ConfigProvider>
  )
}
