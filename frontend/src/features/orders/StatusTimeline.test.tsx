import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusTimeline } from './StatusTimeline'

// Each step reads "<number or ✓><label>, <state>": find it by its label.
const step = (label: string) => screen.getAllByRole('listitem').find((li) => new RegExp(`^(\\d|✓)${label},`).test(li.textContent ?? '')) as HTMLElement

describe('order timeline', () => {
  it('shows a paid order as paid, with the dispatch under way', () => {
    render(<StatusTimeline status="PAID" />)
    expect(step('Pagado')).toHaveTextContent('Pagado, completado')
    expect(within(step('Pagado')).getByText('✓')).toBeInTheDocument()
    const dispatch = step('Despachado')
    expect(dispatch).toHaveAttribute('aria-current', 'step')
    expect(dispatch).toHaveTextContent('en curso')
    expect(dispatch).toHaveTextContent('la bodega está alistando tu pedido para despacharlo')
    expect(dispatch.querySelector('.bg-lime')).not.toBeNull()
  })

  it('keeps the current step as it was for an order waiting for payment', () => {
    render(<StatusTimeline status="PENDING_PAYMENT" />)
    expect(step('Pendiente de pago')).toHaveAttribute('aria-current', 'step')
    expect(step('Pagado')).toHaveTextContent('pendiente')
  })

  it('keeps the staff view unchanged', () => {
    render(<StatusTimeline status="PAID" audience="staff" />)
    expect(step('Pagado')).toHaveAttribute('aria-current', 'step')
    expect(step('Pagado')).toHaveTextContent('listo para alistar y empacar')
  })
})
