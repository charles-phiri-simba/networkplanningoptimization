import { useNavigate } from 'react-router-dom'
import { BrandLockup } from '../components/BrandLockup'
import { useAuth } from '../features/auth/AuthContext'
import { DEMO_PERSONAS } from '../features/auth/demoIdentity'

export function LoginPage() {
  const { selectPersona } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="login-page">
      <section className="login-card">
        <div className="login-brand">
          <BrandLockup />
          <span className="env-badge" aria-label="Demo environment">
            DEMO
          </span>
        </div>
        <p className="eyebrow">SNIP demo login</p>
        <h1>Select a demo persona</h1>
        <p className="muted">
          This is not production authentication. No passwords or secrets are used. The selected
          persona is frontend-only demo identity. This is not production authentication.
        </p>
        <ul className="persona-list">
          {DEMO_PERSONAS.map((persona) => (
            <li key={persona.actorId}>
              <button
                type="button"
                className="persona"
                onClick={() => {
                  selectPersona(persona)
                  navigate('/network')
                }}
              >
                <strong>{persona.role}</strong>
                <span>{persona.displayName}</span>
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
