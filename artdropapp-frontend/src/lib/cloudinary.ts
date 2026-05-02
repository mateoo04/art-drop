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

type CloudinaryWidgetResult = {
  event: string
  info?: {
    public_id?: string
    secure_url?: string
    width?: number
    height?: number
  }
}

type CloudinaryWidget = {
  open: () => void
  close: () => void
  destroy: () => void
}

declare global {
  interface Window {
    cloudinary?: {
      createUploadWidget: (
        options: Record<string, unknown>,
        callback: (error: unknown, result: CloudinaryWidgetResult) => void,
      ) => CloudinaryWidget
    }
  }
}

const WIDGET_SCRIPT_URL = 'https://upload-widget.cloudinary.com/global/all.js'
let widgetScriptPromise: Promise<void> | null = null

export function loadCloudinaryWidgetScript(): Promise<void> {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('Cloudinary widget unavailable on server'))
  }
  if (window.cloudinary?.createUploadWidget) {
    return Promise.resolve()
  }
  if (widgetScriptPromise) return widgetScriptPromise
  widgetScriptPromise = new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>(
      `script[src="${WIDGET_SCRIPT_URL}"]`,
    )
    if (existing) {
      existing.addEventListener('load', () => resolve())
      existing.addEventListener('error', () => reject(new Error('Failed to load Cloudinary widget')))
      return
    }
    const script = document.createElement('script')
    script.src = WIDGET_SCRIPT_URL
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load Cloudinary widget'))
    document.head.appendChild(script)
  })
  return widgetScriptPromise
}

export type CloudinaryUploadResult = {
  publicId: string
  url: string
  width?: number
  height?: number
}

export type OpenCloudinaryUploadOptions = {
  multiple?: boolean
  maxFiles?: number
}

export async function openCloudinaryUpload(
  options: OpenCloudinaryUploadOptions = {},
): Promise<CloudinaryUploadResult[]> {
  const cloudName = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME
  const uploadPreset = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET
  if (!cloudName || !uploadPreset) {
    throw new Error(
      'Cloudinary upload is not configured (set VITE_CLOUDINARY_CLOUD_NAME and VITE_CLOUDINARY_UPLOAD_PRESET).',
    )
  }
  await loadCloudinaryWidgetScript()
  if (!window.cloudinary?.createUploadWidget) {
    throw new Error('Cloudinary widget unavailable')
  }
  const previousBodyOverflow = document.body.style.overflow
  const previousHtmlOverflow = document.documentElement.style.overflow
  const restoreScroll = () => {
    document.body.style.overflow = previousBodyOverflow
    document.documentElement.style.overflow = previousHtmlOverflow
  }
  const multiple = options.multiple ?? false
  const maxFiles = options.maxFiles ?? (multiple ? 10 : 1)

  return new Promise<CloudinaryUploadResult[]>((resolve, reject) => {
    const collected: CloudinaryUploadResult[] = []
    let settled = false
    const finish = (action: () => void, cleanup: () => void) => {
      if (settled) return
      settled = true
      cleanup()
      restoreScroll()
      action()
    }
    const widget = window.cloudinary!.createUploadWidget(
      {
        cloudName,
        uploadPreset,
        sources: ['local', 'url', 'camera'],
        multiple,
        maxFiles,
        clientAllowedFormats: ['jpg', 'jpeg', 'png', 'webp'],
        maxFileSize: 15_000_000,
      },
      (error, result) => {
        if (error) {
          finish(
            () => reject(error instanceof Error ? error : new Error(String(error))),
            () => widget.destroy(),
          )
          return
        }
        if (result.event === 'success' && result.info?.public_id) {
          const info = result.info
          collected.push({
            publicId: info.public_id ?? '',
            url: info.secure_url ?? '',
            width: info.width,
            height: info.height,
          })
          if (!multiple) {
            finish(() => resolve(collected), () => widget.close())
          }
        } else if (result.event === 'queue-end' && multiple) {
          finish(() => resolve(collected), () => widget.close())
        } else if (result.event === 'close') {
          finish(() => resolve(collected), () => widget.destroy())
        }
      },
    )
    widget.open()
  })
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
