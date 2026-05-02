import { Pencil } from 'lucide-react'
import { useState } from 'react'
import { SellerSection } from '../components/account/SellerSection'
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

  if (loading && !user) {
    return (
      <main className="max-w-[1440px] mx-auto px-8 pt-4">
        <div className="py-24 flex justify-center">
          <Spinner label="Loading your profile" />
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
            <button
              type="button"
              aria-label="Edit profile"
              onClick={() => setEditOpen(true)}
              className="text-on-surface-variant hover:text-on-surface p-2"
            >
              <Pencil size={20} />
            </button>
          }
        />

        <SellerSection />

        <section className="pt-12">
          <h2 className="font-headline text-2xl text-on-surface mb-8">Your drops</h2>
          {artworks.loading ? (
            <div className="py-12 flex justify-center">
              <Spinner label="Loading your drops" />
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
