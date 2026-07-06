import { useState } from 'react'
import type { FormEvent } from 'react'
import { apiFetch, ApiError } from '../lib/api'

interface PasteResponse {
  sourceId: string
  messageId: string
}

function PasteInputPage() {
  const [text, setText] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState<PasteResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setResult(null)
    setSubmitting(true)
    try {
      const response = await apiFetch<PasteResponse>('/api/paste', {
        method: 'POST',
        body: JSON.stringify({ text }),
      })
      setResult(response)
      setText('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <h1>Paste Input</h1>
      <p>Paste a conversation here to submit it for AI extraction.</p>

      <form onSubmit={handleSubmit}>
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={12}
          cols={80}
          required
          placeholder="Paste raw conversation text here..."
        />
        <div>
          <button type="submit" disabled={submitting || !text.trim()}>
            {submitting ? 'Saving…' : 'Submit'}
          </button>
        </div>
      </form>

      {error && <p style={{ color: '#d33' }}>{error}</p>}

      {result && (
        <p>
          Saved. sourceId: <code>{result.sourceId}</code>, messageId: <code>{result.messageId}</code>
        </p>
      )}
    </div>
  )
}

export default PasteInputPage
