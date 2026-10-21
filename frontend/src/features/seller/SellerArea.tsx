import { Route, Routes } from 'react-router-dom'
import { NotFoundPage } from '@/app/NotFoundPage'
import { WorkspaceLayout } from '@/layouts/WorkspaceLayout'
import { NewProductPage } from './NewProductPage'
import { SellerProductPage } from './SellerProductPage'
import { SellerProductsPage } from './SellerProductsPage'
import { SellerWarehousesPage } from './SellerWarehousesPage'

const links = [
  { to: '/vendedor', label: 'Mis productos', end: true },
  { to: '/vendedor/productos/nuevo', label: 'Registrar producto' },
  { to: '/vendedor/bodegas', label: 'Mis bodegas' },
]

export default function SellerArea() {
  return (
    <Routes>
      <Route element={<WorkspaceLayout home="/vendedor" links={links} />}>
        <Route index element={<SellerProductsPage />} />
        <Route path="productos/nuevo" element={<NewProductPage />} />
        <Route path="productos/:productId" element={<SellerProductPage />} />
        <Route path="bodegas" element={<SellerWarehousesPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
