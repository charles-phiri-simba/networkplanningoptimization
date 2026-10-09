import { SnipLogo } from './SnipLogo'

export function BrandLockup() {
  return (
    <div className="brand">
      <span className="brand-mark" aria-hidden="true">
        <SnipLogo className="brand-icon" />
      </span>
      <div className="brand-text">
        <p className="brand-name">SNIP</p>
        <p className="brand-sub">Network Intelligence Platform</p>
      </div>
    </div>
  )
}
