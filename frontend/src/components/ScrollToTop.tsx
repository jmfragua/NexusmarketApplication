import { useEffect, useRef } from 'react'
import { useLocation } from 'react-router-dom'

/** A client side route change should start at the top, as a full page load would. */
export function ScrollToTop() {
  const { pathname } = useLocation()
  const previous = useRef(pathname)
  useEffect(() => {
    if (previous.current === pathname) return
    previous.current = pathname
    window.scrollTo({ top: 0 })
  }, [pathname])
  return null
}
