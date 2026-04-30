import { Navigate, useParams } from 'react-router-dom'
import { MasonryFeed } from '../components/home/MasonryFeed'
import { ProfileHeader } from '../components/profile/ProfileHeader'
import { useAuthPrompt } from '../contexts/AuthPromptContext'
import { useProfile } from '../hooks/useProfile'
import { getToken } from '../lib/auth'

export function ProfilePage() {
  const { slug } = useParams<{ slug: string }>()
  const { profile, artworks, inCircle, loading, error, toggleCircle } = useProfile(slug)
  const { promptToAuth } = useAuthPrompt()

  if (loading) {
    return (
      <main className="max-w-[1440px] mx-auto px-8 pt-4">
        <p className="py-24 text-center text-on-surface-variant italic" role="status">
          Loading profile…
        </p>
      </main>
    )
  }

  if (error === 'NOT_FOUND') {
    return (
      <main className="max-w-[1440px] mx-auto px-8 pt-4">
        <p className="py-24 text-center text-on-surface-variant italic">
          We couldn't find that artist.
        </p>
      </main>
    )
  }

  if (error || !profile) {
    return (
      <main className="max-w-[1440px] mx-auto px-8 pt-4">
        <p
          className="py-24 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error ?? 'Unable to load profile.'}
        </p>
      </main>
    )
  }

  if (profile.isSelf) {
    return <Navigate to="/account" replace />
  }

  const isAuthed = Boolean(getToken())
  const circleActionLabel = inCircle ? 'In your Circle' : 'Join Circle'

  return (
    <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
      <ProfileHeader
        user={profile}
        action={
          <button
            type="button"
            onClick={() => {
              if (!isAuthed) {
                promptToAuth(`follow ${profile.displayName}`)
                return
              }
              void toggleCircle()
            }}
            aria-pressed={inCircle === true}
            className={`font-label text-[11px] uppercase tracking-[0.2em] font-semibold px-6 py-3 border transition-all duration-200 ${
              isAuthed && inCircle
                ? 'bg-on-surface text-surface border-on-surface hover:opacity-90'
                : 'bg-transparent text-on-surface border-on-surface hover:bg-on-surface hover:text-surface'
            }`}
          >
            {isAuthed ? circleActionLabel : 'Join Circle'}
          </button>
        }
      />

      <section className="pt-12">
        <h2 className="font-headline text-2xl text-on-surface mb-8">Drops</h2>
        {artworks && artworks.length > 0 ? (
          <MasonryFeed artworks={artworks} />
        ) : (
          <p className="py-12 text-center text-on-surface-variant italic">
            No drops yet.
          </p>
        )}
      </section>
    </main>
  )
}
