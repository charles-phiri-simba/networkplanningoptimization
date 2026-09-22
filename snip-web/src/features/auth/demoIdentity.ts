export interface DemoIdentity {
  actorId: string
  displayName: string
  role: string
  profile: string
}

export const DEMO_PERSONAS: readonly DemoIdentity[] = [
  {
    actorId: 'demo-network-engineer',
    displayName: 'Alex Moyo',
    role: 'Network Engineer',
    profile: 'NETWORK_ENGINEER',
  },
  {
    actorId: 'demo-rf-optimisation',
    displayName: 'Priya Naidoo',
    role: 'RF Optimisation Engineer',
    profile: 'RF_OPTIMISATION',
  },
  {
    actorId: 'demo-assurance-engineer',
    displayName: 'Daniel Khumalo',
    role: 'Assurance Engineer',
    profile: 'ASSURANCE',
  },
]

const STORAGE_KEY = 'snip.demo.identity.v1'

export function loadDemoIdentity(): DemoIdentity | null {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw) as DemoIdentity
    if (!parsed.actorId || !parsed.displayName || !parsed.role || !parsed.profile) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

export function storeDemoIdentity(identity: DemoIdentity): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(identity))
}

export function clearDemoIdentity(): void {
  sessionStorage.removeItem(STORAGE_KEY)
}
