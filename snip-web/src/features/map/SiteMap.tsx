import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import { divIcon } from 'leaflet'
import { Link } from 'react-router-dom'
import type { AssuranceCaseDto } from '../../types/assurance'
import type { CellDto, SiteDto } from '../../types/network'
import { formatCoordinate, formatStatusLabel } from '../../utils/format'
import { attentionPhrase, siteAttention } from '../operations/operationsModel'
import { issueSeverityLabel } from '../operations/issueLabels'
import { locatedSites, mapCenter } from './locatedSites'

interface SiteMapProps {
  sites: SiteDto[]
  cells?: CellDto[] | null
  cases?: AssuranceCaseDto[] | null
}

export function SiteMap({ sites, cells = null, cases = null }: SiteMapProps) {
  const located = locatedSites(sites)
  const center = mapCenter(sites)
  const attentionKnown = cells !== null && cases !== null
  if (located.length === 0 || !center) {
    return (
      <div className="map-empty" role="status">
        No site coordinates are available.
      </div>
    )
  }

  return (
    <div className="site-map-wrap">
      <MapContainer
        center={center}
        zoom={13}
        className="site-map"
        scrollWheelZoom
        aria-label="Network site map"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {located.map((site) => {
          const attention = attentionKnown ? siteAttention(site.siteId, cells, cases) : null
          return (
            <Marker
              key={site.siteId}
              position={[site.latitude as number, site.longitude as number]}
              icon={attentionIcon(attention?.highestSeverity ?? null, attentionKnown)}
            >
              <Popup>
                <div className="map-popup">
                  <strong>{site.name}</strong>
                  <div>{site.siteId}</div>
                  <div>Inventory status: {formatStatusLabel(site.status)}</div>
                  <div>
                    {formatCoordinate(site.latitude)}, {formatCoordinate(site.longitude)}
                  </div>
                  <div>
                    {attentionKnown
                      ? `${attentionPhrase(attention?.highestSeverity ?? null)}${
                          attention && attention.activeCount > 0
                            ? ` · ${attention.activeCount} active · ${issueSeverityLabel(attention.highestSeverity ?? '')}`
                            : ''
                        }`
                      : 'Assurance attention unavailable'}
                  </div>
                  <Link to={`/network/sites/${encodeURIComponent(site.siteId)}`}>Open site</Link>
                </div>
              </Popup>
            </Marker>
          )
        })}
      </MapContainer>
      <p className="muted" role="note">
        Synthetic demonstration locations. Not RF coverage or planning evidence.
      </p>
      <ul className="map-legend" aria-label="Map attention legend">
        <li>
          <span className="site-marker site-marker-attention-none" aria-hidden="true" /> No active
          Assurance findings
        </li>
        <li>
          <span className="site-marker site-marker-attention-warning" aria-hidden="true" /> Needs
          attention
        </li>
        <li>
          <span className="site-marker site-marker-attention-critical" aria-hidden="true" /> Critical
          Assurance finding
        </li>
        <li>
          <span className="site-marker site-marker-attention-unknown" aria-hidden="true" /> Attention
          unavailable
        </li>
      </ul>
    </div>
  )
}

function attentionIcon(highestSeverity: string | null, known: boolean) {
  const tone = !known
    ? 'unknown'
    : highestSeverity === 'CRITICAL'
      ? 'critical'
      : highestSeverity === 'MAJOR'
        ? 'major'
        : highestSeverity === 'WARNING'
          ? 'warning'
          : highestSeverity === 'INFO'
            ? 'info'
            : 'none'
  const label = !known ? '?' : highestSeverity ? highestSeverity.slice(0, 1) : '·'
  return divIcon({
    className: `site-marker site-marker-attention-${tone}`,
    html: `<span class="site-marker-label">${escapeHtml(label)}</span>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  })
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
}
