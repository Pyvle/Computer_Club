export type GlobalRole = 'USER' | 'GLOBAL_ADMIN'

export type ClubApplicationStatus = 'DRAFT' | 'PENDING' | 'REVISION_REQUESTED' | 'APPROVED' | 'REJECTED'

export interface ClubApplicationResponse {
  id: number
  applicantUserId: number
  clubName: string
  address: string
  locationText: string | null
  description: string | null
  status: ClubApplicationStatus
  decisionComment: string | null
  decidedByUserId: number | null
  decidedAt: string | null
  createdClubId: number | null
  createdAt: string
  updatedAt: string
}

export interface ClubApplicationDecisionRequest {
  comment?: string
  ownerUserId?: number
}

export interface ApproveClubApplicationResponse {
  applicationId: number
  createdClubId: number
  ownerUserId: number
}

export interface AuthTokensResponse {
  accessToken: string
  refreshToken: string
}

export interface AdminCategoryResponse {
  id: number
  title: string
  sortOrder: number
  isActive: boolean
}

export interface AdminProductResponse {
  id: number
  categoryId: number
  title: string
  description: string | null
  imageUrl: string | null
  isActive: boolean
}

export interface CreateCategoryRequest {
  title: string
  sortOrder: number
}

export interface UpdateCategoryRequest {
  title: string
  sortOrder: number
  isActive: boolean
}

export interface CreateProductRequest {
  categoryId: number
  title: string
  description?: string
  imageUrl?: string | null
  isActive: boolean
}

export interface UpdateProductRequest {
  categoryId: number
  title: string
  description?: string
  imageUrl?: string | null
  isActive: boolean
}

export interface GlobalClubResponse {
  id: number
  name: string
  addressShort: string
  addressFull: string
  description: string | null
  imageUrl: string | null
  isActive: boolean
  isBlocked: boolean
  blockReason: string | null
  reportsCount: number
  createdAt: string
}

export interface GlobalClubStatsResponse {
  ownersCount: number
  adminsCount: number
  totalSeats: number
  activeSeats: number
  regularSeats: number
  vipSeats: number
  floorplansTotal: number
  publishedFloorplans: number
  draftFloorplans: number
  archivedFloorplans: number
  linkedCatalogItems: number
  availableCatalogItems: number
  timePackagesTotal: number
  activeTimePackages: number
  activeBlocksCount: number
  warningsCount: number
  reportsNewCount: number
  reportsInProgressCount: number
  reportsResolvedCount: number
  bookingsTotal: number
  purchasesTotal: number
  paidRevenueRub: number
}

export interface GlobalClubPermissionOverrideResponse {
  permission: string
  granted: boolean
}

export interface GlobalClubStaffDetailsResponse {
  userId: number
  phone: string | null
  role: ClubRole
  addedAt: string
  addedByUserId: number | null
  addedByPhone: string | null
  rolePermissions: string[]
  overrides: GlobalClubPermissionOverrideResponse[]
  effectivePermissions: string[]
}

export interface GlobalClubBlockResponse {
  userId: number
  phone: string | null
  isBlocked: boolean
  reason: string | null
  blockedUntil: string | null
  blockedByUserId: number | null
  blockedByPhone: string | null
  createdAt: string
  updatedAt: string
}

export interface GlobalClubFloorplanResponse {
  id: number
  clubId: number
  name: string
  status: FloorplanStatus
  width: number
  height: number
  gridSize: number
  version: number
  itemCount: number
  data: unknown
  updatedAt: string
}

