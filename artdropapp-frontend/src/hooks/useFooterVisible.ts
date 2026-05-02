import { useEffect, useState } from 'react'

export function useFooterVisible(): boolean {
  const [visible, setVisible] = useState(false)
  useEffect(() => {
    const footer = document.getElementById('app-footer')
    if (!footer) return
    const observer = new IntersectionObserver(
      ([entry]) => setVisible(entry.isIntersecting),
      { threshold: 0 },
    )
    observer.observe(footer)
    return () => observer.disconnect()
  }, [])
  return visible
}
