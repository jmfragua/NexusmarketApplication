import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { fakeBackend, renderApp, type FakeRoutes } from '@/test/render'
import { AdminUsersPage } from './AdminUsersPage'

const created = (body: Record<string, string>) => ({
  status: 201,
  body: { id: body.userId, fullName: body.fullName, email: body.email, identityDocument: body.identityDocument, role: body.role, status: 'ACTIVE' },
})

function open(routes: FakeRoutes = {}) {
  localStorage.setItem('nexusmarket.session', JSON.stringify({ token: 't', userId: 'admin', email: 'admin@nexusmarket.local', role: 'ADMINISTRATOR', expiresAt: null }))
  const backend = fakeBackend({ 'POST /admin/staff': (init) => created(JSON.parse(String(init.body)) as Record<string, string>), ...routes })
  renderApp(<AdminUsersPage />)
  return { calls: backend.calls, form: screen.getByRole('region', { name: 'crear personal' }) }
}

async function fill(form: HTMLElement, values: { name?: string; email?: string; document?: string; password?: string } = {}) {
  const { name = 'Laura Operaria', email = 'laura@nexusmarket.local', document = '1020304050', password = 'Bodega2026' } = values
  if (name) await userEvent.type(within(form).getByLabelText('Nombre completo'), name)
  if (email) await userEvent.type(within(form).getByLabelText('Correo'), email)
  if (document) await userEvent.type(within(form).getByLabelText('Documento de identidad'), document)
  if (password) await userEvent.type(within(form).getByLabelText('Contraseña inicial', { selector: 'input' }), password)
}

describe('create staff', () => {
  beforeEach(() => localStorage.clear())

  it('offers only the two roles the server accepts', () => {
    const { form } = open()
    const options = within(within(form).getByLabelText('Rol')).getAllByRole('option')
    expect(options.map((o) => o.getAttribute('value'))).toEqual(['LOGISTICS_OPERATOR', 'SUPERVISOR'])
    expect(options.map((o) => o.textContent)).toEqual(['Operador logístico', 'Supervisor'])
  })

  it('checks the fields on the client and sends nothing while they are wrong', async () => {
    const { form, calls } = open()
    await fill(form, { name: '', email: 'laura@local', document: '', password: 'abc' })
    await userEvent.click(within(form).getByRole('button', { name: 'Crear personal' }))
    expect(within(form).getByText('escribe el nombre completo')).toBeInTheDocument()
    expect(within(form).getByText(/ese correo no parece válido/)).toBeInTheDocument()
    expect(within(form).getByText('escribe el documento de identidad')).toBeInTheDocument()
    expect(within(form).getByText(/la contraseña es muy débil/)).toBeInTheDocument()
    expect(calls.some((c) => c.key === 'POST /admin/staff')).toBe(false)
  })

  it('creates the account with an identifier generated here and the chosen role', async () => {
    const { form, calls } = open()
    await fill(form)
    await userEvent.selectOptions(within(form).getByLabelText('Rol'), 'SUPERVISOR')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear personal' }))

    const card = await screen.findByTestId('staff-created')
    const call = calls.find((c) => c.key === 'POST /admin/staff')
    const body = call?.body as Record<string, string>
    expect(body).toMatchObject({ fullName: 'Laura Operaria', email: 'laura@nexusmarket.local', identityDocument: '1020304050', password: 'Bodega2026', role: 'SUPERVISOR' })
    expect(body.userId).toMatch(/^[0-9a-f-]{36}$/)
    expect(card).toHaveTextContent('Laura Operaria')
    expect(card).toHaveTextContent('Supervisor')
    expect(card).toHaveTextContent(body.userId?.slice(0, 8).toUpperCase() ?? '')
    // the form is empty again, ready for the next one
    expect(within(form).getByLabelText('Nombre completo')).toHaveValue('')
  })

  it('hands the new identifier to the block and role controls', async () => {
    const { form, calls } = open()
    await fill(form)
    await userEvent.click(within(form).getByRole('button', { name: 'Crear personal' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Usar este identificador para bloquear o cambiar el rol' }))
    const idField = screen.getByLabelText('Identificador del usuario')
    const body = calls.find((c) => c.key === 'POST /admin/staff')?.body as Record<string, string> | undefined
    const newId = body?.userId
    expect(idField).toHaveValue(newId)
    expect(idField).toHaveFocus()
  })

  it.each([
    ['DuplicateEmailException', 'ya existe una cuenta con ese correo'],
    ['DuplicateIdentityDocumentException', 'ya existe una cuenta con ese documento de identidad'],
  ])('explains a %s from the server in Spanish', async (rule, message) => {
    const { form } = open({ 'POST /admin/staff': () => ({ status: 409, body: { status: 409, rule, message: 'conflict' } }) })
    await fill(form)
    await userEvent.click(within(form).getByRole('button', { name: 'Crear personal' }))
    expect(await within(form).findByText(message)).toBeInTheDocument()
    expect(screen.queryByTestId('staff-created')).toBeNull()
  })

  it('is usable with the keyboard alone', async () => {
    const { form } = open()
    within(form).getByLabelText('Nombre completo').focus()
    await userEvent.keyboard('Laura Operaria')
    await userEvent.tab()
    await userEvent.keyboard('laura@nexusmarket.local')
    await userEvent.tab()
    await userEvent.keyboard('1020304050')
    await userEvent.tab()
    await userEvent.keyboard('Bodega2026')
    await userEvent.tab() // show password button
    await userEvent.tab() // role
    expect(within(form).getByLabelText('Rol')).toHaveFocus()
    await userEvent.tab()
    expect(within(form).getByRole('button', { name: 'Crear personal' })).toHaveFocus()
    await userEvent.keyboard('{Enter}')
    expect(await screen.findByTestId('staff-created')).toBeInTheDocument()
  })
})
