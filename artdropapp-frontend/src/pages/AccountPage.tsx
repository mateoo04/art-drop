import { Pencil, Settings } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { SellerSection } from '../components/account/SellerSection'
import { SettingsSidebar } from '../components/account/SettingsSidebar'
import { MasonryFeed } from '../components/home/MasonryFeed'
import { ProfileEditSidebar } from '../components/profile/ProfileEditSidebar'
import { ProfileHeader } from '../components/profile/ProfileHeader'
import { Spinner } from '../components/ui/Spinner'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useMyArtworks } from '../hooks/useMyArtworks'

export function AccountPage() {
  const { user, loading, error, setUser } = useCurrentUser()
  const artworks = useMyArtworks(user != null)
  const [editOpen, setEditOpen] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const { t } = useTranslation()

  if (loading && !user) {
    return (
      <main className="max-w-[1440px] mx-auto px-8 pt-4">
        <div className="py-24 flex justify-center">
          <Spinner label={t('account.loadingProfile')} />
        </div>
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

  return (
    <>
      <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
        <ProfileHeader
          user={user}
          action={
            <div className="flex items-center gap-1">
              <button
                type="button"
                aria-label={t('settings.open')}
                onClick={() => setSettingsOpen(true)}
                className="text-on-surface-variant hover:text-on-surface p-2"
              >
                <Settings size={20} />
              </button>
              <button
                type="button"
                aria-label={t('account.editProfile')}
                onClick={() => setEditOpen(true)}
                className="text-on-surface-variant hover:text-on-surface p-2"
              >
                <Pencil size={20} />
              </button>
            </div>
          }
        />

        <SellerSection />

        <section className="pt-12">
          <h2 className="font-headline text-2xl text-on-surface mb-8">{t('account.yourDrops')}</h2>
          {artworks.loading ? (
            <div className="py-12 flex justify-center">
              <Spinner label={t('account.loadingDrops')} />
            </div>
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
              {t('account.noDrops')}
            </p>
          )}
        </section>
      </main>

      <SettingsSidebar open={settingsOpen} onClose={() => setSettingsOpen(false)} />
      <ProfileEditSidebar
        open={editOpen}
        user={user}
        onClose={() => setEditOpen(false)}
        onSaved={(updated) => setUser(updated)}
      />
    </>
  )
}
