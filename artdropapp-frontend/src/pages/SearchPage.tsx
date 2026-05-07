import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { SearchArtistsTab } from '../components/search/SearchArtistsTab'
import { SearchArtworksTab } from '../components/search/SearchArtworksTab'
import { SearchChallengesTab } from '../components/search/SearchChallengesTab'

const TABS = ['artworks', 'artists', 'challenges'] as const
type Tab = (typeof TABS)[number]

function parseTab(value: string | null): Tab {
  return TABS.includes(value as Tab) ? (value as Tab) : 'artworks'
}

export function SearchPage() {
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const query = (searchParams.get('q') ?? '').trim()
  const tab = parseTab(searchParams.get('tab'))

  const setTab = (next: Tab) => {
    const params = new URLSearchParams(searchParams)
    params.set('tab', next)
    setSearchParams(params, { replace: true })
  }

  return (
    <main className="mx-auto max-w-[1440px] px-8 pt-4 pb-24">
      <header className="mb-6">
        <p className="font-body text-xs font-normal uppercase tracking-widest text-on-surface-variant">
          {t('search.page.title')}
        </p>
        <h1 className="mt-1 font-headline text-3xl font-medium text-on-surface line-clamp-2">
          {query ? `'${query}'` : t('search.page.titleEmpty')}
        </h1>
      </header>

      <div
        role="tablist"
        aria-label={t('search.page.title')}
        className="-mx-8 mb-8 flex gap-1 overflow-x-auto overflow-y-hidden border-b border-outline-variant/20 px-8 md:mx-0 md:px-0"
      >
        {TABS.map((id) => {
          const active = id === tab
          return (
            <button
              key={id}
              type="button"
              role="tab"
              aria-selected={active}
              onClick={() => setTab(id)}
              className={`relative shrink-0 px-4 py-3 font-body text-sm font-semibold uppercase tracking-widest outline-none transition-colors focus-visible:text-primary ${
                active
                  ? 'text-on-surface after:absolute after:inset-x-0 after:bottom-[-1px] after:h-[2px] after:bg-primary'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              {t(`search.page.tab.${id}`)}
            </button>
          )
        })}
      </div>

      {!query ? (
        <p className="py-24 text-center text-on-surface-variant italic">
          {t('search.page.noQuery')}
        </p>
      ) : tab === 'artworks' ? (
        <SearchArtworksTab query={query} />
      ) : tab === 'artists' ? (
        <SearchArtistsTab query={query} />
      ) : (
        <SearchChallengesTab query={query} />
      )}
    </main>
  )
}
