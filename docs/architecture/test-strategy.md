# Test Strategy

**Approach:** Unit + Integration testing (no E2E for MVP)

**Coverage Goals:**
- Service layer: 80% unit test coverage
- Controllers: Happy path + critical errors (integration tests)
- UI: Component tests for forms and complex logic

**Backend Testing:**

**Unit Tests (JUnit 5 + Mockito):**
```java
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    @Mock private WorkflowInstanceRepository workflowRepo;
    @Mock private NotificationService notificationService;
    @InjectMocks private WorkflowService workflowService;

    @Test
    void createWorkflowInstance_shouldEvaluateConditionalRules() {
        // Test conditional logic evaluation
    }
}
```

**Integration Tests (TestContainers):**
```java
@SpringBootTest
@Testcontainers
class WorkflowControllerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.2-alpine");

    @Autowired private WebTestClient webClient;

    @Test
    void initiateWorkflow_shouldCreateWorkflowAndAssignTasks() {
        // Full integration test with real database
    }
}
```

**Frontend Testing (Jest + React Testing Library):**
```typescript
describe('TaskCompletionForm', () => {
  it('should disable submit until all items checked', () => {
    render(<TaskCompletionForm />);
    expect(screen.getByRole('button', { name: /complete/i })).toBeDisabled();
    // Check all items
    expect(screen.getByRole('button', { name: /complete/i })).toBeEnabled();
  });
});
```

**Manual Testing:**
- Email delivery and rendering (Postman for API triggers)
- Full user workflows (HR initiates → Tech completes → Dashboard updates)
- Offboarding mirror verification
