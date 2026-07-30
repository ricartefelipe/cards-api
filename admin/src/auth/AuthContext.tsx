import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { getToken, loginWithPassword, setToken } from '../api/client'

interface AuthContextValue {
  token: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => getToken())

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      async login(username, password) {
        const access = await loginWithPassword(username, password)
        setToken(access)
        setTokenState(access)
      },
      logout() {
        setToken(null)
        setTokenState(null)
      },
    }),
    [token],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth fora do AuthProvider')
  return ctx
}
