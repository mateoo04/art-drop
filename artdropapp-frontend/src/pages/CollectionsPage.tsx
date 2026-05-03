import { useTranslation } from 'react-i18next'
import { useCollections } from '../hooks/useCollections'

export function CollectionsPage() {
  const { t } = useTranslation()
  const { data, loading, error } = useCollections()

  return (
    <main className="app-main">
      <h1>{t('collections.title')}</h1>
      {loading ? <p className="artwork-list--status">{t('collections.loading')}</p> : null}
      {error ? (
        <p className="artwork-list--status artwork-list--error" role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error ? (
        <ul className="artwork-list">
          {data?.map((collection) => (
            <li key={collection.id} className="artwork-list__li">
              <article className="artwork-list__item">
                <strong className="artwork-list__title">{collection.name}</strong>
                <span>{collection.description}</span>
                <span className="artwork-list__medium">{t('collections.artworkId')}: {collection.artworkId}</span>
                <span className="artwork-list__tags">
                  {t('collections.created')}: {new Date(collection.createdAt).toLocaleString()}
                </span>
                <span className="artwork-list__tags">
                  {t('collections.visibility')}: {collection.isPublic ? t('collections.public') : t('collections.private')}
                </span>
              </article>
            </li>
          ))}
        </ul>
      ) : null}
    </main>
  )
}
