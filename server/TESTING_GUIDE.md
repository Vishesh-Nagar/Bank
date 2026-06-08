# Bank Application Testing Guide

This guide outlines how to create, run, and learn from tests in the Bank application. It covers both the Vite React frontend and the Spring Boot backend.

---

## 1. Client-Side Tests (Frontend)

We use **Vitest** as the test runner and **React Testing Library** for component interactions. This combination is fast and natively supports Vite's build environment.

### How to Run Client Tests
From the `client/` directory, run:
```bash
npm run test
```
- To run tests in watch mode (reruns tests when files change), run: `npx vitest`
- To run tests with UI reporting: `npx vitest --ui`

### How to Create Client Tests
1. **Naming Convention:** Name your files `[ComponentName].test.tsx` or `[serviceName].test.ts`. Place them next to the files they test.
2. **Mocking Services:** When testing a UI component, mock the external API calls so you don't hit a real server.
   ```tsx
   import { vi } from "vitest";
   import * as paymentService from "../../../services/paymentService";
   
   vi.mock("../../../services/paymentService");
   (paymentService.initiatePayment as any).mockResolvedValue({});
   ```
3. **Querying the DOM:** Use queries like `getByRole`, `getByLabelText`, or `getByText` to find elements as a user would.
   ```tsx
   import { render, screen, fireEvent } from "@testing-library/react";
   
   render(<MyComponent />);
   fireEvent.click(screen.getByRole("button", { name: /Submit/i }));
   expect(screen.getByText(/Success/i)).toBeInTheDocument();
   ```

### What to Learn
- **User-Centric Testing:** React Testing Library forces you to interact with the DOM similarly to a real user. Instead of checking a component's internal state, you check what is visibly rendered.
- **Mocking Boundaries:** You learn where the frontend "stops" and the backend "begins". By mocking `api.get` or `api.post`, you isolate your UI logic from network fragility.

---

## 2. Server-Side Tests (Backend)

We use **JUnit 5** alongside **Mockito** for unit testing, and Spring Boot's **MockMvc** for controller integration testing.

### How to Run Server Tests
From the `server/` directory, run:
```bash
./mvnw test
```
To run a specific test class:
```bash
./mvnw test -Dtest="PaymentServiceImplTest"
```

### How to Create Server Tests

#### Testing Services (Business Logic)
1. Use `@ExtendWith(MockitoExtension.class)` on the class.
2. Annotate dependencies (like Repositories or other services) with `@Mock`.
3. Annotate the service you are testing with `@InjectMocks`.
4. Define mock behaviors using `when(...).thenReturn(...)` and verify interactions using `verify(...)`.

```java
@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void testDeposit() {
        // 1. Setup mock behavior
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(mockAccount);
        
        // 2. Execute business logic
        accountService.deposit(1L, new BigDecimal("50.00"));
        
        // 3. Assert and Verify
        assertEquals(new BigDecimal("150.00"), mockAccount.getBalance());
        verify(accountRepository, times(1)).save(mockAccount);
    }
}
```

#### Testing Controllers (HTTP Boundaries)
1. Use `@WebMvcTest(YourController.class)` combined with `@AutoConfigureMockMvc(addFilters = false)` if you want to bypass heavy JWT security filters for pure logic tests.
2. Use `@MockBean` for any Services the controller relies on.
3. Use `mockMvc.perform(...)` to simulate HTTP requests.

```java
@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AccountControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    void testGetAccount() throws Exception {
        when(accountService.getAccountById(1L)).thenReturn(mockDto);

        mockMvc.perform(get("/api/accounts/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.balance").value(100.00));
    }
}
```

### What to Learn
- **Idempotency and Concurrency:** When mocking components like Kafka Listeners or transactional databases, testing forces you to consider edge cases (e.g., "what happens if this message is consumed twice?").
- **Contract Enforcement:** Using `MockMvc` with `jsonPath` teaches you to strictly adhere to JSON payload schemas. If you rename a field in Java, the JSON path test will fail, preventing accidental API breakages.
- **Decoupling Architecture:** If a class is difficult to write a `@Mock` for, it often indicates the class is doing too much and should be refactored into smaller, decoupled services.
