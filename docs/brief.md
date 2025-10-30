# Project Brief: Employee Lifecycle Management System

**Version:** 1.0
**Date:** 2025-10-16
**Author:** Business Analyst - Mary

---

## Executive Summary

The Employee Lifecycle Management System is a web-based workflow orchestration platform designed to streamline and track employee onboarding and offboarding processes. The system addresses critical gaps in current manual processes by providing automated task routing, real-time progress tracking, verification checklists, and security-focused offboarding procedures.

**Current Baseline:** This solution is based on Magna BC's existing "Appointment of Staff Procedure" (see `control-documents/onboarding.md`), which uses a paper-based checklist with 10 sections requiring coordination between HR Manager, Line Manager, Tech Support, and Administrators to provision 30+ hardware, software, and access items.

By replacing this manual checklist with an integrated workflow platform and providing Outlook email integration, this system will eliminate lost tasks, ensure consistency across departments, and reduce security risks from orphaned accounts or uncollected equipment.

**Target Users:** HR administrators, line managers, finance teams, and IT/tech support staff across the organization.

**Key Value Proposition:** Transform chaotic, paper-based employee transitions into tracked, consistent, and secure workflows that ensure nothing falls through the cracks.

---

## Problem Statement

### Current State and Pain Points

**Reference Document:** See `control-documents/onboarding.md` for the actual Magna BC onboarding checklist used today.

When a new employee joins the company, HR initiates an "Appointment of Staff Procedure" document that contains multiple sections (A through J) with checkboxes for various tasks. This document must be manually passed between HR Manager, Line Manager, Tech Support Administrator, and other stakeholders. For example, the current process includes:

- **Section A:** HR Manager completes recruitment details and starting date
- **Section B:** HR Manager handles office access (alarm codes, keys, parking)
- **Sections C-G:** Line Manager requests hardware/software; Tech Support completes setup
- **Section H:** Line Manager creates user accounts (Microsoft Loop, ProjectMaster, GitHub, SharePoint, Teams, Miro)
- **Section I:** Tech Support verifies file server access, domain accounts, email, equipment functionality
- **Section J:** Administrator creates users in ESS, Time Doctor, Jira, Magic Draw

However, this paper-based checklist process suffers from several critical issues:

**Lack of Tracking:**
- No centralized system to monitor where each onboarding is in the process (e.g., whether Section C hardware setup is complete)
- Difficult to identify bottlenecks or delayed tasks (e.g., if Tech Support is waiting on Line Manager input)
- No visibility into which stakeholder is responsible for current blockers
- HR and managers cannot easily see progress status across Sections A-J
- The document shows "Duty Flow: HR, Line Manager, Tech Support Administrator" but no actual workflow tracking

**Consistency Problems:**
- When setup requirements change (e.g., adding new PDD software in Section G), the paper checklist must be manually updated
- No standardized checklists per role type—current document shows specific software for "Part-Time Student Contractor" in PDD department, but this varies
- Different employees in similar roles may receive different equipment/access depending on who fills out the form
- New requirements don't automatically flow to in-progress onboardings

**Verification Gaps:**
- Section I has a testing checklist, but completion relies on manual checkbox marking without system enforcement
- No mandatory verification before moving to next section (e.g., Tech Support could skip Section I testing)
- Items can be skipped or forgotten without detection (unchecked boxes in the reference document)
- No audit trail of what was actually provisioned vs. what was requested

**Security Risks:**
- During offboarding, some installed items may be overlooked during cleanup (the reference document shows 30+ software/access items across Sections C-J)
- Accounts may be left active after employee departure (e.g., MS 365, ProjectMaster, GitHub, SharePoint, Teams, Miro, ESS, Time Doctor, Jira, Magic Draw from Section H & J)
- No automatic tracking of what needs to be deprovisioned—offboarding teams must remember or find the original onboarding document
- Equipment collection can be incomplete (laptops, monitors, keyboards, mice, phones from Section C)
- Potential for data breaches from orphaned access (server access, file shares, VPN access from Section D)
- Hardcoded default passwords noted in document ("set all passwords to 1234Un!x") represent security concerns

