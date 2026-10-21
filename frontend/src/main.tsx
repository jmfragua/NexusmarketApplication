import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'sonner'
import { App } from '@/app/App'
import { createQueryClient } from '@/app/queryClient'
import { ScrollToTop } from '@/components/ScrollToTop'
import { SessionProvider } from '@/features/auth/SessionProvider'
import '@/styles/index.css'

const root = document.getElementById('root')
if (!root) throw new Error('missing #root element')

createRoot(root).render(
  <StrictMode>
    <QueryClientProvider client={createQueryClient()}>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <SessionProvider>
          <ScrollToTop />
          <App />
          <Toaster
            position="bottom-center"
            closeButton
            toastOptions={{
              classNames: {
                toast: '!rounded-2xl !font-sans !text-[0.95rem] !shadow-[4px_5px_0_0_var(--color-ink)] !ring-1 !ring-ink',
              },
            }}
          />
        </SessionProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
