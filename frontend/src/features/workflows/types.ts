/**
 * TypeScript type definitions for Workflow feature
 * Story 3.7: Initiate Workflow UI
 */

export interface InitiateWorkflowFormData {
  templateId: string;
  employeeName: string;
  employeeEmail: string;
  employeeRole: string;
  customFieldValues: Record<string, unknown>;
}

export interface TemplateCustomField {
  id: string;
  name: string;
  label: string;
  fieldType: 'TEXT' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'SELECT';
  required: boolean;
  defaultValue?: string;
  selectOptions?: string[];
  conditionalRules?: ConditionalRule[];
}

export interface ConditionalRule {
  id: string;
  targetFieldName: string;
  operator: 'EQUALS' | 'NOT_EQUALS' | 'CONTAINS';
  value: string;
}

export interface WorkflowTemplate {
  id: string;
  name: string;
  workflowType: 'ONBOARDING' | 'OFFBOARDING';
  description?: string;
  isActive: boolean;
  customFields?: TemplateCustomField[];
}

export interface InitiateWorkflowRequest {
  templateId: string;
  employeeName: string;
  employeeEmail: string;
  employeeRole: string;
  customFieldValues: Record<string, unknown>;
}

export interface WorkflowInitiationResponse {
  workflowInstanceId: string;
  message: string;
}
