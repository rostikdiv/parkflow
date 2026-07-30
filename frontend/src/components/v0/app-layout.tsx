import { Navigation, Radio } from 'lucide-react'
import { SearchBar } from './search-bar'
import { ProfileAvatar } from './profile-avatar'

export function AppLayout({
  children,
  query,
  onQueryChange,
}: {
  children: React.ReactNode
  query: string
  onQueryChange: (value: string) => void
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
            <span className="ml-1 flex items-center gap-1 rounded-full bg-card/90 px-2 py-0.5 text-[10px] font-medium text-primary backdrop-blur-md shadow-sm">
              <Radio size={11} className="animate-live-pulse" aria-hidden="true" />
              Live
            </span>
          </div>
          <SearchBar value={query} onChange={onQueryChange} />
        </div>
        <div className="pointer-events-auto">
          <ProfileAvatar />
        </div>
      </header>
    </div>
  )
}
