import React from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import './styles/base.css'

const container = document.getElementById('root')
if (!container) throw new Error('Không tìm thấy phần tử gốc để gắn giao diện')

createRoot(container).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
