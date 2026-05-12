import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Uncaught render error:', error, info)
  }

  private handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="max-w-[1440px] mx-auto px-8 py-24 flex flex-col items-center text-center gap-4">
          <h1 className="text-2xl font-semibold">Something went wrong</h1>
          <p className="text-neutral-500">
            An unexpected error occurred. Try reloading the page.
          </p>
          <button
            type="button"
            onClick={this.handleReload}
            className="mt-4 inline-flex items-center px-4 py-2 rounded-full bg-neutral-900 text-white hover:bg-neutral-700 transition-colors"
          >
            Reload
          </button>
        </main>
      )
    }
    return this.props.children
  }
}