export interface GlobalClubDetailsResponse {
  id: number
  name: string
  addressShort: string
  addressFull: string
  locationText: string | null
  description: string | null
  imageUrl: string | null
  isActive: boolean
  isBlocked: boolean
  blockReason: string | null
  latitude: number | null
  longitude: number | null
  createdAt: string
  updatedAt: string
  stats: GlobalClubStatsResponse
  dashboard: ClubDashboardResponse
  staff: GlobalClubStaffDetailsResponse[]
  seats: AdminSeatResponse[]
  seatPrices: AdminSeatPriceResponse[]
  seatSpecs: SeatSpecResponse[]
  timePackages: AdminTimePackageResponse[]
  floorplans: GlobalClubFloorplanResponse[]
  catalog: AdminClubCatalogResponse
  reports: ClubUserReportResponse[]
  warnings: ClubWarningResponse[]
  blocks: GlobalClubBlockResponse[]
  bookings: AdminBookingResponse[]
  purchases: AdminPurchaseResponse[]
  audit: AuditLogResponse[]
}

export interface BlockClubRequest {
  reason: string | null
}

export interface ClubWarningResponse {
  id: number
  message: string
  createdBy: number
  createdAt: string
}

export interface AdminUserResponse {
  id: number
  phone: string | null
  isActive: boolean
  globalRole: GlobalRole
  hasPassword: boolean
  createdAt: string
  updatedAt: string
  bookingsCount: number
  purchasesCount: number
  totalSpentRub: number
  visitedClubsCount: number
  lastActivityAt: string | null
  clubRoles: UserClubRoleInfo[]
}

export interface UserPurchasePreview {
  purchaseId: number
  clubId: number
  clubName: string
  totalRub: number
  paymentStatus: string
  createdAt: string
}

export interface UserBookingPreview {
  bookingId: number
  clubId: number
  clubName: string
  startAt: string
  endAt: string
  status: string
  totalRub: number
}

export interface UserActiveBlockInfo {
  clubId: number
  clubName: string
  reason: string | null
  blockedUntil: string | null
  createdAt: string
}

export interface UserClubRoleInfo {
  clubId: number
  clubName: string
  role: string
}

export interface AdminUserDetailsResponse {
  id: number
  phone: string | null
  isActive: boolean
  globalRole: GlobalRole
  hasPassword: boolean
  createdAt: string
  updatedAt: string
  bookingsCount: number
  purchasesCount: number
  totalSpentRub: number
  visitedClubsCount: number
  lastActivityAt: string | null
  recentPurchases: UserPurchasePreview[]
  activeBlocks: UserActiveBlockInfo[]
  recentBookings: UserBookingPreview[]
  clubRoles: UserClubRoleInfo[]
}

export interface GlobalAdminUserBookingItem {
  bookingId: number
  clubId: number
  clubName: string
  startAt: string
  endAt: string
  status: string
  totalRub: number
  seatLabels: string[]
  purchaseId: number | null
}

export interface GlobalAdminUserPurchaseItem {
  purchaseId: number
  clubId: number
  clubName: string
  paymentStatus: string
  totalRub: number
  bookingTotalRub: number
  productsTotalRub: number
  createdAt: string
}

export interface GlobalAdminUserReportItem {
  reportId: number
  clubId: number
  clubName: string
  message: string
  status: ClubReportStatus
  createdAt: string
}

export interface ClubUserListItem {
  userId: number
  phone: string | null
  isActive: boolean
  firstVisitAt: string | null
  lastVisitAt: string | null
  bookingsCount: number
  purchasesCount: number
  totalSpentRub: number
  cancelledBookingsCount: number
  isBlocked: boolean
  blockedUntil: string | null
  blockReason: string | null
}

export interface ClubUserDetailResponse {
  userId: number
  phone: string | null
  isActive: boolean
  isBlocked: boolean
  blockReason: string | null
  blockedUntil: string | null
  blockedAt: string | null
  blockedByPhone: string | null
  firstVisitAt: string | null
  lastVisitAt: string | null
  bookingsCount: number
  purchasesCount: number
  totalSpentRub: number
  avgSpentRub: number
  cancelledBookingsCount: number
  totalHoursBooked: number
  favoriteSeatType: string | null
  recentBookings: ClubUserBookingItem[]
  recentPurchases: ClubUserPurchaseItem[]
  reports: ClubUserReportForDetail[]
}

