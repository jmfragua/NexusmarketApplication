import { lazy, Suspense, type ReactNode } from 'react'
import { Route, Routes } from 'react-router-dom'
import { LoadingRows } from '@/components/ui/States'
import { AccountPage } from '@/features/account/AccountPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { GuestOnly, RequireRole } from '@/features/auth/RequireRole'
import { CartPage } from '@/features/cart/CartPage'
import { CatalogPage } from '@/features/catalog/CatalogPage'
import { ProductPage } from '@/features/catalog/ProductPage'
import { CheckoutPage } from '@/features/checkout/CheckoutPage'
import { InvoicesPage } from '@/features/orders/InvoicesPage'
import { OrderDetailPage } from '@/features/orders/OrderDetailPage'
import { OrdersPage } from '@/features/orders/OrdersPage'
import { StoreLayout } from '@/layouts/StoreLayout'
import { NotFoundPage } from './NotFoundPage'

// The operation areas are split out: a buyer never downloads the code of a role they do not hold.
const SellerArea = lazy(() => import('@/features/seller/SellerArea'))
const LogisticsArea = lazy(() => import('@/features/logistics/LogisticsArea'))
const AdminArea = lazy(() => import('@/features/admin/AdminArea'))
const SupervisorArea = lazy(() => import('@/features/supervisor/SupervisorArea'))

function Lazy({ children }: { children: ReactNode }) {
  return (
    <Suspense
      fallback={
        <div className="mx-auto max-w-5xl p-8">
          <LoadingRows rows={3} label="cargando el panel" />
        </div>
      }
    >
      {children}
    </Suspense>
  )
}

const buyer = (page: ReactNode) => <RequireRole allow="BUYER">{page}</RequireRole>

export function App() {
  return (
    <Routes>
      <Route element={<StoreLayout />}>
        <Route index element={<CatalogPage />} />
        <Route path="productos/:productId" element={<ProductPage />} />
        <Route path="ingresar" element={<GuestOnly><LoginPage /></GuestOnly>} />
        <Route path="registro" element={<GuestOnly><RegisterPage /></GuestOnly>} />
        <Route path="carrito" element={buyer(<CartPage />)} />
        <Route path="pagar" element={buyer(<CheckoutPage />)} />
        <Route path="pedidos" element={buyer(<OrdersPage />)} />
        <Route path="pedidos/:orderId" element={buyer(<OrderDetailPage />)} />
        <Route path="facturas" element={buyer(<InvoicesPage />)} />
        <Route path="cuenta" element={buyer(<AccountPage />)} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="vendedor/*" element={<RequireRole allow="SELLER"><Lazy><SellerArea /></Lazy></RequireRole>} />
      <Route path="logistica/*" element={<RequireRole allow="LOGISTICS_OPERATOR"><Lazy><LogisticsArea /></Lazy></RequireRole>} />
      <Route path="admin/*" element={<RequireRole allow="ADMINISTRATOR"><Lazy><AdminArea /></Lazy></RequireRole>} />
      <Route path="supervisor/*" element={<RequireRole allow="SUPERVISOR"><Lazy><SupervisorArea /></Lazy></RequireRole>} />
    </Routes>
  )
}
