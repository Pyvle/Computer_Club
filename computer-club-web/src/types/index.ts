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
export type ProductOrderStatus = 'NOT_READY' | 'READY' | 'CANCELED'
export type PaymentMethod = 'CARD'
export type SeatType = 'REGULAR' | 'VIP'
export type PaymentStatus = 'CREATED' | 'PAID' | 'FAILED' | 'CANCELED' | 'REFUND'

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
  status: ProductOrderStatus
  totalRub: number
  items: AdminPurchaseOrderItemDetail[]
}

export interface AdminPurchaseDetailResponse {
  id: number
  userId: number
  userPhone: string | null
  clubId: number
  createdAt: string
  paymentMethod: PaymentMethod
  paymentStatus: PaymentStatus
  bookingTotalRub: number
  productsTotalRub: number
  totalRub: number
  booking: AdminPurchaseBookingDetail | null
  productOrder: AdminPurchaseProductOrderDetail | null
}
