'use client'

import { useState, useRef } from 'react'
import { Upload, X, FileText } from 'lucide-react'

interface Props {
  onFileSelected: (file: File) => void
  disabled?: boolean
}

export function FileDropzone({ onFileSelected, disabled }: Props) {
  const [dragging, setDragging] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  function handleFile(file: File) {
    const lowerName = file.name.toLowerCase()
    if (!lowerName.endsWith('.csv') && !lowerName.endsWith('.xls') && !lowerName.endsWith('.xlsx')) {
      alert('Only CSV and Excel files are accepted.')
      return
    }
    setSelectedFile(file)
    onFileSelected(file)
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) handleFile(file)
  }

  function clear() {
    setSelectedFile(null)
    if (inputRef.current) inputRef.current.value = ''
  }

  if (selectedFile) {
    return (
      <div className="flex items-center justify-between rounded-lg border border-border bg-muted/30 px-4 py-3">
        <div className="flex items-center gap-3">
          <FileText className="h-5 w-5 text-primary" />
          <div>
            <p className="text-sm font-medium">{selectedFile.name}</p>
            <p className="text-xs text-muted-foreground">
              {(selectedFile.size / 1024).toFixed(1)} KB
            </p>
          </div>
        </div>
        {!disabled && (
          <button onClick={clear} className="text-muted-foreground hover:text-foreground">
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
    )
  }

  return (
    <div
      onDragOver={e => { e.preventDefault(); setDragging(true) }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
      onClick={() => inputRef.current?.click()}
      className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-12 text-center transition-colors
        ${dragging ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/50 hover:bg-muted/30'}
        ${disabled ? 'pointer-events-none opacity-50' : ''}`}
    >
      <Upload className="h-8 w-8 text-muted-foreground mb-3" />
      <p className="text-sm font-medium">Drag & drop your statement</p>
      <p className="text-xs text-muted-foreground mt-1">or click to browse</p>
      <p className="mt-3 rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
        CSV and Excel files
      </p>
      <input
        ref={inputRef}
        type="file"
        accept=".csv, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/vnd.ms-excel"
        className="hidden"
        onChange={handleChange}
        disabled={disabled}
      />
    </div>
  )
}