export interface ClubUserBookingItem {
  bookingId: number
  startAt: string
  endAt: string
  seatLabels: string[]
  durationHours: number
  totalRub: number
  status: string
}

export interface ClubUserPurchaseItem {
  purchaseId: number
  createdAt: string
  totalRub: number
  paymentStatus: string
}

export interface ClubUserReportForDetail {
  reportId: number
  message: string
  status: string
  createdAt: string
}

export interface CreateUserRequest {
  phone: string
  password: string
  globalRole?: GlobalRole
}

export interface SetActiveRequest {
  isActive: boolean
}

export interface AdminClubCatalogProductResponse {
  productId: number
  categoryId: number
  productTitle: string
  description: string | null
  productIsActive: boolean
  isLinkedToClub: boolean
  clubPriceRub: number | null
  clubIsAvailable: boolean | null
}

export interface AdminClubCatalogResponse {
  categories: AdminCategoryResponse[]
  products: AdminClubCatalogProductResponse[]
}

export interface UpsertClubProductRequest {
  priceRub: number
  isAvailable: boolean
}

export interface AdminSeatResponse {
  id: number
  label: string
  type: 'REGULAR' | 'VIP'
  isActive: boolean
  sortOrder: number
}

export interface CreateSeatRequest {
  label: string
  type: 'REGULAR' | 'VIP'
  sortOrder?: number
}

export interface UpdateSeatRequest {
  label: string
  type: 'REGULAR' | 'VIP'
  sortOrder?: number
  isActive?: boolean
}

export type FloorplanStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface FloorplanSummaryResponse {
  id: number
  clubId: number
  name: string
  status: FloorplanStatus
  version: number
  updatedAt: string
}

export interface FloorplanResponse {
  id: number
  clubId: number
  name: string
  width: number
  height: number
  gridSize: number
  status: FloorplanStatus
  version: number
  data: unknown
  updatedAt: string
}

export interface CreateFloorplanRequest {
  name: string
  width: number
  height: number
  gridSize?: number
}

export interface UpdateFloorplanRequest {
  name: string
  width: number
  height: number
  gridSize: number
  version: number
  data: unknown
}

export interface CloneFloorplanRequest {
  name: string
}

export interface UserLookupResult {
  userId: number
  phone: string | null
  role: string   // globalRole: "USER" | "GLOBAL_ADMIN"
}

export type ClubRole = 'OWNER' | 'ADMIN'

export interface ClubStaffView {
  userId: number
  phone: string | null
  role: ClubRole
  addedAt: string | null
  addedByUserId: number | null
  addedByPhone: string | null
}

export interface PermissionOverrideView {
  permission: string
  granted: boolean
}

export interface ClubStaffPermissionsResponse {
  clubId: number
  userId: number
  role: ClubRole | null
  rolePermissions: string[]
  overrides: PermissionOverrideView[]
  effectivePermissions: string[]
}

export type BookingStatus = 'UPCOMING' | 'ACTIVE' | 'DONE' | 'CANCELED'
export type SeatType = 'REGULAR' | 'VIP'
export type PaymentStatus = 'CREATED' | 'PAID' | 'FAILED' | 'CANCELED' | 'REFUND'

export interface AdminBookingDetailResponse {
  id: number
  status: BookingStatus
  userId: number
  userPhone: string | null
  startAt: string
  endAt: string
  durationHours: number
  rateRubPerHour: number
  totalRub: number
  seats: AdminPurchaseSeatDetail[]
  purchaseId: number | null
}

export interface AdminBookingResponse {
  id: number
  userId: number
  clubId: number
  userPhone: string | null
  status: BookingStatus
  startAt: string
  endAt: string
  durationHours: number
  totalRub: number
  seatLabels: string[]
  purchaseId: number | null
}