**Process Inefficiency:**
- The entire process often feels disorganized and rushed
- Heavy reliance on email creates communication silos
- Repeated back-and-forth for missing information
- No clear accountability when things go wrong

### Impact of the Problem

- **Security Risk:** Active accounts for departed employees create potential data breaches
- **Cost Inefficiency:** Uncollected equipment represents financial loss; unused software licenses waste budget
- **Employee Experience:** New hires may not have necessary tools on day one, creating poor first impressions
- **Compliance Risk:** Inability to demonstrate proper offboarding procedures for audits
- **Productivity Loss:** Staff time wasted tracking down status, resending emails, and fixing missed items

### Why Existing Solutions Fall Short

Current email-based workflows lack:
- Structured task management and routing
- Real-time visibility and reporting
- Mandatory verification mechanisms
- Automated offboarding based on onboarding records
- Integration between onboarding and offboarding processes

### Urgency

With increasing security threats and compliance requirements, organizations cannot afford orphaned accounts or inconsistent access management. Every delayed onboarding impacts employee productivity, and every incomplete offboarding poses security risk.

---

## Proposed Solution

### Core Concept

A web-based **Workflow Orchestration Platform** that automates, tracks, and verifies employee onboarding and offboarding processes from start to finish. The platform serves as a single source of truth, routing tasks to appropriate stakeholders, enforcing verification checkpoints, and maintaining a complete audit trail.

### Key Differentiators

**1. Automated Task Routing**
- Pre-built workflow templates automatically assign tasks to the right people
- Parallel and sequential task execution based on dependencies
- No manual forwarding or tracking needed

**2. Real-Time Visibility Dashboard**
- Visual pipeline showing progress for every employee transition
- Color-coded status indicators (not started, in progress, blocked, complete)
- Executive and manager views for oversight
- Bottleneck identification and SLA tracking

**3. Mandatory Verification Checklists**
- Tech support cannot mark tasks complete without checking off every item
- Dynamic checklists generated based on employee role and manager input
- Eliminates forgotten accounts or equipment

**4. Intelligent Offboarding Mirror**
- System automatically creates offboarding checklist from onboarding records
- Ensures perfect symmetry: everything provisioned must be deprovisioned
- Eliminates security gaps from forgotten accounts

**5. Seamless Outlook Integration**
- Tasks arrive as actionable emails within existing workflows
- Click-to-complete buttons update the system
- No separate login required for basic task completion

**6. Flexible Template System**
- Custom field builder allows company-specific requirements
- Conditional task logic adapts workflows based on role/department
- Easy to update templates without code changes

### Why This Solution Will Succeed

- **Fits Existing Workflows:** Integrates with Outlook rather than replacing communication tools
- **Enforces Accountability:** Cannot skip required steps or verifications
- **Provides Visibility:** Management can see problems before they become critical
- **Reduces Cognitive Load:** System remembers what needs to happen; people just execute
- **Security-Focused:** Offboarding is as thorough as onboarding by design
- **Scalable:** Works for 10 or 10,000 employees with same process consistency

---

## Target Users

**Reference:** See roles defined in `control-documents/onboarding.md` - HR Manager, Line Manager, Tech Support Administrator, Administrator (GFR)

### Primary User Segment: HR Administrators

**Profile:**
- HR generalists and administrators responsible for employee lifecycle management (e.g., HR Manager in Section A & B of current process)
- Typically manage 50-500+ employee transitions per year
- Work across multiple systems (HRIS, payroll, benefits)
- Serve as central coordinators for onboarding/offboarding

**Current Behaviors:**
- Manually create and send onboarding documents via email
- Chase down incomplete forms from managers and other departments
- Track progress using spreadsheets or memory
- Field questions about status from executives and hiring managers

**Pain Points:**
- No centralized tracking system
- Time wasted on status updates and follow-ups
- Blame when things fall through cracks despite not being their responsibility
- Difficulty producing audit reports

**Goals:**
- Initiate employee transitions quickly and correctly
- Have real-time visibility into progress
- Ensure all departments complete their responsibilities
- Produce compliance documentation effortlessly

### Secondary User Segment: IT/Tech Support Staff

