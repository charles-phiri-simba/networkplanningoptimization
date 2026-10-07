export function DemoPermissionBanner() {
  return (
    <p className="banner-demo" role="note">
      <strong>SNIP Demo Environment.</strong> Demo permissions are sent as exact backend header
      values. This is <strong>not production authentication</strong>. Demo authorization uses
      request headers, not production IAM or segregation of duties.
    </p>
  )
}
