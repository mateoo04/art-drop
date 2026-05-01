const CLOUD_NAME = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME ?? ''

export type CloudinaryOptions = {
  width?: number
  height?: number
  crop?: 'fill' | 'fit' | 'limit' | 'scale' | 'thumb'
  quality?: 'auto' | number
  format?: 'auto' | 'jpg' | 'png' | 'webp' | 'avif'
  gravity?: 'auto' | 'face' | 'center'
  dpr?: 'auto' | number
}

export function cloudinaryUrl(publicId: string, opts: CloudinaryOptions = {}): string {
  if (!publicId) return ''
  if (publicId.startsWith('http://') || publicId.startsWith('https://')) {
    return publicId
  }
  if (!CLOUD_NAME) {
    return publicId
  }
  const transforms: string[] = []
  transforms.push(`c_${opts.crop ?? 'limit'}`)
  if (opts.gravity) transforms.push(`g_${opts.gravity}`)
  if (opts.width) transforms.push(`w_${opts.width}`)
  if (opts.height) transforms.push(`h_${opts.height}`)
  if (opts.dpr) transforms.push(`dpr_${opts.dpr}`)
  transforms.push(`q_${opts.quality ?? 'auto'}`)
  transforms.push(`f_${opts.format ?? 'auto'}`)
  const transform = transforms.join(',')
  return `https://res.cloudinary.com/${CLOUD_NAME}/image/upload/${transform}/${publicId}`
}

export function cloudinarySrcSet(
  publicId: string,
  widths: number[],
  opts: Omit<CloudinaryOptions, 'width'> = {},
): string {
  if (!publicId) return ''
  if (publicId.startsWith('http://') || publicId.startsWith('https://')) {
    return ''
  }
  return widths
    .map((w) => `${cloudinaryUrl(publicId, { ...opts, width: w })} ${w}w`)
    .join(', ')
}