**Profile:**
- IT administrators, help desk staff, and system administrators (e.g., Tech Support Administrator handling Sections C-G, I in current process)
- Responsible for provisioning accounts, equipment, and access (hardware, software, operating systems, servers, internet)
- Often juggling multiple onboarding/offboarding requests simultaneously
- Expected to remember complex role-specific requirements (e.g., different software for PDD vs. Admin roles)

**Current Behaviors:**
- Receive forwarded paper/email documents with employee setup requests from Line Managers
- Manually track what accounts/equipment to provision across 30+ possible items
- Reference role-specific software lists (e.g., Section G: PDD Software like Magic Draw, IntelliJ, Visual Studio Code)
- Sometimes miss items in lengthy setup lists (Sections C, D, E, F, G)
- Complete Section I testing checklist manually without system enforcement

**Pain Points:**
- Unclear or incomplete requirements from managers
- No standardized checklist per role type
- Easy to forget items, leading to follow-up tickets
- Offboarding requires remembering what was set up months/years ago
- Blame for security issues from missed deprovisioning

**Goals:**
- Know exactly what to set up for each role
- Complete setup tasks efficiently without back-and-forth
- Verify everything is done before marking complete
- Automate offboarding based on onboarding records

### Other Key User Segments

**Line Managers:**
- Need to complete employee detail forms quickly (currently handle Section B office access requests and Sections C-H hardware/software/access requests)
- Want visibility into when their new hire will be ready (currently no tracking mechanism)
- Require ability to specify unique requirements for team members (e.g., selecting specific software from Section E: General, Section F: Admin, Section G: PDD based on role)
- Responsible for creating user accounts in Section H (Microsoft Loop, ProjectMaster, GitHub, SharePoint, Teams, Miro)

**Finance Teams:**
- Handle payroll and benefits setup
- Need clear deadlines and status tracking
- Want confirmation when their tasks are complete

---

## Goals & Success Metrics

### Business Objectives

- **Reduce onboarding completion time by 40%** (from average 5 days to 3 days)
- **Achieve 100% offboarding completion rate** (zero orphaned accounts after 30 days)
- **Decrease onboarding-related support tickets by 60%** (fewer "I don't have access to X" requests)
- **Achieve 95% SLA compliance** for task completion within defined timeframes
- **Reduce equipment loss/uncollected items to <2%** of offboardings

### User Success Metrics

- **HR:** Can initiate new onboarding in <5 minutes
- **Managers:** Complete their portion in <10 minutes with clear guidance
- **Tech Support:** Zero "missed items" incidents after task marked complete
- **All Users:** Can check status of any employee transition in <30 seconds

### Key Performance Indicators (KPIs)

- **Process Completion Time:** Average days from HR initiation to full completion
- **Task SLA Compliance:** Percentage of tasks completed within target timeframe
- **Verification Rate:** Percentage of checklists with 100% item completion
- **Offboarding Hygiene Score:** Percentage of offboardings with all accounts deactivated and equipment collected within 30 days
- **User Adoption Rate:** Percentage of employee transitions managed through the system vs. legacy email process
- **Audit Readiness:** Time required to generate compliance reports (target: <5 minutes)

---

## MVP Scope

### Core Features (Must Have)

- **Process Templates:** Pre-built onboarding and offboarding workflow templates with role-based task variations and drag-and-drop template designer for administrators

- **Task Assignment & Routing:** Automatic task distribution based on employee role and department; support for parallel tasks (finance + tech simultaneously) and sequential dependencies (manager approval before tech setup)

- **Real-Time Tracking Dashboard:** Visual pipeline showing each employee's progress with color-coded status indicators; manager view for team oversight; overdue task alerts and estimated completion dates

- **Outlook Email Integration:** Tasks arrive as actionable Outlook emails with embedded links to web forms; automated notification system with customizable reminders; completion confirmations sent to stakeholders

- **Equipment & Access Checklist:** Dynamic checklists generated from role requirements; mandatory checkbox verification for each item (software, hardware, accounts); cannot mark complete until all items checked; automatic logging for offboarding reference

- **Automated Offboarding Mirror:** System remembers exactly what was provisioned during onboarding; generates reverse checklist for offboarding; verification checkboxes for account deactivation and hardware collection; complete audit trail

