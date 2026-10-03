// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT; python tools/ui/generate.py
import type { ControlAgent, ControlAgentPage, ControlDevice, ControlDevicePage, ControlPolicy, ControlPolicyPage, ControlTeam, ControlTeamPage, ControlTool, ControlToolPage, ControlUser, ControlUserPage } from '@olo-labs/toolgate-contracts';
export const operations = {
  clientDiscovery: { method: 'GET', path: '/.well-known/olo-toolgate-client' },
  startEndpointEnrollment: { method: 'POST', path: '/api/control/v1/endpoint/enrollments' },
  pollEndpointEnrollment: { method: 'POST', path: '/api/control/v1/endpoint/enrollments/poll' },
  reviewEndpointEnrollment: { method: 'GET', path: '/api/control/v1/endpoint/enrollments/review' },
  decideEndpointEnrollment: { method: 'POST', path: '/api/control/v1/endpoint/enrollments/decision' },
  endpointCheckIn: { method: 'POST', path: '/api/control/v1/endpoint/check-in' },
  getEndpointDevice: { method: 'GET', path: '/api/control/v1/endpoint/devices/{id}' },
  revokeEndpointDevice: { method: 'POST', path: '/api/control/v1/endpoint/devices/{id}/revoke' },
  listApprovals: { method: 'GET', path: '/api/control/v1/approvals' },
  getApproval: { method: 'GET', path: '/api/control/v1/approvals/{id}' },
  decideApproval: { method: 'POST', path: '/api/control/v1/approvals/{id}/decision' },
  resolveApproval: { method: 'POST', path: '/api/control/v1/approvals/resolve' },
  consumeApprovalPermit: { method: 'POST', path: '/api/control/v1/approvals/permits/consume' },
  getCurrentPolicyBundle: { method: 'GET', path: '/api/control/v1/bundles/current' },
  getPolicyBundleVersion: { method: 'GET', path: '/api/control/v1/bundles/versions/{sequence}' },
  publishPolicyBundle: { method: 'POST', path: '/api/control/v1/bundles/publish' },
  rollbackPolicyBundle: { method: 'POST', path: '/api/control/v1/bundles/rollback' },
  getControlOpenApi: { method: 'GET', path: '/api/control/v1/openapi' },
  listControlUser: { method: 'GET', path: '/api/control/v1/users' },
  createControlUser: { method: 'POST', path: '/api/control/v1/users' },
  getControlUser: { method: 'GET', path: '/api/control/v1/users/{id}' },
  updateControlUser: { method: 'PUT', path: '/api/control/v1/users/{id}' },
  deleteControlUser: { method: 'DELETE', path: '/api/control/v1/users/{id}' },
  listControlTeam: { method: 'GET', path: '/api/control/v1/teams' },
  createControlTeam: { method: 'POST', path: '/api/control/v1/teams' },
  getControlTeam: { method: 'GET', path: '/api/control/v1/teams/{id}' },
  updateControlTeam: { method: 'PUT', path: '/api/control/v1/teams/{id}' },
  deleteControlTeam: { method: 'DELETE', path: '/api/control/v1/teams/{id}' },
  listControlAgent: { method: 'GET', path: '/api/control/v1/agents' },
  createControlAgent: { method: 'POST', path: '/api/control/v1/agents' },
  getControlAgent: { method: 'GET', path: '/api/control/v1/agents/{id}' },
  updateControlAgent: { method: 'PUT', path: '/api/control/v1/agents/{id}' },
  deleteControlAgent: { method: 'DELETE', path: '/api/control/v1/agents/{id}' },
  listControlTool: { method: 'GET', path: '/api/control/v1/tools' },
  createControlTool: { method: 'POST', path: '/api/control/v1/tools' },
  getControlTool: { method: 'GET', path: '/api/control/v1/tools/{id}' },
  updateControlTool: { method: 'PUT', path: '/api/control/v1/tools/{id}' },
  deleteControlTool: { method: 'DELETE', path: '/api/control/v1/tools/{id}' },
  listControlPolicy: { method: 'GET', path: '/api/control/v1/policies' },
  createControlPolicy: { method: 'POST', path: '/api/control/v1/policies' },
  getControlPolicy: { method: 'GET', path: '/api/control/v1/policies/{id}' },
  updateControlPolicy: { method: 'PUT', path: '/api/control/v1/policies/{id}' },
  deleteControlPolicy: { method: 'DELETE', path: '/api/control/v1/policies/{id}' },
  listControlDevice: { method: 'GET', path: '/api/control/v1/devices' },
  createControlDevice: { method: 'POST', path: '/api/control/v1/devices' },
  getControlDevice: { method: 'GET', path: '/api/control/v1/devices/{id}' },
  updateControlDevice: { method: 'PUT', path: '/api/control/v1/devices/{id}' },
  deleteControlDevice: { method: 'DELETE', path: '/api/control/v1/devices/{id}' },
  exportConfig: { method: 'GET', path: '/api/control/v1/config/export' },
  importConfig: { method: 'POST', path: '/api/control/v1/config/import' },
  listAudit: { method: 'GET', path: '/api/control/v1/audit' },
} as const;
export interface DirectoryRecords {
  agents: ControlAgent;
  devices: ControlDevice;
  policies: ControlPolicy;
  teams: ControlTeam;
  tools: ControlTool;
  users: ControlUser;
}
export interface DirectoryPages {
  agents: ControlAgentPage;
  devices: ControlDevicePage;
  policies: ControlPolicyPage;
  teams: ControlTeamPage;
  tools: ControlToolPage;
  users: ControlUserPage;
}
export type DirectoryKind = keyof DirectoryRecords;
export const listOperations = {
  agents: operations.listControlAgent,
  devices: operations.listControlDevice,
  policies: operations.listControlPolicy,
  teams: operations.listControlTeam,
  tools: operations.listControlTool,
  users: operations.listControlUser,
} as const;
