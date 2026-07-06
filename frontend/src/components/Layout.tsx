import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './Layout.css'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/kanban', label: 'Kanban Board' },
  { to: '/discussion', label: 'Discussion' },
  { to: '/paste', label: 'Paste Input' },
]

function Layout() {
  const { user, logout } = useAuth()

  return (
    <div className="layout">
      <nav className="layout-nav">
        <span className="layout-brand">AI Workspace</span>
        <ul>
          {NAV_ITEMS.map((item) => (
            <li key={item.to}>
              <NavLink to={item.to} className={({ isActive }) => (isActive ? 'active' : '')}>
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
        <div className="layout-user">
          {user && <span>{user.name}</span>}
          <button type="button" onClick={logout}>
            Log out
          </button>
        </div>
      </nav>
      <main className="layout-content">
        <Outlet />
      </main>
    </div>
  )
}

export default Layout