- **Custom Field Builder:** HR/Admins can add company-specific fields to templates without code changes; fields flow through to relevant tasks automatically

- **Conditional Task Logic:** Smart templates with "if-then" rules (e.g., "If remote = yes, skip office desk assignment"; "If role = developer, add GitHub access"); reduces manual checklist editing

### Out of Scope for MVP

- Mobile native applications (web responsive design only)
- Integration with Active Directory/HR systems (manual entry for MVP)
- Advanced analytics and reporting dashboards (basic reports only)
- Multi-language support
- Single Sign-On (SSO) integration
- Automated hardware inventory management
- Employee self-service portal
- Notification preferences and customization beyond email
- Workflow version control and rollback
- Advanced escalation with custom rules engine

### MVP Success Criteria

The MVP will be considered successful if:
1. All employee onboardings and offboardings can be initiated and tracked through the system
2. Tech support can complete checklists with 100% verification
3. Offboarding automatically uses onboarding records to generate task lists
4. Managers and HR have real-time visibility into progress
5. System reduces average onboarding time by at least 25%
6. Zero critical security gaps (orphaned admin accounts) in pilot group

---

## Post-MVP Vision

### Phase 2 Features

**Integration Enhancements:**
- Active Directory integration for automated account provisioning/deprovisioning
- HRIS integration to automatically pull employee data
- Asset management system integration for hardware tracking
- Calendar integration for scheduling onboarding activities

**Advanced Workflow Features:**
- Workflow version control and template history
- Advanced escalation engine with custom rules
- Task delegation and subtask management
- Workflow analytics and bottleneck detection
- Approval chains with conditional routing

**User Experience Improvements:**
- Employee self-service portal to view their own progress
- Mobile responsive enhancements and native apps
- Rich notification preferences (Slack, Teams, SMS)
- In-app messaging between stakeholders

**Reporting & Analytics:**
- Executive dashboards with KPI tracking
- Predictive completion date modeling
- Department performance comparisons
- Cost tracking per onboarding/offboarding

### Long-term Vision (1-2 Years)

Expand from onboarding/offboarding to complete **Employee Lifecycle Management:**
- Role changes and internal transfers
- Promotion workflows with access adjustments
- Temporary contractor management
- Leave of absence tracking (pause accounts)
- Performance review process integration
- Training and certification tracking

**Platform Evolution:**
- Become configurable workflow engine for any business process
- White-label solution for other organizations
- AI-powered anomaly detection (flag unusual access patterns)
- Intelligent requirement suggestions based on role analysis

### Expansion Opportunities

- **Vertical Expansion:** Offer industry-specific templates (healthcare, finance, education)
- **Horizontal Expansion:** Apply workflow engine to other HR processes (performance reviews, time-off requests, training enrollment)
- **Market Expansion:** SaaS offering for small-medium businesses without internal IT teams
- **Partner Ecosystem:** Integrate with identity management platforms (Okta, Azure AD) and IT service management tools (ServiceNow, Jira)

---

## Technical Considerations

### Platform Requirements

- **Target Platforms:** Web application accessible via modern browsers (Chrome, Firefox, Edge, Safari)
- **Browser/OS Support:** Latest two versions of major browsers; responsive design for tablet access (no mobile app for MVP)
- **Performance Requirements:**
  - Page load times <2 seconds
  - Dashboard refresh <1 second
  - Support 100 concurrent users
  - Email delivery within 30 seconds of task assignment

### Technology Preferences

- **Frontend:** React with TypeScript for type safety and component reusability; React Redux for state management; RTK Query for API data fetching; MUI (Material-UI) for the UI component library; codegen to generate the API client
- **Backend:** Spring Boot (Java) for robust enterprise application development; Liquibase for database migrations; DTOs (Data Transfer Objects) for controllers; Lombok for boilerplate code reduction; Swagger for API documentation
- **Database:** PostgreSQL for relational data integrity and complex reporting queries
- **Hosting/Infrastructure:** Docker Compose for local development and deployment orchestration

### Architecture Considerations

