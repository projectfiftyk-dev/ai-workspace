import { useState } from 'react'
import type { FormEvent } from 'react'
import { apiFetch, ApiError } from '../lib/api'

interface ActionItem {
  text: string
  assignee: string | null
  deadline: string | null
  confirmed: boolean
}

interface AiResult {
  id: string
  sourceId: string
  summary: string
  actionItems: ActionItem[]
  decisions: string[]
  deadlines: string[]
}

interface PasteResponse {
  sourceId: string
  messageId: string
  aiResult: AiResult | null
  analysisError: string | null
}

interface Task {
  id: string
}

function PasteInputPage() {
  const [text, setText] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [sourceId, setSourceId] = useState<string | null>(null)
  const [aiResult, setAiResult] = useState<AiResult | null>(null)
  const [analysisError, setAnalysisError] = useState<string | null>(null)
  const [retrying, setRetrying] = useState(false)
  const [confirmingIndex, setConfirmingIndex] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setAiResult(null)
    setAnalysisError(null)
    setSourceId(null)
    setSubmitting(true)
    try {
      const response = await apiFetch<PasteResponse>('/api/paste', {
        method: 'POST',
        body: JSON.stringify({ text }),
      })
      setSourceId(response.sourceId)
      setAiResult(response.aiResult)
      setAnalysisError(response.analysisError)
      setText('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRetryAnalysis() {
    if (!sourceId) return
    setRetrying(true)
    setAnalysisError(null)
    try {
      const result = await apiFetch<AiResult>(`/api/sources/${sourceId}/analyze`, {
        method: 'POST',
      })
      setAiResult(result)
    } catch (err) {
      setAnalysisError(err instanceof ApiError ? err.message : 'Analysis failed. Please try again.')
    } finally {
      setRetrying(false)
    }
  }

  async function handleConfirm(index: number) {
    if (!aiResult) return
    setConfirmingIndex(index)
    setError(null)
    try {
      await apiFetch<Task>(`/api/ai-results/${aiResult.id}/action-items/${index}/confirm`, {
        method: 'POST',
      })
      setAiResult({
        ...aiResult,
        actionItems: aiResult.actionItems.map((item, i) =>
          i === index ? { ...item, confirmed: true } : item
        ),
      })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not confirm this action item.')
    } finally {
      setConfirmingIndex(null)
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
            {submitting ? 'Analyzing…' : 'Submit'}
          </button>
        </div>
      </form>

      {error && <p style={{ color: '#d33' }}>{error}</p>}

      {analysisError && (
        <div style={{ color: '#d33' }}>
          <p>Analysis failed: {analysisError}</p>
          {sourceId && (
            <button type="button" onClick={handleRetryAnalysis} disabled={retrying}>
              {retrying ? 'Retrying…' : 'Retry analysis'}
            </button>
          )}
        </div>
      )}

      {aiResult && (
        <div>
          <h2>Analysis</h2>

          <h3>Summary</h3>
          <p>{aiResult.summary || '—'}</p>

          <h3>Decisions</h3>
          {aiResult.decisions.length === 0 ? (
            <p>—</p>
          ) : (
            <ul>
              {aiResult.decisions.map((decision, i) => (
                <li key={i}>{decision}</li>
              ))}
            </ul>
          )}

          <h3>Deadlines</h3>
          {aiResult.deadlines.length === 0 ? (
            <p>—</p>
          ) : (
            <ul>
              {aiResult.deadlines.map((deadline, i) => (
                <li key={i}>{deadline}</li>
              ))}
            </ul>
          )}

          <h3>Action items</h3>
          {aiResult.actionItems.length === 0 ? (
            <p>—</p>
          ) : (
            <ul>
              {aiResult.actionItems.map((item, index) => (
                <li key={index}>
                  {item.text}{' '}
                  {item.confirmed ? (
                    <span>✓ Confirmed as task</span>
                  ) : (
                    <button
                      type="button"
                      onClick={() => handleConfirm(index)}
                      disabled={confirmingIndex === index}
                    >
                      {confirmingIndex === index ? 'Confirming…' : 'Confirm as Task'}
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}

export default PasteInputPage
