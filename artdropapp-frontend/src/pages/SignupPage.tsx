import { AuthHeader } from '../components/layout/AuthHeader'

const HERO_IMAGE_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCu_FZrOTTzX5XK4WeCT-aVlozipqUREoyXBxA5nWws63YY8rNsVbPz1NCAOtwKduW0_QEzLi8p9XX7znw-1V0XyRhwF4PpxCMY0mVyilcV-4DmBEUYQVd9c3a_IrAMxQ83RzHyA1036Y8NMzU4av-LYfBL_pi5xovfyk1x6TpPvrL0foUy1iHaaHlFU-QSAvd4v1sU6FInP0ZrSWzPhs8QDeSeanyr-Rox6N1SktzKjx5mUexaemlyoJC8OuSHOsbNs1NIRnhHPFU'

export function SignupPage() {
  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-4 md:py-12 flex flex-col flex-grow">
        <div className="mb-10 w-full overflow-hidden">
          <img
            className="w-full h-48 md:h-64 object-cover grayscale opacity-90 transition-all duration-700 hover:grayscale-0"
            src={HERO_IMAGE_URL}
            alt="Abstract minimalist painting in muted earth tones"
          />
        </div>

        <section className="mb-8">
          <h2 className="font-headline text-4xl font-light mb-2 tracking-tight">
            Begin your collection.
          </h2>
          <p className="font-body text-sm text-on-surface-variant leading-relaxed max-w-[280px]">
            Join our community of digital curators and discover exceptional artworks.
          </p>
        </section>

        {/* Form comes in Task 9 */}
      </main>
    </div>
  )
}
