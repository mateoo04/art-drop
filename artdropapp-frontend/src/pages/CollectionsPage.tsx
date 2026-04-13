import { useCollections } from '../hooks/useCollections'

export function CollectionsPage() {
  const { data, loading, error } = useCollections()

  return (
    <main className="app-main">
      <h1>ArtDrop - Collections</h1>
      {loading ? <p className="artwork-list--status">Loading collections...</p> : null}
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
                <span className="artwork-list__medium">Artwork ID: {collection.artworkId}</span>
                <span className="artwork-list__tags">
                  Created: {new Date(collection.createdAt).toLocaleString()}
                </span>
                <span className="artwork-list__tags">
                  Visibility: {collection.isPublic ? 'Public' : 'Private'}
                </span>
              </article>
            </li>
          ))}
        </ul>
      ) : null}
    </main>
  )
}
