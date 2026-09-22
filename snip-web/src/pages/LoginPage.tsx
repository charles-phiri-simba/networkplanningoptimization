import { useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { DEMO_PERSONAS } from '../features/auth/demoIdentity'

export function LoginPage() {
  const { selectPersona } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="login-page">
      <section className="login-card">
        <p className="eyebrow">SNIP demo login</p>
        <h1>Select a demo persona</h1>
        <p className="muted">
          This is not production authentication. No passwords or secrets are used. The selected
          persona is frontend-only identity state for Increment 1A read-only screens.
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
                <span className="muted">{persona.actorId}</span>
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
