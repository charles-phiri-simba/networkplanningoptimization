import { useEffect, useState } from 'react'
import { snipApi } from '../api/snipApi'
import { AssuranceList } from '../features/assurance/AssuranceList'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import type { AssuranceCaseDto } from '../types/assurance'

export function AssuranceListPage() {
  const [cases, setCases] = useState<AssuranceCaseDto[] | null>(null)
  const [error, setError] = useState<unknown>(null)

  function load() {
    setError(null)
    snipApi.listAssuranceCases().then(setCases).catch(setError)
  }

  useEffect(() => {
    load()
  }, [])

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!cases) {
    return <LoadingState label="Loading assurance cases…" />
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Assurance</h1>
          <p className="muted">Active and historical findings returned by SNIP.</p>
        </div>
      </header>
      <section className="panel">
        <AssuranceList cases={cases} />
      </section>
    </div>
  )
}
