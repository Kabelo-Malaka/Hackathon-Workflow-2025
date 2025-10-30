# Coding Standards

**Critical Rules for AI Agents:**

1. **Never log sensitive data** - No passwords, tokens, or PII in logs
2. **Always use DTOs at API boundaries** - Never expose entities in responses
3. **All database queries via repositories** - No direct EntityManager use
4. **Async for email only** - Don't overuse @Async
5. **Transactional at service layer** - @Transactional on services, not controllers
6. **Validate all inputs** - Use @Valid and Jakarta Bean Validation
7. **Use constructor injection** - Immutable dependencies (final fields)
8. **Return Optional for nullable queries** - Clear API contracts
9. **Lombok for entities only** - Don't overuse @Data on DTOs
10. **Specific exceptions, not generic** - Throw ValidationException, ResourceNotFoundException, etc.

**Naming Conventions:**
- Entities: Singular nouns (User, WorkflowTemplate)
- Repositories: EntityRepository pattern
- Services: EntityService pattern
- Controllers: EntityController pattern
- DTOs: ActionEntityRequest/Response (CreateUserRequest, UserResponse)

**Code Quality Tools:**
- Backend: Checkstyle (optional but recommended)
- Frontend: ESLint + Prettier (enforced)
