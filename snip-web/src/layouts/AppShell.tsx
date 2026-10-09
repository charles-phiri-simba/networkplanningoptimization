import { NavLink, Outlet } from 'react-router-dom'
import { BrandLockup } from '../components/BrandLockup'
import { useAuth } from '../features/auth/AuthContext'

const PRIMARY = [
  { to: '/network', label: 'Network' },
  { to: '/planning', label: 'Planning' },
  { to: '/assurance', label: 'Assurance' },
  { to: '/optimization', label: 'Optimization' },
  { to: '/ai', label: 'Agents' },
]

export function AppShell() {
  const { identity, signOut } = useAuth()

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to main content
      </a>
      <header className="app-header">
        <BrandLockup />
        <nav className="app-nav" aria-label="Primary">
          {PRIMARY.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="header-meta">
          <span className="env-badge" aria-label="Demo environment">
            DEMO
          </span>
          <div className="actor">
            <p>
              <span className="eyebrow">Demo actor</span>
              <strong>{identity?.displayName}</strong>
            </p>
            <p className="muted">{identity?.role}</p>
            <button type="button" className="btn btn-quiet" onClick={signOut}>
              Switch persona
            </button>
          </div>
        </div>
      </header>
      <main id="main-content" className="app-main" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}
