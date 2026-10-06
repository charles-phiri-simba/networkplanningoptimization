export const VENDOR_IMPORT_PERMISSION_HEADER = 'X-SNIP-VENDOR-IMPORT-PERMISSION'

export const VendorImportPermission = {
  VIEW: 'VIEW_SYNCHRONIZATION_STATUS',
  RECOVERY: 'TRIGGER_RECOVERY_SYNCHRONIZATION',
} as const

export type VendorImportPermissionValue =
  (typeof VendorImportPermission)[keyof typeof VendorImportPermission]

export interface SynchronizationSourceSummaryDto {
  sourceSystem: string
  sourceScope: string
  connectorId: string
  enabled: boolean
  preferredMode?: string | null
  cadenceSeconds?: number | null
}

export interface SynchronizationSourceStateDto {
  present: boolean
  sourceSystem: string
  sourceScope: string
  connectorId: string
  enabled: boolean
  freshness?: string | null
  sourceHealth?: string | null
  recoveryRequired?: boolean | null
  knowledgeConfidence?: string | null
  confidenceReasonCodes?: string | null
  lastTrustedSnapshotId?: string | null
  lastTrustedSynchronizationAt?: string | null
  checkpointStatus?: string | null
  checkpointType?: string | null
}

export interface ImportBatchSummaryDto {
  importId?: string
  status?: string | null
  synchronizationMode?: string | null
  failureCode?: string | null
  error?: string | null
}
