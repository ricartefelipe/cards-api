import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import {
  AccountDetailPage,
  AccountsPage,
  CustomerDetailPage,
  CustomersPage,
  DashboardPage,
  LoginPage,
  PhysicalCardDetailPage,
  PhysicalCardsPage,
  VirtualCardDetailPage,
  VirtualCardsPage,
  WebhooksPage,
} from './pages'

function Protected({ children }: { children: React.ReactNode }) {
  const { token } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <Protected>
                <DashboardPage />
              </Protected>
            }
          />
          <Route
            path="/accounts"
            element={
              <Protected>
                <AccountsPage />
              </Protected>
            }
          />
          <Route
            path="/accounts/:id"
            element={
              <Protected>
                <AccountDetailPage />
              </Protected>
            }
          />
          <Route
            path="/customers"
            element={
              <Protected>
                <CustomersPage />
              </Protected>
            }
          />
          <Route
            path="/customers/:id"
            element={
              <Protected>
                <CustomerDetailPage />
              </Protected>
            }
          />
          <Route
            path="/physical-cards"
            element={
              <Protected>
                <PhysicalCardsPage />
              </Protected>
            }
          />
          <Route
            path="/physical-cards/:id"
            element={
              <Protected>
                <PhysicalCardDetailPage />
              </Protected>
            }
          />
          <Route
            path="/virtual-cards"
            element={
              <Protected>
                <VirtualCardsPage />
              </Protected>
            }
          />
          <Route
            path="/virtual-cards/:id"
            element={
              <Protected>
                <VirtualCardDetailPage />
              </Protected>
            }
          />
          <Route
            path="/webhooks"
            element={
              <Protected>
                <WebhooksPage />
              </Protected>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
