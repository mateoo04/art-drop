export function AppFooter() {
  const year = new Date().getFullYear()
  return (
    <footer
      id="app-footer"
      className="bg-surface-container-high w-full py-12 px-8 flex flex-col md:flex-row justify-between items-center mt-24"
    >
      <div className="font-headline text-sm text-on-surface mb-6 md:mb-0">ArtDrop</div>
      <p className="font-body text-xs tracking-widest uppercase text-primary">
        © {year} ArtDrop Digital Gallery. All rights reserved.
      </p>
      <div className="flex gap-8 mt-6 md:mt-0">
        <a
          className="font-body text-xs tracking-widest uppercase text-primary hover:underline underline-offset-4 transition-all"
          href="#"
        >
          Privacy Policy
        </a>
        <a
          className="font-body text-xs tracking-widest uppercase text-primary hover:underline underline-offset-4 transition-all"
          href="#"
        >
          Terms of Service
        </a>
        <a
          className="font-body text-xs tracking-widest uppercase text-primary hover:underline underline-offset-4 transition-all"
          href="#"
        >
          Contact
        </a>
      </div>
    </footer>
  )
}
