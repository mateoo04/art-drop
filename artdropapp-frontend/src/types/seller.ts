export type SellerStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED'

export interface SellerApplication {
  id: number
  userId: number
  applicant: AdminUserSummary | null
  message: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  submittedAt: string
  decidedAt: string | null
  decidedByUserId: number | null
  decisionReason: string | null
  revokedAt: string | null
  revokedByUserId: number | null
  revokeReason: string | null
  derivedSellerStatus: SellerStatus
  canReapplyAt: string | null
}

export type AdminPrimaryRole = 'USER' | 'ADMIN'

export interface AdminUserSummary {
  id: number
  username: string
  slug: string
  displayName: string
  email: string
  avatarUrl: string | null
  sellerStatus: SellerStatus
  pendingApplication: SellerApplication | null
  primaryRole: AdminPrimaryRole
  enabled: boolean
}

export interface AdminUserDetail {
  user: AdminUserSummary
  applicationHistory: SellerApplication[]
}

export interface PageResult<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}
