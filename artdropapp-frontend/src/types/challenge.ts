export type ChallengeKind = 'FEATURED' | 'OPEN'
export type ChallengeStatus = 'UPCOMING' | 'ACTIVE' | 'ENDED'

export interface SubmissionThumbnail {
  submissionId: number
  artworkId: number
  title: string
  imageUrl: string
  imageAlt: string
  artistDisplayName: string | null
  artistSlug: string | null
}

export interface Challenge {
  id: number
  title: string
  description: string | null
  quote: string | null
  kind: ChallengeKind | null
  status: ChallengeStatus | null
  theme: string | null
  coverImageUrl: string | null
  startsAt: string | null
  endsAt: string | null
  submissionCount: number
  submissions: SubmissionThumbnail[]
}
