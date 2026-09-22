import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from './AuthContext'
import { AppRoutes } from '../../routes/AppRoutes'
import { DEMO_PERSONAS } from './demoIdentity'

beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    ),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
  sessionStorage.clear()
})

describe('demo identity', () => {
  it('selects a persona and shows it in the shell', async () => {
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/login']}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>,
    )
    expect(screen.getByRole('heading', { name: /select a demo persona/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: new RegExp(DEMO_PERSONAS[0].role, 'i') }))
    expect(screen.getByText(DEMO_PERSONAS[0].displayName)).toBeInTheDocument()
    expect(screen.getByLabelText('Primary')).toBeInTheDocument()
  })
})
