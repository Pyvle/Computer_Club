export type GlobalRole = 'USER' | 'GLOBAL_ADMIN'

export type ClubApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

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
