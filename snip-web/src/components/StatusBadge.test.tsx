import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge } from './StatusBadge'

describe('StatusBadge', () => {
  it('keeps governance and confidence statuses visually distinct', () => {
    const statuses = [
      ['HIGH', 'high'],
      ['LOW', 'low'],
      ['CRITICAL', 'critical'],
      ['OPEN', 'open'],
      ['APPROVED', 'approved'],
      ['AUTHORIZED', 'authorized'],
      ['READY_FOR_EXECUTION', 'ready'],
      ['APPLIED', 'applied'],
      ['VERIFIED', 'verified'],
      ['NOT_REQUIRED', 'not-required'],
      ['FAILED', 'failed'],
      ['BLOCKED', 'blocked'],
    ] as const

    const { container } = render(
      <>
        {statuses.map(([status]) => (
          <StatusBadge key={status} status={status} />
        ))}
      </>,
    )

    const tones = statuses.map(([, tone]) => tone)
    expect(new Set(tones).size).toBe(tones.length)
    for (const [, tone] of statuses) {
      expect(container.querySelector(`.badge-${tone}`)).toBeInTheDocument()
    }
  })
})
