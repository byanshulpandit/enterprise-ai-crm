export type Role = 'ROLE_ADMIN' | 'ROLE_MARKETER';

export interface PageMetadata {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

export interface ApiError {
  code: string;
  message: string;
  timestamp: string;
  requestId: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: ApiError;
  metadata?: {
    timestamp: string;
    requestId: string;
    pagination?: PageMetadata;
  };
}

export interface AuthUser {
  id: number;
  username: string;
  role: Role;
}

export interface AuthTokenResponse {
  token: string;
  tokenType: string;
  user: AuthUser;
}

export interface Customer {
  id: number;
  customerId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  city: string | null;
  country: string | null;
  totalSpend: number;
  visitCount: number;
  lastActiveDate: string | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface CustomerRequestDto {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  city?: string | null;
  country?: string | null;
  totalSpend?: number;
  visitCount?: number;
  lastActiveDate?: string | null;
  tags?: string[];
}

export interface CustomerPatchRequestDto {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string | null;
  city?: string | null;
  country?: string | null;
  totalSpend?: number;
  visitCount?: number;
  lastActiveDate?: string | null;
  tags?: string[];
}

export type RuleField = 'city' | 'totalSpend' | 'visitCount' | 'lastActiveDate' | 'tags';

export type RuleOperator =
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'GREATER_THAN'
  | 'GREATER_THAN_OR_EQUAL'
  | 'LESS_THAN'
  | 'LESS_THAN_OR_EQUAL'
  | 'CONTAINS'
  | 'NOT_CONTAINS'
  | 'IN'
  | 'NOT_IN'
  | 'BEFORE'
  | 'AFTER';

export interface ConditionRuleNode {
  field: RuleField;
  op: RuleOperator;
  value: string | number | string[];
}

export interface LogicalRuleNode {
  operator: 'AND' | 'OR';
  conditions: (LogicalRuleNode | ConditionRuleNode)[];
}

export type RuleNode = LogicalRuleNode | ConditionRuleNode;

export interface Segment {
  id: number;
  name: string;
  description: string | null;
  rules: RuleNode;
  createdBy: number;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface SegmentCreateRequest {
  name: string;
  description?: string;
  rules: RuleNode;
}

export interface SegmentPreviewResponse {
  segmentId: number;
  segmentName: string;
  matchedAudienceCount: number;
  evaluatedAt: string;
}

export type CampaignStatus = 'DRAFT' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface Campaign {
  id: number;
  name: string;
  description: string | null;
  segmentId: number;
  segmentName: string;
  messageTemplate: string;
  status: CampaignStatus;
  personalizationEnabled: boolean;
  aiSummary: string | null;
  createdBy: number;
  createdByName: string;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CampaignCreateRequest {
  name: string;
  description?: string;
  segmentId: number;
  messageTemplate: string;
  personalizationEnabled?: boolean;
}

export interface CampaignLaunchResponse {
  campaignId: number;
  status: CampaignStatus;
  targetAudienceSize: number;
  message: string;
  launchedAt: string;
}

export interface DeliverySummaryResponse {
  campaignId: number;
  campaignStatus: CampaignStatus;
  targetAudienceSize: number;
  pendingCount: number;
  sentCount: number;
  failedCount: number;
  completionPercentage: number;
  terminal: boolean;
}

export interface DeliveryRecordResponse {
  id: number;
  campaignId: number;
  customerId: number;
  customerEmail: string;
  customerName: string;
  message: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  failureReason: string | null;
  processedAt: string | null;
  createdAt: string;
}

export interface UploadRowError {
  row: number;
  email?: string;
  reason: string;
}

export interface UploadResultResponse {
  uploadId: number;
  fileName: string;
  status: 'SUCCESS' | 'PARTIAL_SUCCESS' | 'FAILED';
  totalRecords: number;
  successfulRecords: number;
  failedRecords: number;
  errors: UploadRowError[];
}

export interface UploadHistoryDto {
  id: number;
  uploadedBy: number;
  fileName: string;
  fileType: string;
  totalRows: number;
  successCount: number;
  failureCount: number;
  status: string;
  createdAt: string;
}

export interface AiRuleGenerationResponse {
  prompt: string;
  ruleTree: RuleNode;
  explanation: string;
  fallbackUsed: boolean;
  provider: string;
}

export interface AiSegmentAuditDto {
  id: number;
  userId: number;
  username: string;
  prompt: string;
  fallbackUsed: boolean;
  provider: string;
  createdAt: string;
}

export interface CustomerOverviewReportResponse {
  totalActiveCustomers: number;
  grossCustomerSpend: number;
  averageSpendPerCustomer: number;
  totalVisits: number;
  averageVisitsPerCustomer: number;
  topLocations: { city: string; customerCount: number }[];
}

export interface CampaignReportResponse {
  campaignId: number;
  campaignName: string;
  status: CampaignStatus;
  targetAudienceSize: number;
  metrics: {
    sent: number;
    failed: number;
    deliveryRatePercentage: number;
  };
  timeline: {
    launchedAt: string;
    completedAt: string | null;
    durationSeconds: number;
  };
}

export interface CampaignHistoryItemResponse {
  id: number;
  name: string;
  status: CampaignStatus;
  targetAudienceSize: number;
  sentCount: number;
  failedCount: number;
  launchedAt: string;
  completedAt: string | null;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: Role;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserCreateRequest {
  username: string;
  email: string;
  password: string;
  role: Role;
}
