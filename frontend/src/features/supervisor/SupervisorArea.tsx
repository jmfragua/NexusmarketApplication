import { Route, Routes } from 'react-router-dom'
import { NotFoundPage } from '@/app/NotFoundPage'
import { WorkspaceLayout } from '@/layouts/WorkspaceLayout'
import { SupervisorBillingPage } from './SupervisorBillingPage'
import { SupervisorInventoryPage } from './SupervisorInventoryPage'
import { SupervisorOverviewPage } from './SupervisorOverviewPage'

const links = [
  { to: '/supervisor', label: 'Resumen', end: true },
  { to: '/supervisor/facturacion', label: 'Facturación' },
  { to: '/supervisor/inventario', label: 'Inventario' },
]

export default function SupervisorArea() {
  return (
    <Routes>
      <Route element={<WorkspaceLayout home="/supervisor" links={links} />}>
        <Route index element={<SupervisorOverviewPage />} />
        <Route path="facturacion" element={<SupervisorBillingPage />} />
        <Route path="inventario" element={<SupervisorInventoryPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
