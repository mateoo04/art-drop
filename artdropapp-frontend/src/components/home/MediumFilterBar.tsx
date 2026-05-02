import { Swiper, SwiperSlide } from 'swiper/react'
import { FreeMode, Mousewheel, Keyboard, A11y } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/free-mode'

type MediumFilterBarProps = {
  mediums: string[]
  active: string
  onChange: (medium: string) => void
}

export function MediumFilterBar({ mediums, active, onChange }: MediumFilterBarProps) {
  const options = ['All', ...mediums]
  return (
    <div className="py-8 border-b border-outline-variant/10 mb-12 -mx-8">
      <Swiper
        modules={[FreeMode, Mousewheel, Keyboard, A11y]}
        freeMode={{ enabled: true, momentum: true }}
        mousewheel={{ forceToAxis: true }}
        keyboard={{ enabled: true }}
        slidesPerView="auto"
        spaceBetween={12}
        slidesOffsetBefore={32}
        slidesOffsetAfter={32}
        className="w-full"
      >
        {options.map((medium) => {
          const isActive = medium === active
          return (
            <SwiperSlide key={medium} className="!w-auto">
              <div
                role="button"
                tabIndex={0}
                aria-pressed={isActive}
                onClick={() => onChange(medium)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    onChange(medium)
                  }
                }}
                className={
                  isActive
                    ? 'bg-primary text-on-primary px-6 py-2 text-xs font-semibold tracking-widest uppercase transition-all whitespace-nowrap cursor-pointer select-none focus:outline-none focus-visible:ring-2 focus-visible:ring-on-surface'
                    : 'bg-secondary-container text-on-secondary-container px-6 py-2 text-xs font-semibold tracking-widest uppercase hover:bg-outline-variant/20 transition-all whitespace-nowrap cursor-pointer select-none focus:outline-none focus-visible:ring-2 focus-visible:ring-on-surface'
                }
              >
                {medium}
              </div>
            </SwiperSlide>
          )
        })}
      </Swiper>
    </div>
  )
}