export interface AdminPurchaseResponse {
  id: number
  userId: number
  clubId: number
  userPhone: string | null
  paymentStatus: PaymentStatus
  totalAmountRub: number
  bookingTotalRub: number
  productsTotalRub: number
  seatLabels: string[]
  productCount: number
  createdAt: string
}

export interface AdminPurchaseSeatDetail {
  seatId: number
  label: string
  type: SeatType
}

export interface AdminPurchaseBookingDetail {
  bookingId: number
  status: BookingStatus
  startAt: string
  endAt: string
  durationHours: number
  rateRubPerHour: number
  totalRub: number
  seats: AdminPurchaseSeatDetail[]
}

export interface AdminPurchaseOrderItemDetail {
  title: string
  qty: number
  priceRub: number
  subtotalRub: number
}

export interface AdminPurchaseProductOrderDetail {
  orderId: number
  totalRub: number
  items: AdminPurchaseOrderItemDetail[]
}

export interface AdminPurchaseDetailResponse {
  id: number
  userId: number
  userPhone: string | null
  clubId: number
  createdAt: string
  paymentStatus: PaymentStatus
  bookingTotalRub: number
  productsTotalRub: number
  totalRub: number
  booking: AdminPurchaseBookingDetail | null
  productOrder: AdminPurchaseProductOrderDetail | null
}

export interface DashboardBookingPreview {
  id: number
  userPhone: string | null
  startAt: string
  endAt: string
  status: BookingStatus
}

export interface DashboardPurchasePreview {
  id: number
  userPhone: string | null
  totalRub: number
  paymentStatus: PaymentStatus
  createdAt: string
}

export interface ClubSettingsResponse {
  id: number
  name: string
  addressFull: string
  addressShort: string
  locationText: string | null
  description: string | null
  imageUrl: string | null
  isActive: boolean
  latitude: number | null
  longitude: number | null
}

export interface UpdateClubSettingsRequest {
  name: string
  addressFull: string
  addressShort: string
  locationText: string | null
  description: string | null
  imageUrl: string | null
  isActive: boolean
  latitude: number | null
  longitude: number | null
}

export interface AddressSearchResult {
  addressFull: string
  addressShort: string
  latitude: number
  longitude: number
}

export interface ClubUserBlockView {
  userId: number
  phone: string | null
  isBlocked: boolean
  reason: string | null
  blockedUntil: string | null
  blockedByUserId: number | null
  blockedByPhone: string | null
  createdAt: string
  updatedAt: string
}

export interface UpsertClubUserBlockRequest {
  isBlocked: boolean
  reason?: string | null
  blockedUntil?: string | null
}

export interface AuditLogResponse {
  id: number
  createdAt: string
  actorUserId: number
  actorPhone: string | null
  action: string
  entityType: string
  entityId: string | null
  before: unknown
  after: unknown
}

export interface AdminTimePackageResponse {
  id: number
  name: string
  hours: number
  pricePerHourRub: number
  totalPriceRub: number
  isActive: boolean
  sortOrder: number
  /** "HH:mm" или null — без ограничения по времени */
  availableFrom: string | null
  availableTo: string | null
}

export interface CreateTimePackageRequest {
  name: string
  hours: number
  pricePerHourRub: number
  sortOrder?: number
  availableFrom?: string | null
  availableTo?: string | null
}

export interface UpdateTimePackageRequest {
  name: string
  hours: number
  pricePerHourRub: number
  isActive: boolean
  sortOrder: number
  availableFrom: string | null
  availableTo: string | null
}

export interface AdminSeatPriceResponse {
  seatType: string
  pricePerHourRub: number
}

export interface SpecLine {
  name: string
  value: string
}

export interface SeatSpecResponse {
  seatType: string
  title: string
  specs: SpecLine[]
}

export interface UpdateSeatSpecRequest {
  title: string
  specs: SpecLine[]
}

export interface UpsertSeatPriceRequest {
  seatType: string
  pricePerHourRub: number
}

