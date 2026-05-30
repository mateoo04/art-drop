export const qk = {
  artworks: {
    all: ['artworks'] as const,
    detail: (id: number | string) => ['artworks', 'detail', String(id)] as const,
    mine: ['artworks', 'mine'] as const,
    mediums: () => ['artworks', 'mediums'] as const,
    search: (q: string) => ['artworks', 'search', q] as const,
    searchPreview: (q: string) => ['artworks', 'search-preview', q] as const,
    eligibleChallenges: (artworkId: number | string) =>
      ['artworks', 'eligible-challenges', String(artworkId)] as const,
  },
  challenges: {
    detail: (id: number | string) => ['challenges', 'detail', String(id)] as const,
    eligibleArtworks: (challengeId: number | string) =>
      ['challenges', 'eligible-artworks', String(challengeId)] as const,
    submissions: (challengeId: number | string, sort?: string) =>
      sort == null
        ? (['challenges', 'submissions', String(challengeId)] as const)
        : (['challenges', 'submissions', String(challengeId), sort] as const),
    search: (q: string) => ['challenges', 'search', q] as const,
  },
  users: {
    search: (q: string) => ['users', 'search', q] as const,
  },
  feed: {
    all: ['feed'] as const,
    home: (medium: string | null | undefined) => ['feed', 'home', medium ?? 'All'] as const,
  },
  profile: {
    all: ['profile'] as const,
    artworks: (slug: string) => ['profile', 'artworks', slug] as const,
  },
  reservations: {
    mine: ['my-reservation'] as const,
  },
} as const
