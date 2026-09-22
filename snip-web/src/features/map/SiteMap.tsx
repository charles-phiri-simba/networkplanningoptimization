import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import { divIcon } from 'leaflet'
import { Link } from 'react-router-dom'
import type { SiteDto } from '../../types/network'
import { formatCoordinate, formatStatusLabel } from '../../utils/format'
import { locatedSites, mapCenter } from './locatedSites'

interface SiteMapProps {
  sites: SiteDto[]
}

export function SiteMap({ sites }: SiteMapProps) {
  const located = locatedSites(sites)
  const center = mapCenter(sites)
  if (located.length === 0 || !center) {
    return (
      <div className="map-empty" role="status">
        No site coordinates are available.
      </div>
    )
  }

  return (
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
      {located.map((site) => (
        <Marker
          key={site.siteId}
          position={[site.latitude as number, site.longitude as number]}
          icon={statusIcon(site.status)}
        >
          <Popup>
            <div className="map-popup">
              <strong>{site.name}</strong>
              <div>{site.siteId}</div>
              <div>
                Status: {formatStatusLabel(site.status)} · {formatCoordinate(site.latitude)},{' '}
                {formatCoordinate(site.longitude)}
              </div>
              <Link to={`/network/sites/${encodeURIComponent(site.siteId)}`}>Open site</Link>
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  )
}

function statusIcon(status: string) {
  const tone = status.toUpperCase() === 'ACTIVE' ? 'ok' : 'neutral'
  const label = status.slice(0, 1).toUpperCase()
  return divIcon({
    className: `site-marker site-marker-${tone}`,
    html: `<span class="site-marker-label">${escapeHtml(label)}</span>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  })
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
}
