import { ChallengeBlock } from '../components/challenges/ChallengeBlock'
import { useChallenges } from '../hooks/useChallenges'

export function ChallengesPage() {
  const { data, loading, error } = useChallenges()

  return (
    <main className="max-w-screen-2xl mx-auto px-8 py-12 md:py-20 flex flex-col gap-32">
      <section className="max-w-4xl">
        <h1 className="text-6xl md:text-8xl font-headline tracking-tighter leading-[0.9] mb-8">
          Current
          <br />
          Exhibitions
        </h1>
        <p className="text-lg md:text-xl text-on-surface-variant max-w-2xl font-light leading-relaxed">
          Our monthly curated challenges invite creators to explore specific themes through
          the lens of modern minimalism. Submit your vision to be featured in the Digital
          Gallery.
        </p>
      </section>

      {loading ? (
        <p className="py-12 text-center text-on-surface-variant italic" role="status">
          Loading challenges…
        </p>
      ) : null}

      {error ? (
        <p
          className="py-12 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error}
        </p>
      ) : null}

      {!loading && !error && data
        ? data.map((challenge, index) => (
            <div key={challenge.id}>
              {index > 0 ? (
                <div className="h-1 bg-surface-container-high w-1/4 mx-auto mb-32" />
              ) : null}
              <ChallengeBlock
                challenge={challenge}
                layout={index % 2 === 0 ? 'bento' : 'horizontal'}
              />
            </div>
          ))
        : null}
    </main>
  )
}
