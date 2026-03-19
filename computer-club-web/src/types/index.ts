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
  isActive: boolean
}

export interface UpdateProductRequest {
  categoryId: number
  title: string
  description?: string
  isActive: boolean
}

export interface AdminUserResponse {
  id: number
  phone: string | null
  isActive: boolean
  globalRole: GlobalRole
  hasPassword: boolean
  createdAt: string
  updatedAt: string
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
