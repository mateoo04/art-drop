import { useState } from 'react'
import { MasonryFeed } from '../components/home/MasonryFeed'
import { ProfileEditSidebar } from '../components/profile/ProfileEditSidebar'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useMyArtworks } from '../hooks/useMyArtworks'
import type { UserProfile } from '../types/user'

function formatJoinDate(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
}

function Avatar({ user }: { user: UserProfile }) {
  if (user.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt={user.displayName}
        className="w-28 h-28 rounded-full object-cover bg-surface-container-low"
      />
    )
  }
  const initial = (user.displayName || user.username || '?').slice(0, 1).toUpperCase()
  return (
    <div className="w-28 h-28 rounded-full bg-surface-container-low flex items-center justify-center font-headline text-4xl text-on-surface">
      {initial}
    </div>
  )
}

export function AccountPage() {
  const { user, loading, error, setUser } = useCurrentUser()
  const artworks = useMyArtworks(user != null)
  const [editOpen, setEditOpen] = useState(false)

  if (loading && !user) {
    return (
      <main className="max-w-[1440px] mx-auto px-8 pt-4">
        <p className="py-24 text-center text-on-surface-variant italic" role="status">
          Loading your profile…
        </p>
      </main>
    )
  }

  if (error && !user) {
    return (
      <main className="max-w-[1440px] mx-auto px-8 pt-4">
        <p
          className="py-24 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error}
        </p>
      </main>
    )
  }

  if (!user) return null

  const circleSize = user.circleSize ?? 0
  const followingCount = user.followingCount ?? 0

  return (
    <>
      <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
        <section className="flex items-start gap-8 pt-8 pb-12 border-b border-outline-variant/15">
          <Avatar user={user} />
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h1 className="font-headline text-4xl text-on-surface leading-tight">
                  {user.displayName}
                </h1>
                <p className="font-body text-sm text-on-surface-variant mt-1">@{user.username}</p>
              </div>
              <button
                type="button"
                aria-label="Edit profile"
                onClick={() => setEditOpen(true)}
                className="material-symbols-outlined text-on-surface-variant hover:text-on-surface p-2"
              >
                edit
              </button>
            </div>
            {user.bio ? (
              <p className="font-body text-base text-on-surface mt-4 max-w-2xl whitespace-pre-line">
                {user.bio}
              </p>
            ) : (
              <p className="font-body text-sm text-on-surface-variant italic mt-4">
                Add a bio to introduce your practice.
              </p>
            )}
            <dl className="flex gap-8 mt-6 font-label text-[11px] uppercase tracking-[0.15em] text-on-surface-variant">
              <div>
                <dt className="sr-only">In your Circle</dt>
                <dd>
                  <span className="font-headline text-lg text-on-surface not-italic normal-case tracking-normal mr-1.5">
                    {circleSize}
                  </span>
                  in your Circle
                </dd>
              </div>
              <div>
                <dt className="sr-only">Circles you're in</dt>
                <dd>
                  <span className="font-headline text-lg text-on-surface not-italic normal-case tracking-normal mr-1.5">
                    {followingCount}
                  </span>
                  Circles you're in
                </dd>
              </div>
              <div>
                <dt className="sr-only">Joined</dt>
                <dd>Joined {formatJoinDate(user.createdAt)}</dd>
              </div>
            </dl>
          </div>
        </section>

        <section className="pt-12">
          <h2 className="font-headline text-2xl text-on-surface mb-8">Your drops</h2>
          {artworks.loading ? (
            <p className="py-12 text-center text-on-surface-variant italic" role="status">
              Loading…
            </p>
          ) : artworks.error ? (
            <p
              className="py-12 text-center text-error border border-error-container/40 bg-error-container/10"
              role="alert"
            >
              {artworks.error}
            </p>
          ) : artworks.data && artworks.data.length > 0 ? (
            <MasonryFeed artworks={artworks.data} />
          ) : (
            <p className="py-12 text-center text-on-surface-variant italic">
              You haven't dropped anything yet.
            </p>
          )}
        </section>
      </main>

      <ProfileEditSidebar
        open={editOpen}
        user={user}
        onClose={() => setEditOpen(false)}
        onSaved={(updated) => setUser(updated)}
      />
    </>
  )
}
