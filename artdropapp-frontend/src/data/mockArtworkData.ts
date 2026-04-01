import type { Artwork } from '../types/artwork'

export const mockArtworks: Artwork[] = [
  {
    title: 'Morning Harbor',
    medium: 'Oil on canvas',
    tags: ['landscape', 'maritime', 'warm'],
    publishedAt: '2024-06-15T10:30:00',
    likeCount: 142,
    commentCount: 18,
  },
  {
    title: 'Urban Lines',
    medium: 'Digital print',
    tags: ['abstract', 'architecture', 'monochrome'],
    publishedAt: '2024-08-02T14:00:00',
    likeCount: 89,
    commentCount: 7,
  },
  {
    title: 'Ceramic Vessel Study',
    medium: 'Stoneware',
    tags: ['sculpture', 'minimal', 'earth tones'],
    publishedAt: '2025-01-20T09:15:00',
    likeCount: 56,
    commentCount: 12,
  },
  {
    title: 'Night Bloom',
    medium: 'Watercolor',
    tags: ['floral', 'nocturnal'],
    publishedAt: '2025-03-01T16:45:00',
    likeCount: 203,
    commentCount: 31,
  },
]
