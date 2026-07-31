'use client'

import { useEffect, useRef } from 'react'
import { LogOut, CalendarDays, User, ShieldCheck } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '../../lib/auth'

interface ProfileMenuProps {
  open: boolean
  onClose: () => void
  onOpenReservations: () => void
  onOpenProfile: () => void
  onOpenAdmin: () => void
  toggleRef: React.RefObject<HTMLButtonElement | null>
}

export function ProfileMenu({ open, onClose, onOpenReservations, onOpenProfile, onOpenAdmin, toggleRef }: ProfileMenuProps) {
  const { user, logout } = useAuth()

  console.log('ProfileMenu render. open:', open, 'user:', user?.email);

  if (!open) return null

  function handleLogout() {
    onClose()
    logout()
  }

  function handleReservations() {
    onClose()
    onOpenReservations()
  }

  return (
    <>
      <div 
        className="fixed inset-0 z-[2999]" 
        onClick={(e) => {
          e.stopPropagation()
          onClose()
        }} 
      />
      <div
        className={cn(
          'absolute right-0 top-full mt-2 w-64 z-[3000]',
          'rounded-2xl border border-border bg-popover elevation-5',
        )}
      >
        {/* User info header */}
      <div className="flex items-center gap-3 border-b border-border px-4 py-3">
        <span className="grid size-9 shrink-0 place-items-center rounded-full bg-primary text-sm font-medium text-primary-foreground">
          {user?.fullName?.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() ?? 'U'}
        </span>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-foreground">{user?.fullName ?? '—'}</p>
          <p className="truncate text-xs text-muted-foreground">{user?.email ?? '—'}</p>
        </div>
      </div>

      {/* Menu items */}
      <div className="p-1.5">
        <MenuItem
          icon={<User size={16} />}
          label="Profile"
          sublabel="Role: Driver"
          onClick={() => {
            onClose();
            onOpenProfile();
          }}
        />
        <MenuItem
          icon={<CalendarDays size={16} />}
          label="My Reservations"
          sublabel="View & manage bookings"
          onClick={handleReservations}
        />
        {user?.role === 'ADMIN' && (
          <MenuItem
            icon={<ShieldCheck size={16} />}
            label="Admin Dashboard"
            sublabel="Manage lots & anomalies"
            onClick={() => {
              onClose();
              onOpenAdmin();
            }}
          />
        )}
      </div>

      <div className="border-t border-border p-1.5">
        <MenuItem
          icon={<LogOut size={16} />}
          label="Sign out"
          onClick={handleLogout}
          destructive
        />
      </div>
      </div>
    </>
  )
}

function MenuItem({
  icon,
  label,
  sublabel,
  onClick,
  destructive,
}: {
  icon: React.ReactNode
  label: string
  sublabel?: string
  onClick: () => void
  destructive?: boolean
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left',
        'transition-colors duration-100',
        destructive
          ? 'text-destructive hover:bg-destructive/10'
          : 'text-foreground hover:bg-accent',
      )}
    >
      <span className="shrink-0">{icon}</span>
      <div className="min-w-0">
        <p className="text-sm font-medium">{label}</p>
        {sublabel && <p className="text-xs text-muted-foreground">{sublabel}</p>}
      </div>
    </button>
  )
}
