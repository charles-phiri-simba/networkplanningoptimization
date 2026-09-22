import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Page not found</h1>
          <p className="muted">That route is not part of this SNIP increment.</p>
        </div>
      </header>
      <p>
        <Link to="/network">Back to Network</Link>
      </p>
    </div>
  )
}
