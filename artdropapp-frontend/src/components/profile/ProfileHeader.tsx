import type { ReactNode } from 'react'
import type { UserProfile } from '../../types/user'

type ProfileHeaderProps = {
  user: UserProfile
  action?: ReactNode
}

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
  const initial = (user.displayName || '?').slice(0, 1).toUpperCase()
  return (
    <div className="w-28 h-28 rounded-full bg-surface-container-low flex items-center justify-center font-headline text-4xl text-on-surface">
      {initial}
    </div>
  )
}

export function ProfileHeader({ user, action }: ProfileHeaderProps) {
  const showCounts = user.circleSize != null
  return (
    <section className="flex items-start gap-8 pt-8 pb-12 border-b border-outline-variant/15">
      <Avatar user={user} />
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-4">
          <h1 className="font-headline text-4xl text-on-surface leading-tight">
            {user.displayName}
          </h1>
          {action}
        </div>
        {user.bio ? (
          <p className="font-body text-base text-on-surface mt-4 max-w-2xl whitespace-pre-line">
            {user.bio}
          </p>
        ) : user.isSelf ? (
          <p className="font-body text-sm text-on-surface-variant italic mt-4">
            Add a bio to introduce your practice.
          </p>
        ) : null}
        <dl className="flex gap-8 mt-6 font-label text-[11px] uppercase tracking-[0.15em] text-on-surface-variant items-center">
          {showCounts ? (
            <>
              <div>
                <dt className="sr-only">In your Circle</dt>
                <dd>
                  <span className="font-headline text-lg text-on-surface not-italic normal-case tracking-normal mr-1.5">
                    {user.circleSize ?? 0}
                  </span>
                  in your Circle
                </dd>
              </div>
              <div>
                <dt className="sr-only">Circles you're in</dt>
                <dd>
                  <span className="font-headline text-lg text-on-surface not-italic normal-case tracking-normal mr-1.5">
                    {user.followingCount ?? 0}
                  </span>
                  Circles you're in
                </dd>
              </div>
            </>
          ) : null}
          <div>
            <dt className="sr-only">Drops</dt>
            <dd>
              <span className="font-headline text-lg text-on-surface not-italic normal-case tracking-normal mr-1.5">
                {user.artworkCount}
              </span>
              {user.artworkCount === 1 ? 'drop' : 'drops'}
            </dd>
          </div>
          <div>
            <dt className="sr-only">Joined</dt>
            <dd>Joined {formatJoinDate(user.createdAt)}</dd>
          </div>
        </dl>
      </div>
    </section>
  )
}
