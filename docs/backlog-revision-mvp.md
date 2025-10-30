# Backlog Revision: MVP Scope Reduction

## Document Information

| Field | Value |
|-------|-------|
| **Document Type** | Backlog Revision |
| **Version** | 1.0 |
| **Date** | 2025-10-30 |
| **Author** | Sarah (Product Owner) |
| **Status** | Proposed for Approval |
| **Related Documents** | docs/prd.md, docs/stories/*.story.md |

---

## Executive Summary

This document formalizes a scope reduction for the Employee Lifecycle Management System MVP to focus on core workflow functionality while maintaining the unique value proposition (offboarding mirror security feature). The revision reduces remaining work from 29 stories to 20 stories (**31% reduction**), making the project more achievable within time and resource constraints.

### Scope Decision: OPTION 1 - Minimal Viable Product

**What's Included in MVP:**
- ✅ Epic 1: Foundation & Authentication (complete)
- ✅ Epic 2: Workflow Template Management (simplified - stories 2.1-2.5 only)
- ✅ Epic 3: Workflow Execution & Task Routing (complete)
- ✅ Epic 4: Task Completion & Verification (complete - includes offboarding mirror)

**What's Deferred to Future Phase:**
- 🔮 Epic 2: Stories 2.6, 2.7 (Custom Fields, Conditional Logic)
- 🔮 Epic 5: Dashboard & Visibility (all 8 stories)

**Rationale:**
1. Epic 1 is nearly complete (6/8 done) - finish what's started
2. Epic 4's offboarding mirror (Story 4.7) is the key security differentiator - must keep
3. Epic 2 simplified still provides template management, just with fixed structure instead of custom fields
4. Epic 5 provides reporting/visibility but not core workflow functionality - deferrable
5. Basic task lists (Epic 4.8) provide sufficient visibility for MVP without Kanban dashboards

---

## Current Sprint Status

### Epic 1: Foundation & Authentication
**Status: 75% Complete (6/8 Done)**

| Story | Title | Status | Notes |
|-------|-------|--------|-------|
| 1.1 | Project Repository & Monorepo Setup | ✅ Done | Completed |
| 1.2 | Docker Compose Infrastructure | ✅ Done | Completed |
| 1.3 | Database Schema Foundation & Liquibase Setup | ✅ Done | Completed |
| 1.4 | Authentication & Session Management | ✅ Done | Completed |
| 1.5 | User Management CRUD | ✅ Done | Completed |
| 1.6 | Frontend Authentication UI & Routing | ✅ Done | Completed |
| 1.7 | Basic User Management UI (HR Admin) | 📋 Approved | Ready for dev |
| 1.8 | Testing Framework Setup | 📋 Approved | Ready for dev |

**Action:** Complete Stories 1.7 and 1.8 to finish Epic 1.

---

## Revised MVP Backlog

### Epic 2: Workflow Template Management (SIMPLIFIED)
**Status: Not Started**
**Stories in MVP: 5 of 7** (Stories 2.6 and 2.7 moved to Future Phase)

| Story | Title | Status | Priority | Notes |
|-------|-------|--------|----------|-------|
| 2.1 | Workflow Template Data Model | 📝 Backlog | HIGH | **Keep** - Foundation |
| 2.2 | Template CRUD API Endpoints | 📝 Backlog | HIGH | **Keep** - Core API |
| 2.3 | Template Builder Backend Services | 📝 Backlog | HIGH | **Keep** - Business logic |
| 2.4 | Template Library UI | 📝 Backlog | HIGH | **Keep** - Browse templates |
| 2.5 | Template Builder Form - Basic Info & Tasks | 📝 Backlog | HIGH | **Keep** - Create templates |
| ~~2.6~~ | ~~Template Builder Form - Custom Fields~~ | 🔮 Future | LOW | **DEFERRED** - Nice to have |
| ~~2.7~~ | ~~Template Builder Form - Conditional Task Logic~~ | 🔮 Future | LOW | **DEFERRED** - Complex feature |

**MVP Scope Changes:**
- Templates will have **fixed structure** (no custom fields)
- Tasks cannot have conditional show/hide logic
- Templates define task sequences with role assignments only
- **Workaround:** Use multiple template variants instead of conditional logic (e.g., "Onboarding - Remote Employee" vs "Onboarding - Office Employee")

**Technical Impact:**
- Remove `template_custom_fields` table from Story 2.1 schema
- Remove `template_conditional_rules` table from Story 2.1 schema
- Simplify validation logic in Story 2.3
- Reduce UI complexity in Story 2.5

---

### Epic 3: Workflow Execution & Task Routing (COMPLETE)
**Status: Not Started**
**Stories in MVP: 7 of 7** (All stories included)

| Story | Title | Status | Priority | Notes |
|-------|-------|--------|----------|-------|
| 3.1 | Workflow Instance Data Model | 📝 Backlog | HIGH | **Keep** - Core functionality |
| 3.2 | Workflow Instantiation Service | 📝 Backlog | HIGH | **Keep** - Creates workflows |
| 3.3 | Task Assignment & Routing Logic | 📝 Backlog | HIGH | **Keep** - Auto-assigns tasks |
| 3.4 | Workflow State Management | 📝 Backlog | HIGH | **Keep** - State transitions |
| 3.5 | Workflow Initiation API | 📝 Backlog | HIGH | **Keep** - API endpoints |
| 3.6 | Workflow List & Detail API | 📝 Backlog | HIGH | **Keep** - View workflows |
| 3.7 | Initiate Workflow UI (HR Admin) | 📝 Backlog | HIGH | **Keep** - HR initiates workflows |

**No Changes** - This epic is critical and fully scoped for MVP.

**Technical Note:** Since Epic 2 no longer includes custom fields (2.6), Story 3.2 and 3.7 will work with fixed employee data fields only (name, email, role, start_date).

---

### Epic 4: Task Completion & Verification (COMPLETE)
**Status: Not Started**
**Stories in MVP: 8 of 8** (All stories included - this is the core value)

| Story | Title | Status | Priority | Notes |
|-------|-------|--------|----------|-------|
| 4.1 | Task Checklist Data Model | 📝 Backlog | HIGH | **Keep** - Checklist foundation |
| 4.2 | Task Completion Service with Checklist Validation | 📝 Backlog | HIGH | **Keep** - Validation logic |
| 4.3 | Task Completion API | 📝 Backlog | HIGH | **Keep** - Complete tasks API |
| 4.4 | Email Notification Service & Templates | 📝 Backlog | HIGH | **Keep** - Email notifications |
| 4.5 | Task Assignment Email Triggers | 📝 Backlog | HIGH | **Keep** - Auto-send emails |
| 4.6 | Task Completion Form UI | 📝 Backlog | HIGH | **Keep** - UI for task completion |
| 4.7 | Automated Offboarding Mirror | 📝 Backlog | **CRITICAL** | ⭐ **KEY DIFFERENTIATOR** - Security value |
| 4.8 | Task List & Queue UI | 📝 Backlog | HIGH | **Keep** - Visibility (replaces Epic 5 dashboards) |

**No Changes** - This epic delivers the core security value proposition and must be fully implemented.

**Email Simplification (Optional Optimization):**
- Story 4.4: Can use simple HTML templates instead of complex Thymeleaf rendering if time is tight
- Story 4.5: Focus on task assignment emails only; defer completion notifications if needed

---

### Epic 5: Dashboard & Visibility (DEFERRED)
**Status: Not Started**
**Stories in MVP: 0 of 8** (Entire epic moved to Future Phase)

| Story | Title | Status | Priority | Notes |
|-------|-------|--------|----------|-------|
| ~~5.1~~ | ~~Dashboard Data Aggregation API~~ | 🔮 Future | LOW | **DEFERRED** |
| ~~5.2~~ | ~~Kanban Dashboard UI~~ | 🔮 Future | LOW | **DEFERRED** |
| ~~5.3~~ | ~~Dashboard Filtering & Search~~ | 🔮 Future | LOW | **DEFERRED** |
| ~~5.4~~ | ~~Workflow Detail View~~ | 🔮 Future | MEDIUM | **DEFERRED** (Story 3.6 provides basic view) |
| ~~5.5~~ | ~~Audit Trail View~~ | 🔮 Future | LOW | **DEFERRED** |
| ~~5.6~~ | ~~Audit Event Capture Service~~ | 🔮 Future | LOW | **DEFERRED** |
| ~~5.7~~ | ~~Basic Reporting API~~ | 🔮 Future | LOW | **DEFERRED** |
| ~~5.8~~ | ~~Export & Download UI~~ | 🔮 Future | LOW | **DEFERRED** |

**MVP Workaround for Visibility:**
- Use Story 4.8 (Task List & Queue UI) for personal task views
- Use Story 3.6 (Workflow List & Detail API) with basic table UI instead of Kanban
- Email notifications (Story 4.5) provide proactive visibility
- No audit trail or reporting in MVP - can be added later

**Future Phase Priority:**
- Stories 5.2 (Kanban Dashboard) and 5.4 (Workflow Detail View) should be prioritized first in Phase 2
- Stories 5.5-5.8 (Audit/Reporting) are lower priority for Phase 2+

---

## Revised Story Count & Effort Estimate

### Original Backlog (Full PRD Scope)
- **Total Stories:** 37 stories across 5 epics
- **Remaining After Epic 1:** 29 stories (Epics 2-5)

### Revised MVP Backlog
| Epic | Original Stories | MVP Stories | Deferred Stories |
|------|------------------|-------------|------------------|
| Epic 1 | 8 | 8 ✅ | 0 |
| Epic 2 | 7 | 5 | 2 (2.6, 2.7) |
| Epic 3 | 7 | 7 | 0 |
| Epic 4 | 8 | 8 | 0 |
| Epic 5 | 8 | 0 | 8 |
| **TOTAL** | **37** | **28** | **10** |

### Remaining Work After Epic 1
- **MVP Remaining:** 20 stories (Epics 2-4, simplified)
- **Deferred to Future:** 10 stories
- **Scope Reduction:** 31% fewer stories than original plan

### Estimated Timeline Impact
Assuming 2 developers working full-time with part-time QA:

| Scenario | Stories | Est. Weeks | Notes |
|----------|---------|------------|-------|
| **Original Plan** (Epics 2-5 full) | 29 stories | 12-16 weeks | 1.8-2.4 stories/week |
| **Revised MVP** (Epics 2-4 simplified) | 20 stories | 8-11 weeks | 1.8-2.5 stories/week |
| **Savings** | -9 stories | **-4 to -5 weeks** | **31% reduction** |

**Critical Path:**
1. **Weeks 1-2:** Complete Epic 1 (Stories 1.7, 1.8)
2. **Weeks 3-5:** Epic 2 simplified (Stories 2.1-2.5)
3. **Weeks 6-9:** Epic 3 complete (Stories 3.1-3.7)
4. **Weeks 10-12:** Epic 4 complete (Stories 4.1-4.8)

---

## PRD Updates Required

The following sections of `docs/prd.md` need updates to reflect this scope change:

### 1. Functional Requirements (Section: Requirements)
**Changes:**
- ~~FR13: Allow HR/Admin users to add custom fields to workflow templates~~ → **Defer to Future Phase**
- ~~FR14: Support conditional task logic with if-then rules~~ → **Defer to Future Phase**
- ~~FR17: Maintain a complete audit trail~~ → **Modify to: "Basic action logging for critical events"**

### 2. Non-Functional Requirements
**No changes** - All NFRs remain valid for MVP scope.

### 3. Epic List (Section: Epic List)
**Add note to Epic 2:**
> **MVP Scope:** Stories 2.1-2.5 only. Custom fields (2.6) and conditional logic (2.7) deferred to Phase 2.

**Add note to Epic 5:**
> **Deferred to Phase 2:** Entire epic moved out of MVP scope. Basic visibility provided by Epic 4.8 (Task Queue UI) and Epic 3.6 (Workflow List API).

### 4. User Interface Design Goals (Section: Core Screens and Views)
**Remove/Modify:**
- ~~Kanban Dashboard~~ → Replace with "Basic Workflow List View"
- ~~Audit Trail View~~ → Remove from MVP
- ~~Email Preview~~ → Remove from MVP (use fixed templates)

**Keep:**
- Dashboard (simplified table view instead of Kanban)
- Employee Detail View
- Task Completion Form
- Template Builder (simplified without custom fields)
- Template Library
- Task Queue

---

## Updated Feature Set Summary

### What Users CAN Do in MVP

**HR Administrators:**
- ✅ Create user accounts with role assignments
- ✅ Create workflow templates (fixed structure with task sequences)
- ✅ Initiate onboarding/offboarding workflows for employees
- ✅ View list of all active workflows and their status
- ✅ View assigned tasks across the organization

**Line Managers / Tech Support / Administrators:**
- ✅ Receive email notifications when tasks are assigned
- ✅ Complete tasks with mandatory checklist verification
- ✅ View personal task queue sorted by due date
- ✅ Mark tasks complete after verifying all items

**System Features:**
- ✅ Automatic task routing based on role
- ✅ Workflow state management (Initiated → In Progress → Completed)
- ✅ **Offboarding mirror** - automatically generates deprovisioning checklists from onboarding records
- ✅ Email notifications for task assignments
- ✅ Session-based authentication with role-based access control

### What Users CANNOT Do in MVP (Deferred)

**Deferred to Phase 2:**
- ❌ Add custom fields to workflow templates (use fixed employee data fields)
- ❌ Create conditional task logic (use multiple template variants instead)
- ❌ View Kanban-style dashboard (use basic table view)
- ❌ Generate reports or export data
- ❌ View detailed audit trail
- ❌ Filter workflows with advanced search

**Workarounds for MVP:**
- **No Custom Fields:** Use fixed fields (employee_name, employee_email, employee_role, start_date, department)
- **No Conditional Logic:** Create separate templates for different scenarios (e.g., "Remote Onboarding Template" vs "Office Onboarding Template")
- **No Kanban Dashboard:** Use Story 3.6 API to build simple table view showing workflows with status
- **No Reporting:** Export data manually from database or wait for Phase 2

---

## Risk Assessment

### Risks of Scope Reduction

| Risk | Impact | Mitigation |
|------|--------|------------|
| **User dissatisfaction with lack of custom fields** | MEDIUM | Provide 5-6 common fields that cover 80% of use cases; clearly communicate Phase 2 timeline |
| **Template management becomes tedious without conditional logic** | LOW | Create multiple template variants; most orgs have 2-3 standard onboarding types anyway |
| **Lack of visibility without dashboards** | MEDIUM | Story 4.8 (Task Queue) and Story 3.6 (Workflow List) provide basic visibility; email notifications help |
| **No audit trail for compliance** | LOW | Database audit columns (created_by, created_at, updated_at) still capture basic audit data; full UI deferred |

### Risks of NOT Reducing Scope

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Project timeline overrun by 4-5 weeks** | HIGH | Scope reduction addresses this |
| **Team burnout from overly ambitious scope** | HIGH | Scope reduction addresses this |
| **Delivering incomplete features rather than complete reduced scope** | CRITICAL | Scope reduction addresses this |

**Recommendation:** Risks of scope reduction are manageable with good communication and workarounds. Risks of NOT reducing scope are severe.

---

## Communication Plan

### Stakeholder Messaging

**To Development Team:**
> "We're streamlining the MVP to focus on core workflow functionality. This means:
> - Finish Epic 1 (almost done!)
> - Simplified Epic 2 (no custom fields)
> - Full Epics 3 & 4 (including the key offboarding mirror feature)
> - Epic 5 deferred to Phase 2
>
> This reduces remaining work by 31% while keeping all critical security features. We're building for success, not burnout."

**To Business Stakeholders:**
> "The MVP will deliver the core security value: automated onboarding/offboarding with mandatory checklists and the offboarding mirror that prevents orphaned accounts. We're deferring advanced features (custom fields, fancy dashboards) to Phase 2 to ensure we deliver a solid, working product on time."

**To Users:**
> "The first release will support your core workflows with fixed templates. You'll be able to manage employee transitions, verify all provisioned items, and ensure nothing is missed during offboarding. We'll add customization and advanced reporting in future releases based on your feedback."

---

## Acceptance Criteria for Scope Change

This backlog revision is approved when:

- [ ] Product Owner (Sarah) approves this document
- [ ] Scrum Master (Bob) acknowledges updated sprint planning
- [ ] Development team confirms understanding of revised scope
- [ ] PRD is updated with scope annotations (see "PRD Updates Required" section)
- [ ] Epic 2 Stories 2.6 and 2.7 are marked as "Future Phase" status
- [ ] Epic 5 Stories 5.1-5.8 are marked as "Future Phase" status
- [ ] This document is committed to git and referenced in project documentation

---

## Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2025-10-30 | 1.0 | Initial backlog revision - MVP scope reduction (Option 1) | Sarah (Product Owner) |

---

## Approval

**Product Owner Approval:**
- [ ] Approved by: ___________________ (Sarah)
- [ ] Date: _________________
- [ ] Signature: ___________________

**Scrum Master Acknowledgment:**
- [ ] Acknowledged by: ___________________ (Bob)
- [ ] Date: _________________

**Development Team Acknowledgment:**
- [ ] Dev Lead: ___________________
- [ ] Date: _________________

---

## Appendix: Story-by-Story Comparison

### Epic 2 Changes Detail

#### Story 2.1: Workflow Template Data Model
**Original AC:** 7 acceptance criteria including custom fields and conditional rules tables
**Revised AC:** 5 acceptance criteria - remove AC for `template_custom_fields` and `template_conditional_rules` tables
**Impact:** Simpler schema, faster development

#### Story 2.3: Template Builder Backend Services
**Original AC:** 10 acceptance criteria including custom field and conditional rule validation
**Revised AC:** 7 acceptance criteria - remove validation for custom fields and circular dependencies in rules
**Impact:** Less complex business logic

#### Story 2.5: Template Builder Form - Basic Info & Tasks
**Original AC:** 11 acceptance criteria
**Revised AC:** 11 acceptance criteria (no change - this story doesn't include custom fields UI)
**Impact:** No change

#### Stories 2.6 & 2.7: REMOVED
**Impact:** -2 stories, significant UI complexity reduction

---

**END OF DOCUMENT**
