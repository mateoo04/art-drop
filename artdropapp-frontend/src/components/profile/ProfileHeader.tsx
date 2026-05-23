import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { UserProfile } from '../../types/user'
import { formatEuDate } from '../../lib/dateFormat'

type ProfileHeaderProps = {
  user: UserProfile
  action?: ReactNode
}

function Avatar({ user }: { user: UserProfile }) {
  if (user.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt={user.displayName}
        className="w-20 h-20 rounded-full object-cover bg-surface-container-low sm:w-28 sm:h-28"
      />
    )
  }
  const initial = (user.displayName || '?').slice(0, 1).toUpperCase()
  return (
    <div className="w-20 h-20 rounded-full bg-surface-container-low flex items-center justify-center font-headline text-3xl text-on-surface sm:w-28 sm:h-28 sm:text-4xl">
      {initial}
    </div>
  )
}

export function ProfileHeader({ user, action }: ProfileHeaderProps) {
  const { t } = useTranslation()
  const showCounts = user.circleSize != null
  return (
    <section className="flex items-start gap-4 pt-6 pb-10 border-b border-outline-variant/15 sm:gap-8 sm:pt-8 sm:pb-12">
      <Avatar user={user} />
      <div className="flex-1 min-w-0">
        <div className="flex min-w-0 items-start justify-between gap-3 sm:gap-4">
          <h1 className="min-w-0 max-w-full break-words font-headline text-3xl text-on-surface leading-tight sm:text-4xl">
            {user.displayName}
          </h1>
          {action ? <div className="max-w-full shrink-0">{action}</div> : null}
        </div>
        {user.bio ? (
          <p className="font-body text-base text-on-surface mt-4 max-w-2xl whitespace-pre-line break-words">
            {user.bio}
          </p>
        ) : user.isSelf ? (
          <p className="font-body text-sm text-on-surface-variant italic mt-4">
            {t('profile.addBio')}
          </p>
        ) : null}
        <dl className="flex flex-wrap gap-x-6 gap-y-3 mt-6 font-label text-[11px] uppercase tracking-[0.15em] text-on-surface-variant items-center sm:gap-x-8">
          {showCounts ? (
            <>
              <div>
                <dt className="sr-only">{t('profile.circleSizeLabel')}</dt>
                <dd>
                  <span className="font-headline text-lg text-on-surface not-italic normal-case tracking-normal mr-1.5">
                    {user.circleSize ?? 0}
                  </span>
                  {t('profile.circleSize')}
                </dd>
              </div>
              <div>
                <dt className="sr-only">{t('profile.followingCountLabel')}</dt>
                <dd>
                  <span className="font-headline text-lg text-on-surface not-italic normal-case tracking-normal mr-1.5">
                    {user.followingCount ?? 0}
                  </span>
                  {t('profile.followingCount')}
                </dd>
              </div>
            </>
          ) : null}
          <div>
            <dt className="sr-only">{t('profile.drops')}</dt>
            <dd>
              <span className="font-headline text-lg text-on-surface not-italic normal-case tracking-normal mr-1.5">
                {user.artworkCount}
              </span>
              {user.artworkCount === 1 ? t('profile.dropSingular') : t('profile.dropPlural')}
            </dd>
          </div>
          <div>
            <dt className="sr-only">{t('profile.joinedPrefix')}</dt>
            <dd>{t('profile.joinedPrefix')} {formatEuDate(user.createdAt)}</dd>
          </div>
        </dl>
      </div>
    </section>
  )
}