- **Repository Structure:** Monorepo with separate frontend and backend directories
- **Service Architecture:** Modular monolith with clear service boundaries (WorkflowService, NotificationService, TemplateService, UserService, AuditService)
- **Integration Requirements:**
  - SMTP integration with company Outlook server for outbound emails
  - RESTful API design for future integrations
  - Webhook support for external system notifications (post-MVP)
- **Security/Compliance:**
  - Role-based access control (RBAC) for HR, Manager, Tech, Admin roles
  - Audit logging for all actions (who did what, when)
  - Data encryption at rest and in transit
  - Session management and timeout policies
  - GDPR consideration for employee data retention

---

## Constraints & Assumptions

### Constraints

- **Budget:** Limited to internal development resources; no third-party SaaS subscription budget allocated
- **Timeline:** MVP target is 12-16 weeks from kickoff to pilot deployment
- **Resources:**
  - 1 full-time backend developer (Spring Boot)
  - 1 full-time frontend developer (React)
  - 1 part-time business analyst (requirements refinement)
  - 1 part-time QA engineer
  - Shared DevOps support for deployment
- **Technical:**
  - Must integrate with existing corporate Outlook email server
  - Must deploy to Docker containers on existing infrastructure
  - Cannot modify Active Directory directly (read-only access post-MVP)
  - Limited to PostgreSQL database (company standard)

### Key Assumptions

- Users have reliable internet access and modern browsers
- HR will serve as primary system administrator during pilot
- Tech support staff will adopt checklist-based verification willingly
- Outlook SMTP integration will be sufficient for MVP (no need for Graph API initially)
- Employee data can be manually entered for MVP (integration comes later)
- Existing template structures will satisfy 80%+ of use cases
- 20-30 employee pilot program will provide sufficient validation
- Management will support mandatory use of system once validated
- Current onboarding/offboarding volume (<50/month) will not require advanced scaling initially

---

## Risks & Open Questions

### Key Risks

- **User Adoption Risk:** If stakeholders continue using email in parallel, system becomes just another tool rather than single source of truth. **Mitigation:** Executive sponsorship; make system easier than email; track adoption metrics.

- **Outlook Integration Complexity:** SMTP-only approach may limit interactivity; users might not click email links consistently. **Mitigation:** Design compelling email templates with clear calls-to-action; monitor click-through rates; prepare fallback to manual portal access.

- **Template Flexibility vs. Simplicity:** Balancing configurability (custom fields, conditional logic) with ease of use could lead to overly complex templates. **Mitigation:** Start with simple templates; add complexity incrementally; provide template design best practices.

- **Change Management:** Shifting from informal email processes to structured workflows may face resistance. **Mitigation:** Involve stakeholders early in design; emphasize time savings; provide training and support.

- **Data Migration:** No historical record of what was provisioned for existing employees; offboarding will lack context. **Mitigation:** Accept that system benefits apply to new hires only initially; consider manual data entry for key existing employees.

- **Scope Creep:** Stakeholders may request numerous integration and automation features beyond MVP. **Mitigation:** Strict MVP scope discipline; document Phase 2 backlog; demonstrate quick wins with MVP first.

### Open Questions

- What is the exact volume of onboardings/offboardings per month currently?
- Who will have admin rights to edit templates and workflows?
- What is the company's policy on account retention periods (e.g., disable immediately, delete after 90 days)?
- Are there regulatory/compliance requirements we must satisfy (SOC 2, HIPAA, etc.)?
- What is the escalation chain if tasks remain incomplete?
- How should emergency/urgent offboardings be handled differently?
- What happens if an employee's role changes mid-onboarding?
- Should the system handle contractors differently from full-time employees?
- What reporting does finance/security/compliance need for audits?
- Is there an existing asset management system we should integrate with post-MVP?

### Areas Needing Further Research

- **Email Deliverability:** Test Outlook SMTP integration in corporate environment; verify email template rendering across clients
- **User Workflow Analysis:** Shadow HR, managers, and tech support during actual onboardings to identify workflow nuances
- **Template Requirements Discovery:** Workshop with stakeholders to define all role-based templates needed
- **Security Baseline:** Review current security policies for account provisioning/deprovisioning to ensure system enforces them
- **Competitor Analysis:** Evaluate existing employee onboarding tools (BambooHR, Workday, custom solutions) to identify feature gaps and differentiators
- **Scalability Testing:** Determine future growth projections to validate infrastructure requirements

