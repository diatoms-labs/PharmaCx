export type UserRole = 'SYSTEM_ADMIN' | 'DIRECTOR' | 'HEAD_OF_DEPARTMENT' | 'MANAGER' | 'ENGINEER' | 'OPERATOR';

export type DocumentStatus =
  | 'REQUESTED'
  | 'QA_PREPARATION'
  | 'AUTHOR_DRAFT'
  | 'PEER_REVIEW'
  | 'QA_REVIEW'
  | 'APPROVAL'
  | 'PUBLISHED'
  | 'RETIRED';

export type StepStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED' | 'SKIPPED';

export type TrainingStatus = 'ASSIGNED' | 'IN_PROGRESS' | 'READ' | 'QUIZ_PASSED' | 'COMPLETED' | 'FAILED' | 'OVERDUE';

// ── Dynamic collections (replace old Department / DocumentType enums) ──────────

export interface OrganizationalUnit {
  id: string;
  code: string;
  displayName: string;
  parentUnitId: string | null;
  type: string; // DEPARTMENT | DIVISION | TEAM | VIRTUAL
  headUserId: string | null;
  active: boolean;
}

export interface DocumentTypeConfig {
  id: string;
  code: string;
  displayName: string;
  ownerUnitId: string | null;
  allowedUnitIds: string[];
  numberingPrefix: string;
  active: boolean;
}

// ── Editor permissions (per user, all off by default) ─────────────────────────

export interface EditorPermissions {
  canDownload: boolean;
  canPrint: boolean;
  canUpload: boolean;
}

// ── Core domain types ──────────────────────────────────────────────────────────

export interface AppUser {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: UserRole;
  unitId: string;
  unitCode?: string;
  unitDisplayName?: string;
  active: boolean;
  editorPermissions?: EditorPermissions;
}

export interface WorkflowStep {
  stepIndex: number;
  name: string;
  type: string;
  assignedToUserId: string | null;
  assignedToUsername: string | null;
  assignedByUserId: string | null;
  status: StepStatus;
  comment: string | null;
  signatureData: string | null;
  rejectionReason: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface TrainingQuestion {
  questionId: string;
  questionText: string;
  questionType: 'MULTIPLE_CHOICE' | 'TRUE_FALSE';
  options: string[];
  correctAnswerIndex: number;
  explanation: string | null;
}

export interface ControlledDocument {
  id: string;
  title: string;
  documentNumber: string | null;
  documentTypeId: string;
  unitId: string;
  version: number;
  status: DocumentStatus;
  templateFileId: string | null;
  documentFileId: string | null;
  currentStepIndex: number;
  workflowSteps: WorkflowStep[];
  trainingQuestions: TrainingQuestion[];
  requestedBy: string;
  authorId: string | null;
  qaPreparerId: string | null;
  effectiveDate: string | null;
  nextReviewDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface QuizAnswer {
  questionId: string;
  selectedAnswerIndex: number;
}

export interface QuizAttempt {
  attemptNumber: number;
  answers: QuizAnswer[];
  score: number;
  totalQuestions: number;
  passed: boolean;
  attemptedAt: string;
}

export interface TrainingAssignment {
  id: string;
  documentId: string;
  documentTitle: string;
  documentNumber: string;
  documentTypeId: string;
  documentVersion: number;
  traineeUserId: string;
  traineeUsername: string;
  unitId: string;
  assignedByUserId: string | null;
  assignedByUsername: string | null;
  status: TrainingStatus;
  assignedAt: string;
  dueDate: string;
  failedAt: string | null;
  readAt: string | null;
  readDurationSeconds: number | null;
  quizPassedAt: string | null;
  acknowledgedAt: string | null;
  quizAttempts: QuizAttempt[];
  signatureData: string | null;
}

export interface DocumentTemplate {
  id: string;
  name: string;
  documentTypeId: string;
  description: string | null;
  fileStorageId: string | null;
  version: number;
  latest: boolean;
  active: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuditEvent {
  id: string;
  timestamp: string;
  userId: string;
  username: string;
  action: string;
  resourceType: string;
  resourceId: string;
  resourceName: string;
  reason: string | null;
}

export interface UserFolder {
  id: string;
  name: string;
  ownerId: string;
  ownerUsername: string;
  parentFolderId: string | null;
  ownerUnitId: string | null;
  folderType: string; // PERSONAL | DEPARTMENT | SHARED
  policyId: string | null;
  allowedDocumentTypeIds: string[];
  sharedWithAll: boolean;
  sharedWithUserIds: string[];
  documentIds: string[];
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  username: string;
  fullName: string;
  role: UserRole;
  unitId: string;
  unitCode: string;
  unitDisplayName: string;
}

export interface EditorConfig {
  documentServerUrl: string;
  config: Record<string, unknown>;
  mode: string;
  features: {
    canEdit: boolean;
    canReview: boolean;
    canComment: boolean;
    trackChanges: boolean;
    canAcceptRejectChanges: boolean;
    canDownload: boolean;
    canPrint: boolean;
    versionHistory: boolean;
    protection: boolean;
  };
}

export interface DocumentDetailResponse {
  document: ControlledDocument;
  editorConfig: EditorConfig | null;
  features: Record<string, boolean> | null;
}

export interface TemplateDetailResponse {
  template: DocumentTemplate;
  editorConfig: EditorConfig | null;
}

// ── Access control response ────────────────────────────────────────────────────

export interface ContactInfo {
  reason: string;
  name: string;
  role: string;
  email: string;
}

export interface AccessDeniedResponse {
  error: 'ACCESS_DENIED';
  code: string;
  message: string;
  detail: string;
  contact: ContactInfo;
}
