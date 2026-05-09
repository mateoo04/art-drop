import type { TFunction } from 'i18next'

export function translateChallengeSubmitError(code: string, t: TFunction): string {
  switch (code) {
    case 'CHALLENGE_NOT_ACTIVE':
      return t('challenges.join.errorNotActive')
    case 'ARTWORK_TOO_OLD':
      return t('challenges.join.errorTooOld')
    case 'ALREADY_SUBMITTED':
      return t('challenges.join.errorAlreadySubmitted')
    case 'IN_OTHER_CHALLENGE':
      return t('challenges.join.errorInOther')
    case 'NOT_OWNER':
      return t('challenges.join.errorNotOwner')
    case 'UNAUTHENTICATED':
      return t('challenges.join.errorUnauthenticated')
    default:
      return t('challenges.join.errorFallback')
  }
}

export function translateChallengeWithdrawError(code: string, t: TFunction): string {
  switch (code) {
    case 'CHALLENGE_ENDED':
      return t('artwork.detail.challenge.errorEnded')
    case 'NOT_OWNER':
      return t('artwork.detail.challenge.errorNotOwner')
    default:
      return t('artwork.detail.challenge.errorFallback')
  }
}
