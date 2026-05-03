import { Swiper, SwiperSlide } from 'swiper/react'
import { FreeMode, Mousewheel, Keyboard, A11y } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/free-mode'
import { useTranslation } from 'react-i18next'

type MediumFilterBarProps = {
  mediums: string[]
  active: string
  onChange: (medium: string) => void
}

export function MediumFilterBar({ mediums, active, onChange }: MediumFilterBarProps) {
  const { t } = useTranslation()
  const options = [{ value: 'All', label: t('home.filters.all') }, ...mediums.map((m) => ({ value: m, label: m }))]
  return (
    <div className="-mx-8 pt-10 pb-3">
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
        {options.map(({ value, label }) => {
          const isActive = value === active
          return (
            <SwiperSlide key={value} className="!w-auto">
              <div
                role="button"
                tabIndex={0}
                aria-pressed={isActive}
                onClick={() => onChange(value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    onChange(value)
                  }
                }}
                className={
                  isActive
                    ? 'bg-primary text-on-primary px-6 py-2 text-xs font-semibold tracking-widest uppercase transition-all whitespace-nowrap cursor-pointer select-none focus:outline-none focus-visible:ring-2 focus-visible:ring-on-surface'
                    : 'bg-secondary-container text-on-secondary-container px-6 py-2 text-xs font-semibold tracking-widest uppercase hover:bg-outline-variant/20 transition-all whitespace-nowrap cursor-pointer select-none focus:outline-none focus-visible:ring-2 focus-visible:ring-on-surface'
                }
              >
                {label}
              </div>
            </SwiperSlide>
          )
        })}
      </Swiper>
    </div>
  )
}
