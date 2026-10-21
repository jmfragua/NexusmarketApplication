import { useLocation } from 'react-router-dom'

/** Renders the current path so a test can assert where a flow navigated to. */
export function LocationProbe() {
  const location = useLocation()
  return <output data-testid="location">{location.pathname}</output>
}
