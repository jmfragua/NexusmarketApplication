import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fakeBackend, renderApp } from '@/test/render'
import { AboutPage } from './AboutPage'

describe('about page', () => {
  beforeEach(() => {
    localStorage.clear()
    fakeBackend({})
  })

  it('states, in plain words, what the project is and how the payment works', () => {
    renderApp(<AboutPage />, { path: '/acerca', route: '/acerca' })
    expect(screen.getByRole('heading', { level: 1, name: 'acerca del proyecto' })).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'pagos de prueba' })).toHaveTextContent('no se cobra dinero real')
    expect(screen.getByRole('region', { name: 'datos que guardamos' })).toHaveTextContent('no instala cookies')
    expect(screen.getByRole('region', { name: 'contacto' })).toBeInTheDocument()
    expect(screen.getByText(/no es un documento legal/)).toBeInTheDocument()
  })

  it('scrolls to the section named by the address', () => {
    const scrollIntoView = vi.fn<() => void>()
    Element.prototype.scrollIntoView = scrollIntoView
    renderApp(<AboutPage />, { path: '/acerca', route: '/acerca#datos' })
    expect(scrollIntoView).toHaveBeenCalledTimes(1)
    expect(scrollIntoView.mock.contexts[0]).toBe(document.getElementById('datos'))
  })

  it('does not scroll when there is no anchor', () => {
    const scrollIntoView = vi.fn<() => void>()
    Element.prototype.scrollIntoView = scrollIntoView
    renderApp(<AboutPage />, { path: '/acerca', route: '/acerca' })
    expect(scrollIntoView).not.toHaveBeenCalled()
  })
})
