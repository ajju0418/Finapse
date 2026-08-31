/**
 * Client-side password rules.
 *
 * These mirror the server's constraints exactly so the user gets instant feedback,
 * but the server remains the enforcing authority — this is UX, not security.
 */

export interface PasswordRule {
  id: string
  label: string
  satisfied: boolean
}

export interface PasswordAssessment {
  rules: PasswordRule[]
  /** 0–4. Only meaningful once every required rule passes. */
  score: number
  label: string
  valid: boolean
}

export const MIN_PASSWORD_LENGTH = 10

export function assessPassword(password: string): PasswordAssessment {
  const rules: PasswordRule[] = [
    {
      id: 'length',
      label: `At least ${MIN_PASSWORD_LENGTH} characters`,
      satisfied: password.length >= MIN_PASSWORD_LENGTH,
    },
    {
      id: 'case',
      label: 'Upper and lowercase letters',
      satisfied: /[a-z]/.test(password) && /[A-Z]/.test(password),
    },
    { id: 'digit', label: 'At least one number', satisfied: /\d/.test(password) },
  ]

  const valid = rules.every((rule) => rule.satisfied)

  // Bonus signals beyond the minimum bar.
  let score = rules.filter((r) => r.satisfied).length
  if (password.length >= 16) score += 1
  if (/[^A-Za-z0-9]/.test(password)) score += 1
  score = Math.min(score, 4)

  const labels = ['Too weak', 'Weak', 'Fair', 'Strong', 'Excellent']

  return {
    rules,
    score: password.length === 0 ? 0 : score,
    label: password.length === 0 ? '' : labels[score],
    valid,
  }
}
