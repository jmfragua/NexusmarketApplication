import { Route, Routes } from 'react-router-dom'
import { NotFoundPage } from '@/app/NotFoundPage'
import { WorkspaceLayout } from '@/layouts/WorkspaceLayout'
import { AdminBillingPage } from './AdminBillingPage'
import { AdminReturnsPage } from './AdminReturnsPage'
import { AdminSellersPage } from './AdminSellersPage'
import { AdminUsersPage } from './AdminUsersPage'
import { AdminWarehousesPage } from './AdminWarehousesPage'

const links = [
  { to: '/admin', label: 'Vendedores', end: true },
  { to: '/admin/usuarios', label: 'Usuarios' },
  { to: '/admin/bodegas', label: 'Bodegas' },
  { to: '/admin/facturacion', label: 'Facturación' },
  { to: '/admin/devoluciones', label: 'Devoluciones y reembolsos' },
]

export default function AdminArea() {
  return (
    <Routes>
      <Route element={<WorkspaceLayout home="/admin" links={links} />}>
        <Route index element={<AdminSellersPage />} />
        <Route path="usuarios" element={<AdminUsersPage />} />
        <Route path="bodegas" element={<AdminWarehousesPage />} />
        <Route path="facturacion" element={<AdminBillingPage />} />
        <Route path="devoluciones" element={<AdminReturnsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
