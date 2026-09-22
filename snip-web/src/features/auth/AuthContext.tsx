import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import {
  clearDemoIdentity,
  loadDemoIdentity,
  storeDemoIdentity,
  type DemoIdentity,
} from './demoIdentity'

interface AuthContextValue {
  identity: DemoIdentity | null
  selectPersona: (identity: DemoIdentity) => void
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [identity, setIdentity] = useState<DemoIdentity | null>(() => loadDemoIdentity())

  const value = useMemo<AuthContextValue>(
    () => ({
      identity,
      selectPersona: (next) => {
        storeDemoIdentity(next)
        setIdentity(next)
      },
      signOut: () => {
        clearDemoIdentity()
        setIdentity(null)
      },
    }),
    [identity],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
