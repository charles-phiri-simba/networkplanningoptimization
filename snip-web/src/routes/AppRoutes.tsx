import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { AppShell } from '../layouts/AppShell'
import { AiPage } from '../pages/AiPage'
import { AssuranceCasePage } from '../pages/AssuranceCasePage'
import { AssuranceListPage } from '../pages/AssuranceListPage'
import { CellPage } from '../pages/CellPage'
import { ChangePlanPage } from '../pages/ChangePlanPage'
import { LoginPage } from '../pages/LoginPage'
import { NetworkPage } from '../pages/NetworkPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { OptimizationListPage } from '../pages/OptimizationListPage'
import { OptimizePage } from '../pages/OptimizePage'
import { ProposalPage } from '../pages/ProposalPage'
import { SandboxExecutionPage } from '../pages/SandboxExecutionPage'
import { PlanningCreatePage } from '../pages/PlanningCreatePage'
import { PlanningListPage } from '../pages/PlanningListPage'
import { PlanningScenarioPage } from '../pages/PlanningScenarioPage'
import { SitePage } from '../pages/SitePage'

export function AppRoutes() {
  const { identity } = useAuth()

  if (!identity) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <Routes>
      <Route path="/login" element={<Navigate to="/network" replace />} />
      <Route element={<AppShell />}>
        <Route path="/planning" element={<PlanningListPage />} />
        <Route path="/planning/new" element={<PlanningCreatePage />} />
        <Route path="/planning/scenarios" element={<Navigate to="/planning" replace />} />
        <Route path="/planning/scenarios/:scenarioId" element={<PlanningScenarioPage />} />
        <Route path="/network" element={<NetworkPage />} />
        <Route path="/network/sites/:siteId" element={<SitePage />} />
        <Route path="/network/cells/:cellId" element={<CellPage />} />
        <Route path="/network/cells/:cellId/optimize" element={<OptimizePage />} />
        <Route path="/optimization" element={<OptimizationListPage />} />
        <Route path="/optimization/proposals/:proposalId" element={<ProposalPage />} />
        <Route path="/change-plans/:planId" element={<ChangePlanPage />} />
        <Route path="/sandbox/executions/:executionId" element={<SandboxExecutionPage />} />
        <Route path="/assurance" element={<AssuranceListPage />} />
        <Route path="/assurance/:caseId" element={<AssuranceCasePage />} />
        <Route path="/ai" element={<AiPage />} />
        <Route path="/" element={<Navigate to="/network" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
