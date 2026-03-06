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
  username: string
  isActive: boolean
  globalRole: GlobalRole
  hasPassword: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  username: string
  password: string
  phone?: string
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