export interface FloorplanBookingEntry {
  seatId: number
  bookingId: number
  userId: number
  userPhone: string | null
  status: 'UPCOMING' | 'ACTIVE'
  startAt: string
  endAt: string
  totalRub: number
  paymentStatus: string | null
}

// --- Клиентские типы ---

export interface TimePackageClientResponse {
  id: number
  name: string
  hours: number
  pricePerHourRub: number
  totalPriceRub: number
  availableFrom: string | null
  availableTo: string | null
}

export interface SeatPriceClientResponse {
  seatType: 'REGULAR' | 'VIP'
  pricePerHourRub: number
}

export interface SeatClientResponse {
  id: number
  label: string
  type: 'REGULAR' | 'VIP'
}

export interface ClubProductResponse {
  productId: number
  categoryId: number
  categoryTitle: string
  title: string
  description: string | null
  imageUrl: string | null
  priceRub: number
  isAvailable: boolean
}

export interface FloorplanWithAvailabilityClientResponse {
  floorplan: FloorplanResponse
  from: string
  to: string
  busySeatIds: number[]
  seats: { seatId: number; isBusy: boolean }[]
}

export interface CartBookingLineClientResponse {
  lineId: number
  startAt: string
  endAt: string
  packageHours: number | null
  seatIds: number[]
  lineTotalRub?: number | null
}

export interface CartProductLineClientResponse {
  lineId: number
  productId: number
  title: string
  qty: number
  priceRub: number
  lineTotalRub: number
}

export interface CartClientResponse {
  cartId: number
  userId: number
  clubId: number
  updatedAt: string
  bookings: CartBookingLineClientResponse[]
  products: CartProductLineClientResponse[]
}

export interface ClubProductClientResponse {
  productId: number
  categoryId: number
  categoryTitle: string
  title: string
  description: string | null
  imageUrl: string | null
  priceRub: number
  isAvailable: boolean
}

export interface CheckoutClientResponse {
  purchaseId: number
  paymentStatus: string
  bookingTotalRub: number
  productsTotalRub: number
  totalRub: number
}

export interface ClubListItemResponse {
  id: number
  name: string
  address: string
  locationText: string | null
  description: string | null
  imageUrl: string | null
  latitude: number | null
  longitude: number | null
  minPricePerHourRub: number | null
  /** Присутствует только при запросе /clubs/available (авторизованный пользователь) */
  isBlocked?: boolean
  blockReason?: string | null
}

export interface ClientPurchaseListItem {
  purchaseId: number
  clubId: number
  clubName: string
  createdAt: string
  bookingTotalRub: number
  productsTotalRub: number
  totalRub: number
  paymentStatus: PaymentStatus
}

export interface ClientBookingItem {
  bookingId: number
  startAt: string
  endAt: string
  seatIds: number[]
  seatLabels: string[]
  totalRub: number
}

export interface ClientProductItem {
  productId: number
  name: string
  qty: number
  unitRub: number
  totalRub: number
}

export interface ClientPurchaseDetails {
  purchaseId: number
  clubId: number
  clubName: string
  createdAt: string
  paymentStatus: PaymentStatus
  bookingItems: ClientBookingItem[]
  productItems: ClientProductItem[]
  bookingTotalRub: number
  productsTotalRub: number
  totalRub: number
}

export type ClubReportStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED'

export interface ClubUserReportResponse {
  id: number
  userId: number
  userPhone: string | null
  message: string
  status: ClubReportStatus
  createdAt: string
}

export interface PlatformMessageResponse {
  id: number
  message: string
  createdAt: string
}

export interface ClubDashboardResponse {
  activeBookingsCount: number
  upcomingTodayCount: number
  occupiedSeats: number
  totalSeats: number
  todayRevenueRub: number
  /** null если у запрашивающего нет прав на расширенную финансовую статистику */
  weekRevenueRub: number | null
  monthRevenueRub: number | null
  recentBookings: DashboardBookingPreview[]
  recentPurchases: DashboardPurchasePreview[]
}
