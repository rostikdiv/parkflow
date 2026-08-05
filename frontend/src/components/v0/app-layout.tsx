import { Navigation } from 'lucide-react'
import { SearchBar } from './search-bar'
import { ProfileAvatar } from './profile-avatar'

export function AppLayout({
  children,
  query,
  onQueryChange,
  onOpenReservations,
  onOpenProfile,
  onOpenAdmin,
}: {
  children: React.ReactNode
  query: string
  onQueryChange: (value: string) => void
  onOpenReservations: () => void
  onOpenProfile: () => void
  onOpenAdmin: () => void
}) {
  return (
    <div className="absolute inset-0 overflow-hidden bg-background">
      {/* Map surface */}
      <div className="absolute inset-0 z-0">
        {children}
      </div>

      {/* Top bar: search + profile */}
      <header className="absolute inset-x-0 top-0 z-[1000] flex items-start gap-3 p-4 pt-[max(1rem,env(safe-area-inset-top))] pointer-events-none">
        <div className="flex min-w-0 flex-1 flex-col gap-2 pointer-events-auto">
          <div className="flex items-center gap-2 px-1">
            <span className="grid size-6 place-items-center rounded-md bg-primary text-primary-foreground">
              <Navigation size={14} aria-hidden="true" />
            </span>
            <span className="text-sm font-semibold tracking-tight text-white drop-shadow-md">ParkFlow</span>
          </div>
          <SearchBar value={query} onChange={onQueryChange} />
        </div>
        <div className="pointer-events-auto">
          <ProfileAvatar onOpenReservations={onOpenReservations} onOpenProfile={onOpenProfile} onOpenAdmin={onOpenAdmin} />
        </div>
      </header>
    </div>
  )
}
