import { Link } from 'react-router-dom'

export function AuthHeader() {
  return (
    <header className="w-full h-24 flex items-center justify-center bg-surface px-6">
      <Link
        to="/"
        className="font-headline text-3xl font-bold tracking-tight text-on-surface transition-colors hover:text-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-4 focus-visible:ring-offset-surface"
        aria-label="Go to home page"
      >
        ArtDrop
      </Link>
    </header>
  )
}
