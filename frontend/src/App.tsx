import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import KanbanBoardPage from './pages/KanbanBoardPage'
import DiscussionViewPage from './pages/DiscussionViewPage'
import PasteInputPage from './pages/PasteInputPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/kanban" element={<KanbanBoardPage />} />
          <Route path="/discussion" element={<DiscussionViewPage />} />
          <Route path="/paste" element={<PasteInputPage />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default App
