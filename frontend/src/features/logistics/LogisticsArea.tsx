import { Route, Routes } from 'react-router-dom'
import { NotFoundPage } from '@/app/NotFoundPage'
import { WorkspaceLayout } from '@/layouts/WorkspaceLayout'
import { LogisticsOrdersPage } from './LogisticsOrdersPage'
import { LogisticsWarehousesPage } from './LogisticsWarehousesPage'

const links = [
  { to: '/logistica', label: 'Pedidos', end: true },
  { to: '/logistica/bodegas', label: 'Bodegas y existencias' },
]

export default function LogisticsArea() {
  return (
    <Routes>
      <Route element={<WorkspaceLayout home="/logistica" links={links} />}>
        <Route index element={<LogisticsOrdersPage />} />
        <Route path="bodegas" element={<LogisticsWarehousesPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
