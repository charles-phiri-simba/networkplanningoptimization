export function SnipLogo({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M4.9 16.1C1 12.2 1 5.8 4.9 1.9"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M7.8 13.2c-2.3-2.3-2.3-6.1 0-8.5"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <circle cx="12" cy="9" r="2" stroke="currentColor" strokeWidth="2" />
      <path
        d="M16.2 4.8c2.3 2.3 2.3 6.1 0 8.4"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M19.1 1.9C23 5.8 23 12.1 19.1 16"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path d="M12 13v9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M9 18h6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path d="M10 22h4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}
