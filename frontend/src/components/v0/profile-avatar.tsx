'use client'

import { useState, useRef } from 'react'
import { useAuth } from '../../lib/auth'
import { ProfileMenu } from './profile-menu'

interface ProfileAvatarProps {
  onOpenReservations: () => void
  onOpenProfile: () => void
  onOpenAdmin: () => void
}

export function ProfileAvatar({ onOpenReservations, onOpenProfile, onOpenAdmin }: ProfileAvatarProps) {
  const { user } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const toggleRef = useRef<HTMLButtonElement>(null)

  const initials = user?.fullName?.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() ?? 'U'

  return (
    // Relative wrapper so the dropdown is positioned below the avatar
    <div className="relative">
      <button
        ref={toggleRef}
        type="button"
        onClick={() => {
          console.log('Profile button clicked! menuOpen was:', menuOpen);
          setMenuOpen(v => !v);
        }}
        aria-label="Open profile menu"
        aria-haspopup="true"
        aria-expanded={menuOpen}
        className="grid size-14 shrink-0 place-items-center rounded-full border border-border bg-card/90 backdrop-blur-md elevation-2 transition-transform duration-150 hover:scale-[1.03] active:scale-95"
      >
        <span className="grid size-9 place-items-center rounded-full bg-primary text-sm font-medium text-primary-foreground">
          {initials}
        </span>
      </button>

      <ProfileMenu
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        onOpenReservations={onOpenReservations}
        onOpenProfile={onOpenProfile}
        onOpenAdmin={onOpenAdmin}
      />
    </div>
  )
}
