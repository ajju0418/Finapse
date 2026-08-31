import { cn } from '@/lib/utils'

interface FinapseLogoProps {
  size?: number
  showText?: boolean
  className?: string
}

export function FinapseLogo({ size = 28, showText = true, className }: FinapseLogoProps) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 32 32"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="shrink-0"
      >
        <rect width="32" height="32" rx="8" className="fill-primary" />
        <path
          d="M9.5 7h13v3.5H13v4h7v3.5h-7v7.5H9.5V7z"
          className="fill-primary-foreground"
        />
        <rect x="19" y="22" width="2" height="3.5" rx="0.75" className="fill-primary-foreground" opacity="0.3" />
        <rect x="22" y="20" width="2" height="5.5" rx="0.75" className="fill-primary-foreground" opacity="0.45" />
        <rect x="25" y="17.5" width="2" height="8" rx="0.75" className="fill-primary-foreground" opacity="0.6" />
      </svg>
      {showText && (
        <span className="font-heading text-xl font-bold tracking-tight">
          Finapse
        </span>
      )}
    </span>
  )
}