---

## Appendices

### A. Research Summary

**Brainstorming Session Results:**
A structured brainstorming session was conducted to explore the problem space and generate solution ideas. Key techniques included AI-powered idea generation, which produced multiple solution concepts including workflow orchestration, smart template systems, approval chains, integration hubs, and checklist-driven processes. The Workflow Orchestration Platform emerged as the strongest concept, combining tracking, automation, verification, and security in one cohesive solution.

**Key Insights:**
- The "offboarding mirror" concept—using onboarding records to generate offboarding tasks—was identified as a critical security differentiator
- Stakeholders emphasized the importance of maintaining Outlook email integration rather than forcing adoption of entirely new communication tools
- Conditional task logic was identified as essential for handling diverse role requirements without creating dozens of separate templates

### B. Stakeholder Input

**HR Team Feedback:**
- "We need to see where things are stuck without sending 'just checking in' emails constantly"
- "New managers don't know what information tech support needs, so they skip important details"
- "When someone quits suddenly, we scramble to remember what accounts they had"

**Tech Support Feedback:**
- "We get vague requests like 'set up a developer' but every team has different tool requirements"
- "It's embarrassing when a new hire's manager emails us on day 3 asking why they don't have GitHub access yet"
- "For offboarding, we do our best but honestly, we probably miss stuff because there's no checklist of what we set up 2 years ago"

**Management Feedback:**
- "I don't know if my new hire will be ready on their start date until the day before—or sometimes the day of"
- "It feels like onboarding takes forever but nobody can tell me why"

### C. References

- **Current Onboarding Process:** `control-documents/onboarding.md` - Magna BC "Appointment of Staff Procedure" showing existing checklist with Sections A-J
  - Employee example: Eduan Roux (Part-Time Student Contractor, PDD Department)
  - Demonstrates 30+ individual items requiring setup across hardware, software, access, and verification
  - Shows multi-role workflow: HR Manager → Line Manager → Tech Support → Administrator
  - Illustrates pain points: manual checkboxes, no tracking, paper-based handoffs
- Project brainstorming session results (this session)
- Company HR policies and procedures documentation (to be reviewed)
- IT security and access management policies (to be reviewed)

---

## Next Steps

### Immediate Actions

1. **Stakeholder Review Meeting** - Present this brief to HR leadership, IT management, and finance for feedback and approval (Target: Within 1 week)

2. **Validate Assumptions** - Conduct interviews with HR, tech support, and managers to validate problem statement and solution approach (Target: Within 2 weeks)

3. **Quantify Current State** - Gather baseline metrics on current onboarding/offboarding times, task completion rates, and security gaps (Target: Within 2 weeks)

4. **Technical Feasibility Check** - Verify Outlook SMTP access, Docker infrastructure availability, and PostgreSQL provisioning (Target: Within 1 week)

5. **Prioritize MVP Features** - Conduct workshop with development team to estimate effort and refine MVP scope (Target: Within 3 weeks)

6. **Create Product Requirements Document (PRD)** - Develop detailed functional specifications based on approved project brief (Target: Within 4 weeks)

7. **Assemble Team & Kick Off** - Confirm developer availability, establish sprint cadence, and begin Sprint 0 planning (Target: Within 5 weeks)

### PM Handoff

This Project Brief provides the full context for the **Employee Lifecycle Management System**. The next step is to create a comprehensive **Product Requirements Document (PRD)** that translates this business vision into detailed functional specifications.

**Recommended Approach:**
Work with the Product Owner/Product Manager to create the PRD section by section, using the BMad PRD template. The PRD should detail:
- User stories and acceptance criteria
- Detailed feature specifications
- UI/UX requirements and wireframes
- API contracts and data models
- Security and compliance requirements
- Test scenarios and quality criteria

The PRD will serve as the definitive specification for the development team during implementation.

---

*This Project Brief was created using the BMAD-METHOD™ framework and incorporates insights from structured brainstorming and stakeholder analysis.*
