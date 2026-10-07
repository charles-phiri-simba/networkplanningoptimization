import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'

const PRIMARY = [
  { to: '/network', label: 'Network' },
  { to: '/planning', label: 'Planning' },
  { to: '/assurance', label: 'Assurance' },
  { to: '/optimization', label: 'Optimization' },
  { to: '/ai', label: 'AI' },
]

export function AppShell() {
  const { identity, signOut } = useAuth()

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to main content
      </a>
      <header className="app-header">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            S
          </span>
          <div>
            <p className="brand-name">SNIP</p>
            <p className="brand-sub">Network Planning &amp; Optimisation</p>
          </div>
        </div>
        <nav className="app-nav" aria-label="Primary">
          {PRIMARY.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              {item.label}
            </NavLink>
          ))}
        </nav>
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
      </header>
      <main id="main-content" className="app-main" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}
