# Retail Support Agent — Core Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a working, end-to-end retail customer support agent — one orchestrator routing
to four specialist sub-agents (Order Support, Returns, Product Discovery, FAQ/Policy) — as a new
Gradle project in this repo, runnable and manually verifiable exactly like the numbered demos in
`java/google-adk-gradle`.

**Architecture:** Each specialist wraps a small in-memory "backend service" (simulating a real
order/returns/catalog system) behind `FunctionTool`s, except FAQ/Policy which uses an `McpToolset`
against a companion Python `fastmcp` server (same mechanism as `p6_mcp_tools`). An orchestrator
`LlmAgent` with no domain tools delegates every turn via ADK's built-in `transfer_to_agent`
sub-agent mechanism (proven in `p2_multi_agent`).

**Tech Stack:** Java 17, Gradle, `com.google.adk:google-adk` 1.4.0, `com.google.adk:google-adk-langchain4j` 1.4.0, `dev.langchain4j` 1.12.2 (Groq via `OpenAiChatModel`/`OpenAiStreamingChatModel`), JUnit 5, Python 3 + `fastmcp` for the policy MCP server.

**Spec:** `docs/superpowers/specs/2026-09-04-retail-support-agent-design.md`

## Global Constraints

- JDK 17 toolchain (`JavaLanguageVersion.of(17)`), matching every other project in this repo.
- ADK dependency versions pinned exactly: `google-adk` 1.4.0, `google-adk-langchain4j` 1.4.0, `langchain4j-bom` 1.12.2 — these are the versions already verified to compile and run against Groq in `java/google-adk-gradle`.
- Model access goes through `GroqLlmFactory` only — no agent constructs an `OpenAiChatModel` directly. Reads `GROQ_API_KEY` (required) and `GROQ_MODEL` (optional, default `openai/gpt-oss-120b`) from the environment.
- `.env` holds real secrets and is git-ignored; `.env.example` is the committed template. Never hardcode an API key in source.
- Every tool function returns `Map<String, Object>` with a `"status"` key of `"success"` or `"error"` — the convention already established in `p0_basic_agent`/`p6_mcp_tools`. No tool throws an unchecked exception back to the LLM flow.
- No comments explaining *what* code does — only a comment where a *why* is genuinely non-obvious (e.g. the streaming-model-required note already in `GroqLlmFactory`).
- This plan does **not** implement guardrail callbacks, human-in-the-loop confirmation, `escalate_to_human`, or the eval harness — those are explicitly deferred to a follow-up plan per the spec's own scoping.

---

### Task 1: Project scaffold + shared infra + OrderService (TDD)

**Files:**
- Create: `java/retail-support-agent/settings.gradle`
- Create: `java/retail-support-agent/build.gradle`
- Create: `java/retail-support-agent/.gitignore`
- Create: `java/retail-support-agent/.env.example`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/common/GroqLlmFactory.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/common/RunnerSupport.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/backend/OrderItem.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/backend/Order.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/backend/OrderService.java`
- Test: `java/retail-support-agent/src/test/java/com/example/retail/backend/OrderServiceTest.java`

**Interfaces:**
- Produces: `com.example.retail.backend.OrderItem(String itemId, String name, int quantity)` (record)
- Produces: `com.example.retail.backend.Order(String orderId, String customerId, String status, String eta, List<OrderItem> items)` (record)
- Produces: `com.example.retail.backend.OrderService` — `Optional<Order> getOrder(String orderId)`, `List<Order> getOrdersForCustomer(String customerId)`
- Produces: `com.example.retail.common.GroqLlmFactory.create()` → `LangChain4j`, used by every agent task below
- Produces: `com.example.retail.common.RunnerSupport.runScript(BaseAgent agent, String appName, String... turns)` — runs a fixed script of user turns and prints the transcript, used by every demo main below

- [ ] **Step 1: Create the Gradle project scaffold**

`java/retail-support-agent/settings.gradle`:
```groovy
rootProject.name = 'retail-support-agent'
```

`java/retail-support-agent/build.gradle`:
```groovy
plugins {
    id 'java'
}

group = 'com.example.retail'
version = '1.0.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

ext {
    adkVersion = '1.4.0'
    langchain4jVersion = '1.12.2'
    junitVersion = '5.11.3'
}

dependencies {
    implementation platform("dev.langchain4j:langchain4j-bom:${langchain4jVersion}")

    implementation "com.google.adk:google-adk:${adkVersion}"
    implementation "com.google.adk:google-adk-langchain4j:${adkVersion}"
    implementation "dev.langchain4j:langchain4j-open-ai"

    runtimeOnly 'org.slf4j:slf4j-simple:2.0.16'

    testImplementation platform("org.junit:junit-bom:${junitVersion}")
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}

def loadDotEnv() {
    def envFile = file("${projectDir}/.env")
    def vars = [:]
    if (envFile.exists()) {
        envFile.eachLine { line ->
            line = line.trim()
            if (line && !line.startsWith('#') && line.contains('=')) {
                def idx = line.indexOf('=')
                vars[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
        }
    }
    return vars
}

def demoTask = { taskName, taskDescription, mainClassName ->
    tasks.register(taskName, JavaExec) {
        group = 'retail-demos'
        description = taskDescription
        classpath = sourceSets.main.runtimeClasspath
        mainClass = mainClassName
        environment loadDotEnv()
        standardInput = System.in
    }
}

demoTask('runOrderDemo', 'Order Support agent smoke test', 'com.example.retail.agents.OrderSupportAgentDemo')
demoTask('runReturnsDemo', 'Returns agent smoke test', 'com.example.retail.agents.ReturnsAgentDemo')
demoTask('runCatalogDemo', 'Product Discovery agent smoke test', 'com.example.retail.agents.ProductDiscoveryAgentDemo')
demoTask('runFaqDemo', 'FAQ/Policy agent smoke test (needs policy_mcp_server.py running)', 'com.example.retail.agents.FaqPolicyAgentDemo')
demoTask('runOrchestrator', 'Full multi-turn orchestrator demo (needs policy_mcp_server.py running)', 'com.example.retail.RetailSupportDemo')
```

`java/retail-support-agent/.gitignore`:
```
.gradle/
build/
.env
*.iml
.idea/
out/
```

`java/retail-support-agent/.env.example`:
```
# Copy this file to .env (in this same directory) and fill in your key.
# .env is git-ignored - never commit real keys.

GROQ_API_KEY=your-groq-api-key-here
GROQ_MODEL=openai/gpt-oss-120b
```

- [ ] **Step 2: Add the shared model factory and runner helper (copied verbatim from `java/google-adk-gradle`, repackaged)**

`java/retail-support-agent/src/main/java/com/example/retail/common/GroqLlmFactory.java`:
```java
package com.example.retail.common;

import com.google.adk.models.langchain4j.LangChain4j;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.time.Duration;

public final class GroqLlmFactory {

    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String DEFAULT_MODEL = "openai/gpt-oss-120b";

    private GroqLlmFactory() {}

    public static LangChain4j create() {
        return create(System.getenv().getOrDefault("GROQ_MODEL", DEFAULT_MODEL));
    }

    public static LangChain4j create(String modelName) {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GROQ_API_KEY is not set. Copy .env.example to .env in java/retail-support-agent"
                            + " and fill in your key.");
        }

        OpenAiChatModel chatModel =
                OpenAiChatModel.builder()
                        .baseUrl(GROQ_BASE_URL)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(60))
                        .build();

        // Streaming needs a StreamingChatModel too - the adapter throws otherwise.
        OpenAiStreamingChatModel streamingChatModel =
                OpenAiStreamingChatModel.builder()
                        .baseUrl(GROQ_BASE_URL)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(60))
                        .build();

        return LangChain4j.builder()
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .modelName(modelName)
                .build();
    }
}
```

`java/retail-support-agent/src/main/java/com/example/retail/common/RunnerSupport.java`:
```java
package com.example.retail.common;

import com.google.adk.agents.BaseAgent;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

public final class RunnerSupport {

    private static final String USER_ID = "demo-user";

    private RunnerSupport() {}

    public static void runScript(BaseAgent agent, String appName, String... turns) {
        InMemoryRunner runner = new InMemoryRunner(agent);
        Session session = runner.sessionService().createSession(appName, USER_ID).blockingGet();

        for (String turn : turns) {
            System.out.println("\nYou > " + turn);
            Content userMsg = Content.fromParts(Part.fromText(turn));
            Flowable<Event> events = runner.runAsync(USER_ID, session.id(), userMsg);
            System.out.print("Agent > ");
            events.blockingForEach(event -> System.out.println(event.stringifyContent()));
        }
    }
}
```

- [ ] **Step 3: Write the failing test for OrderService**

`java/retail-support-agent/src/test/java/com/example/retail/backend/OrderServiceTest.java`:
```java
package com.example.retail.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private final OrderService service = new OrderService();

    @Test
    void returnsOrderWhenIdExists() {
        Optional<Order> order = service.getOrder("ORD-1001");

        assertTrue(order.isPresent());
        assertEquals("Shipped", order.get().status());
    }

    @Test
    void returnsEmptyWhenIdDoesNotExist() {
        assertTrue(service.getOrder("ORD-9999").isEmpty());
    }

    @Test
    void returnsAllOrdersForCustomer() {
        List<Order> orders = service.getOrdersForCustomer("CUST-1");

        assertEquals(2, orders.size());
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `cd java/retail-support-agent && gradle test --tests "com.example.retail.backend.OrderServiceTest"`
Expected: compilation FAILS — `Order`, `OrderItem`, and `OrderService` don't exist yet.

- [ ] **Step 5: Implement OrderItem, Order, and OrderService**

`java/retail-support-agent/src/main/java/com/example/retail/backend/OrderItem.java`:
```java
package com.example.retail.backend;

public record OrderItem(String itemId, String name, int quantity) {}
```

`java/retail-support-agent/src/main/java/com/example/retail/backend/Order.java`:
```java
package com.example.retail.backend;

import java.util.List;

public record Order(
        String orderId, String customerId, String status, String eta, List<OrderItem> items) {}
```

`java/retail-support-agent/src/main/java/com/example/retail/backend/OrderService.java`:
```java
package com.example.retail.backend;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OrderService {

    private final Map<String, Order> orders;

    public OrderService() {
        this.orders =
                Map.of(
                        "ORD-1001",
                        new Order(
                                "ORD-1001",
                                "CUST-1",
                                "Shipped",
                                "2026-09-08",
                                List.of(new OrderItem("ITEM-1", "Garden Hose 50ft", 1))),
                        "ORD-1002",
                        new Order(
                                "ORD-1002",
                                "CUST-1",
                                "Delivered",
                                "2026-08-30",
                                List.of(new OrderItem("ITEM-2", "Pruning Shears", 2))));
    }

    public Optional<Order> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public List<Order> getOrdersForCustomer(String customerId) {
        return orders.values().stream()
                .filter(order -> order.customerId().equals(customerId))
                .toList();
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `gradle test --tests "com.example.retail.backend.OrderServiceTest"`
Expected: PASS, 3 tests green.

- [ ] **Step 7: Commit**

```bash
git add java/retail-support-agent
git commit -m "feat: scaffold retail-support-agent project with OrderService"
```

---

### Task 2: ReturnService (TDD)

**Files:**
- Create: `java/retail-support-agent/src/main/java/com/example/retail/backend/ReturnRequest.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/backend/ReturnService.java`
- Test: `java/retail-support-agent/src/test/java/com/example/retail/backend/ReturnServiceTest.java`

**Interfaces:**
- Consumes: `com.example.retail.backend.OrderService` (Task 1) — `getOrder(String)`
- Produces: `com.example.retail.backend.ReturnRequest(String returnId, String orderId, String itemId, String reason, String status, double refundAmount)` (record)
- Produces: `com.example.retail.backend.ReturnService(OrderService orderService)` — `boolean isEligible(String orderId, String itemId)`, `ReturnRequest initiateReturn(String orderId, String itemId, String reason)` (throws `IllegalStateException` if not eligible), `Optional<ReturnRequest> getReturn(String returnId)`

- [ ] **Step 1: Write the failing tests**

`java/retail-support-agent/src/test/java/com/example/retail/backend/ReturnServiceTest.java`:
```java
package com.example.retail.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReturnServiceTest {

    private final OrderService orderService = new OrderService();
    private final ReturnService returnService = new ReturnService(orderService);

    @Test
    void deliveredOrderItemIsEligible() {
        assertTrue(returnService.isEligible("ORD-1002", "ITEM-2"));
    }

    @Test
    void shippedOrderItemIsNotEligible() {
        assertFalse(returnService.isEligible("ORD-1001", "ITEM-1"));
    }

    @Test
    void initiateReturnCreatesPendingRequestForEligibleItem() {
        ReturnRequest request = returnService.initiateReturn("ORD-1002", "ITEM-2", "Wrong size");

        assertEquals("Pending", request.status());
        assertEquals("ORD-1002", request.orderId());
    }

    @Test
    void initiateReturnThrowsForIneligibleItem() {
        assertThrows(
                IllegalStateException.class,
                () -> returnService.initiateReturn("ORD-1001", "ITEM-1", "Changed mind"));
    }

    @Test
    void getReturnLooksUpByCreatedId() {
        ReturnRequest created = returnService.initiateReturn("ORD-1002", "ITEM-2", "Wrong size");

        assertEquals(created, returnService.getReturn(created.returnId()).orElseThrow());
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `gradle test --tests "com.example.retail.backend.ReturnServiceTest"`
Expected: compilation FAILS — `ReturnRequest` and `ReturnService` don't exist yet.

- [ ] **Step 3: Implement ReturnRequest and ReturnService**

`java/retail-support-agent/src/main/java/com/example/retail/backend/ReturnRequest.java`:
```java
package com.example.retail.backend;

public record ReturnRequest(
        String returnId, String orderId, String itemId, String reason, String status, double refundAmount) {}
```

`java/retail-support-agent/src/main/java/com/example/retail/backend/ReturnService.java`:
```java
package com.example.retail.backend;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ReturnService {

    private final OrderService orderService;
    private final Map<String, ReturnRequest> returns = new HashMap<>();
    private int nextReturnId = 1;

    public ReturnService(OrderService orderService) {
        this.orderService = orderService;
    }

    public boolean isEligible(String orderId, String itemId) {
        return orderService
                .getOrder(orderId)
                .map(
                        order ->
                                "Delivered".equals(order.status())
                                        && order.items().stream()
                                                .anyMatch(item -> item.itemId().equals(itemId)))
                .orElse(false);
    }

    public ReturnRequest initiateReturn(String orderId, String itemId, String reason) {
        if (!isEligible(orderId, itemId)) {
            throw new IllegalStateException(
                    "Order " + orderId + " item " + itemId + " is not eligible for return");
        }
        String returnId = "RET-" + nextReturnId++;
        ReturnRequest request = new ReturnRequest(returnId, orderId, itemId, reason, "Pending", 0.0);
        returns.put(returnId, request);
        return request;
    }

    public Optional<ReturnRequest> getReturn(String returnId) {
        return Optional.ofNullable(returns.get(returnId));
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `gradle test --tests "com.example.retail.backend.ReturnServiceTest"`
Expected: PASS, 5 tests green.

- [ ] **Step 5: Commit**

```bash
git add java/retail-support-agent/src/main/java/com/example/retail/backend/ReturnRequest.java java/retail-support-agent/src/main/java/com/example/retail/backend/ReturnService.java java/retail-support-agent/src/test/java/com/example/retail/backend/ReturnServiceTest.java
git commit -m "feat: add ReturnService with eligibility rules"
```

---

### Task 3: CatalogService (TDD)

**Files:**
- Create: `java/retail-support-agent/src/main/java/com/example/retail/backend/Product.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/backend/CatalogService.java`
- Test: `java/retail-support-agent/src/test/java/com/example/retail/backend/CatalogServiceTest.java`

**Interfaces:**
- Produces: `com.example.retail.backend.Product(String id, String name, double price, String category)` (record)
- Produces: `com.example.retail.backend.CatalogService` — `List<Product> search(String query)`, `List<Product> recommend(String category)`

- [ ] **Step 1: Write the failing tests**

`java/retail-support-agent/src/test/java/com/example/retail/backend/CatalogServiceTest.java`:
```java
package com.example.retail.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogServiceTest {

    private final CatalogService service = new CatalogService();

    @Test
    void searchMatchesByNameSubstring() {
        List<Product> results = service.search("hose");

        assertEquals(1, results.size());
        assertEquals("P-1", results.get(0).id());
    }

    @Test
    void searchMatchesByCategory() {
        List<Product> results = service.search("garden");

        assertEquals(3, results.size());
    }

    @Test
    void searchReturnsEmptyForNoMatch() {
        assertTrue(service.search("xyz-nonexistent").isEmpty());
    }

    @Test
    void recommendFiltersByCategory() {
        List<Product> results = service.recommend("tools");

        assertEquals(1, results.size());
        assertEquals("P-4", results.get(0).id());
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `gradle test --tests "com.example.retail.backend.CatalogServiceTest"`
Expected: compilation FAILS — `Product` and `CatalogService` don't exist yet.

- [ ] **Step 3: Implement Product and CatalogService**

`java/retail-support-agent/src/main/java/com/example/retail/backend/Product.java`:
```java
package com.example.retail.backend;

public record Product(String id, String name, double price, String category) {}
```

`java/retail-support-agent/src/main/java/com/example/retail/backend/CatalogService.java`:
```java
package com.example.retail.backend;

import java.util.List;

public final class CatalogService {

    private final List<Product> products =
            List.of(
                    new Product("P-1", "Garden Hose 50ft", 29.99, "garden"),
                    new Product("P-2", "Pruning Shears", 14.99, "garden"),
                    new Product("P-3", "Patio Chair", 89.99, "outdoor"),
                    new Product("P-4", "Cordless Drill", 59.99, "tools"),
                    new Product("P-5", "Watering Can", 12.99, "garden"));

    public List<Product> search(String query) {
        String needle = query.toLowerCase();
        return products.stream()
                .filter(
                        p ->
                                p.name().toLowerCase().contains(needle)
                                        || p.category().toLowerCase().contains(needle))
                .toList();
    }

    public List<Product> recommend(String category) {
        return products.stream().filter(p -> p.category().equalsIgnoreCase(category)).toList();
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `gradle test --tests "com.example.retail.backend.CatalogServiceTest"`
Expected: PASS, 4 tests green.

- [ ] **Step 5: Commit**

```bash
git add java/retail-support-agent/src/main/java/com/example/retail/backend/Product.java java/retail-support-agent/src/main/java/com/example/retail/backend/CatalogService.java java/retail-support-agent/src/test/java/com/example/retail/backend/CatalogServiceTest.java
git commit -m "feat: add CatalogService with search and recommendations"
```

---

### Task 4: Order tools + OrderSupportAgent + live smoke demo

**Files:**
- Create: `java/retail-support-agent/src/main/java/com/example/retail/tools/OrderTools.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/OrderSupportAgent.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/OrderSupportAgentDemo.java`
- Test: `java/retail-support-agent/src/test/java/com/example/retail/tools/OrderToolsTest.java`

**Interfaces:**
- Consumes: `com.example.retail.backend.OrderService` (Task 1), `com.example.retail.common.GroqLlmFactory.create()` and `RunnerSupport.runScript(...)` (Task 1)
- Produces: `com.example.retail.tools.OrderTools.getOrderStatus(String orderId)` → `Map<String, Object>`, `getShipmentTracking(String orderId)` → `Map<String, Object>`
- Produces: `com.example.retail.agents.OrderSupportAgent.build()` → `LlmAgent`, named `"order_support_agent"` — consumed by Task 8's orchestrator

- [ ] **Step 1: Write the failing test for OrderTools**

`java/retail-support-agent/src/test/java/com/example/retail/tools/OrderToolsTest.java`:
```java
package com.example.retail.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderToolsTest {

    @Test
    void getOrderStatusReturnsSuccessForKnownOrder() {
        Map<String, Object> result = OrderTools.getOrderStatus("ORD-1001");

        assertEquals("success", result.get("status"));
        assertEquals("Shipped", result.get("orderStatus"));
    }

    @Test
    void getOrderStatusReturnsErrorForUnknownOrder() {
        Map<String, Object> result = OrderTools.getOrderStatus("ORD-0000");

        assertEquals("error", result.get("status"));
    }

    @Test
    void getShipmentTrackingReturnsSuccessForKnownOrder() {
        Map<String, Object> result = OrderTools.getShipmentTracking("ORD-1002");

        assertEquals("success", result.get("status"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `gradle test --tests "com.example.retail.tools.OrderToolsTest"`
Expected: compilation FAILS — `OrderTools` doesn't exist yet.

- [ ] **Step 3: Implement OrderTools**

`java/retail-support-agent/src/main/java/com/example/retail/tools/OrderTools.java`:
```java
package com.example.retail.tools;

import com.example.retail.backend.OrderService;
import com.google.adk.tools.Annotations.Schema;
import java.util.Map;

public final class OrderTools {

    private static final OrderService SERVICE = new OrderService();

    private OrderTools() {}

    public static Map<String, Object> getOrderStatus(
            @Schema(name = "orderId", description = "The order ID to look up, e.g. ORD-1001")
                    String orderId) {
        return SERVICE
                .getOrder(orderId)
                .<Map<String, Object>>map(
                        order ->
                                Map.of(
                                        "status", "success",
                                        "orderId", order.orderId(),
                                        "orderStatus", order.status(),
                                        "eta", order.eta()))
                .orElse(Map.of("status", "error", "report", "No order found with ID " + orderId));
    }

    public static Map<String, Object> getShipmentTracking(
            @Schema(name = "orderId", description = "The order ID to track, e.g. ORD-1001")
                    String orderId) {
        return SERVICE
                .getOrder(orderId)
                .<Map<String, Object>>map(
                        order ->
                                Map.of(
                                        "status", "success",
                                        "orderId", order.orderId(),
                                        "report",
                                                "Order "
                                                        + order.orderId()
                                                        + " is "
                                                        + order.status()
                                                        + ", expected by "
                                                        + order.eta()
                                                        + "."))
                .orElse(Map.of("status", "error", "report", "No order found with ID " + orderId));
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `gradle test --tests "com.example.retail.tools.OrderToolsTest"`
Expected: PASS, 3 tests green.

- [ ] **Step 5: Implement OrderSupportAgent and its smoke-test demo**

`java/retail-support-agent/src/main/java/com/example/retail/agents/OrderSupportAgent.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.example.retail.tools.OrderTools;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionTool;

public final class OrderSupportAgent {

    private OrderSupportAgent() {}

    public static LlmAgent build() {
        return LlmAgent.builder()
                .name("order_support_agent")
                .model(GroqLlmFactory.create())
                .description(
                        "Answers questions about order status, delivery ETA, and shipment tracking.")
                .instruction(
                        "You help customers check their order status and tracking. Use the tools"
                                + " provided; never guess an order's status.")
                .tools(
                        FunctionTool.create(OrderTools.class, "getOrderStatus"),
                        FunctionTool.create(OrderTools.class, "getShipmentTracking"))
                .build();
    }
}
```

`java/retail-support-agent/src/main/java/com/example/retail/agents/OrderSupportAgentDemo.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class OrderSupportAgentDemo {

    public static void main(String[] args) throws Exception {
        LlmAgent agent = OrderSupportAgent.build();
        RunnerSupport.runScript(
                agent,
                "order_support_agent",
                "What's the status of order ORD-1001?",
                "Track order ORD-1002 for me.");
    }
}
```

- [ ] **Step 6: Run the live smoke demo and confirm real tool calls happen**

Run: `gradle runOrderDemo` (needs `.env` with `GROQ_API_KEY` set — copy `.env.example` if not done yet)
Expected: transcript shows a `getOrderStatus` function call for ORD-1001 reporting "Shipped", and a `getShipmentTracking` call for ORD-1002 reporting "Delivered".

- [ ] **Step 7: Commit**

```bash
git add java/retail-support-agent/src/main/java/com/example/retail/tools/OrderTools.java java/retail-support-agent/src/main/java/com/example/retail/agents/OrderSupportAgent.java java/retail-support-agent/src/main/java/com/example/retail/agents/OrderSupportAgentDemo.java java/retail-support-agent/src/test/java/com/example/retail/tools/OrderToolsTest.java
git commit -m "feat: add OrderSupportAgent with order status/tracking tools"
```

---

### Task 5: Return tools + ReturnsAgent + live smoke demo

**Files:**
- Create: `java/retail-support-agent/src/main/java/com/example/retail/tools/ReturnTools.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/ReturnsAgent.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/ReturnsAgentDemo.java`
- Test: `java/retail-support-agent/src/test/java/com/example/retail/tools/ReturnToolsTest.java`

**Interfaces:**
- Consumes: `com.example.retail.backend.OrderService`, `com.example.retail.backend.ReturnService`, `com.example.retail.backend.ReturnRequest` (Task 1, Task 2)
- Produces: `com.example.retail.tools.ReturnTools.checkReturnEligibility(String orderId, String itemId)`, `.initiateReturn(String orderId, String itemId, String reason)`, `.getRefundStatus(String returnId)` — all → `Map<String, Object>`
- Produces: `com.example.retail.agents.ReturnsAgent.build()` → `LlmAgent`, named `"returns_agent"` — consumed by Task 8's orchestrator

- [ ] **Step 1: Write the failing tests for ReturnTools**

`java/retail-support-agent/src/test/java/com/example/retail/tools/ReturnToolsTest.java`:
```java
package com.example.retail.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ReturnToolsTest {

    @Test
    void checkReturnEligibilityReportsTrueForDeliveredItem() {
        Map<String, Object> result = ReturnTools.checkReturnEligibility("ORD-1002", "ITEM-2");

        assertEquals(true, result.get("eligible"));
    }

    @Test
    void initiateReturnFailsForIneligibleItem() {
        Map<String, Object> result = ReturnTools.initiateReturn("ORD-1001", "ITEM-1", "Changed mind");

        assertEquals("error", result.get("status"));
    }

    @Test
    void initiateReturnSucceedsForEligibleItem() {
        Map<String, Object> result = ReturnTools.initiateReturn("ORD-1002", "ITEM-2", "Wrong size");

        assertEquals("success", result.get("status"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `gradle test --tests "com.example.retail.tools.ReturnToolsTest"`
Expected: compilation FAILS — `ReturnTools` doesn't exist yet.

- [ ] **Step 3: Implement ReturnTools**

`java/retail-support-agent/src/main/java/com/example/retail/tools/ReturnTools.java`:
```java
package com.example.retail.tools;

import com.example.retail.backend.OrderService;
import com.example.retail.backend.ReturnRequest;
import com.example.retail.backend.ReturnService;
import com.google.adk.tools.Annotations.Schema;
import java.util.Map;

public final class ReturnTools {

    private static final OrderService ORDER_SERVICE = new OrderService();
    private static final ReturnService RETURN_SERVICE = new ReturnService(ORDER_SERVICE);

    private ReturnTools() {}

    public static Map<String, Object> checkReturnEligibility(
            @Schema(name = "orderId", description = "The order ID") String orderId,
            @Schema(name = "itemId", description = "The item ID within that order") String itemId) {
        boolean eligible = RETURN_SERVICE.isEligible(orderId, itemId);
        return Map.of(
                "status",
                "success",
                "eligible",
                eligible,
                "report",
                eligible
                        ? "Item " + itemId + " on order " + orderId + " is eligible for return."
                        : "Item " + itemId + " on order " + orderId + " is not eligible for return.");
    }

    public static Map<String, Object> initiateReturn(
            @Schema(name = "orderId", description = "The order ID") String orderId,
            @Schema(name = "itemId", description = "The item ID within that order") String itemId,
            @Schema(name = "reason", description = "Why the customer is returning the item")
                    String reason) {
        try {
            ReturnRequest request = RETURN_SERVICE.initiateReturn(orderId, itemId, reason);
            return Map.of(
                    "status",
                    "success",
                    "returnId",
                    request.returnId(),
                    "report",
                    "Return " + request.returnId() + " created for order " + orderId + ".");
        } catch (IllegalStateException e) {
            return Map.of("status", "error", "report", e.getMessage());
        }
    }

    public static Map<String, Object> getRefundStatus(
            @Schema(name = "returnId", description = "The return ID to check") String returnId) {
        return RETURN_SERVICE
                .getReturn(returnId)
                .<Map<String, Object>>map(
                        r ->
                                Map.of(
                                        "status", "success",
                                        "returnId", r.returnId(),
                                        "refundStatus", r.status()))
                .orElse(Map.of("status", "error", "report", "No return found with ID " + returnId));
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `gradle test --tests "com.example.retail.tools.ReturnToolsTest"`
Expected: PASS, 3 tests green.

- [ ] **Step 5: Implement ReturnsAgent and its smoke-test demo**

`java/retail-support-agent/src/main/java/com/example/retail/agents/ReturnsAgent.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.example.retail.tools.ReturnTools;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionTool;

public final class ReturnsAgent {

    private ReturnsAgent() {}

    public static LlmAgent build() {
        return LlmAgent.builder()
                .name("returns_agent")
                .model(GroqLlmFactory.create())
                .description("Handles return eligibility checks, initiating returns, and refund status.")
                .instruction(
                        "Always call checkReturnEligibility before initiateReturn. Never tell a"
                                + " customer a return is approved unless the tool confirms it.")
                .tools(
                        FunctionTool.create(ReturnTools.class, "checkReturnEligibility"),
                        FunctionTool.create(ReturnTools.class, "initiateReturn"),
                        FunctionTool.create(ReturnTools.class, "getRefundStatus"))
                .build();
    }
}
```

`java/retail-support-agent/src/main/java/com/example/retail/agents/ReturnsAgentDemo.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class ReturnsAgentDemo {

    public static void main(String[] args) throws Exception {
        LlmAgent agent = ReturnsAgent.build();
        RunnerSupport.runScript(
                agent,
                "returns_agent",
                "I want to return item ITEM-2 from order ORD-1002, it's the wrong size.",
                "Can I return item ITEM-1 from order ORD-1001 too? I just don't like it.");
    }
}
```

- [ ] **Step 6: Run the live smoke demo and confirm the eligibility gate works**

Run: `gradle runReturnsDemo`
Expected: first turn eligibility check passes and a return is created; second turn's eligibility check reports ineligible (order not yet delivered) and the agent declines without calling `initiateReturn`.

- [ ] **Step 7: Commit**

```bash
git add java/retail-support-agent/src/main/java/com/example/retail/tools/ReturnTools.java java/retail-support-agent/src/main/java/com/example/retail/agents/ReturnsAgent.java java/retail-support-agent/src/main/java/com/example/retail/agents/ReturnsAgentDemo.java java/retail-support-agent/src/test/java/com/example/retail/tools/ReturnToolsTest.java
git commit -m "feat: add ReturnsAgent with eligibility-gated return tools"
```

---

### Task 6: Catalog tools + ProductDiscoveryAgent + live smoke demo

**Files:**
- Create: `java/retail-support-agent/src/main/java/com/example/retail/tools/CatalogTools.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/ProductDiscoveryAgent.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/ProductDiscoveryAgentDemo.java`
- Test: `java/retail-support-agent/src/test/java/com/example/retail/tools/CatalogToolsTest.java`

**Interfaces:**
- Consumes: `com.example.retail.backend.CatalogService`, `com.example.retail.backend.Product` (Task 3)
- Produces: `com.example.retail.tools.CatalogTools.searchCatalog(String query)`, `.getRecommendations(String category)` → `Map<String, Object>`
- Produces: `com.example.retail.agents.ProductDiscoveryAgent.build()` → `LlmAgent`, named `"product_discovery_agent"` — consumed by Task 8's orchestrator

- [ ] **Step 1: Write the failing tests for CatalogTools**

`java/retail-support-agent/src/test/java/com/example/retail/tools/CatalogToolsTest.java`:
```java
package com.example.retail.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogToolsTest {

    @Test
    void searchCatalogReturnsSuccessForMatch() {
        Map<String, Object> result = CatalogTools.searchCatalog("garden");

        assertEquals("success", result.get("status"));
    }

    @Test
    void searchCatalogReturnsErrorForNoMatch() {
        Map<String, Object> result = CatalogTools.searchCatalog("xyz-nonexistent");

        assertEquals("error", result.get("status"));
    }

    @Test
    void getRecommendationsReturnsSuccessForKnownCategory() {
        Map<String, Object> result = CatalogTools.getRecommendations("tools");

        assertEquals("success", result.get("status"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `gradle test --tests "com.example.retail.tools.CatalogToolsTest"`
Expected: compilation FAILS — `CatalogTools` doesn't exist yet.

- [ ] **Step 3: Implement CatalogTools**

`java/retail-support-agent/src/main/java/com/example/retail/tools/CatalogTools.java`:
```java
package com.example.retail.tools;

import com.example.retail.backend.CatalogService;
import com.example.retail.backend.Product;
import com.google.adk.tools.Annotations.Schema;
import java.util.List;
import java.util.Map;

public final class CatalogTools {

    private static final CatalogService SERVICE = new CatalogService();

    private CatalogTools() {}

    public static Map<String, Object> searchCatalog(
            @Schema(name = "query", description = "Product name or category keyword to search for")
                    String query) {
        List<Product> results = SERVICE.search(query);
        if (results.isEmpty()) {
            return Map.of("status", "error", "report", "No products matched '" + query + "'.");
        }
        return Map.of("status", "success", "items", results.stream().map(Product::name).toList());
    }

    public static Map<String, Object> getRecommendations(
            @Schema(
                            name = "category",
                            description = "Product category to recommend from, e.g. garden")
                    String category) {
        List<Product> results = SERVICE.recommend(category);
        if (results.isEmpty()) {
            return Map.of("status", "error", "report", "No recommendations for '" + category + "'.");
        }
        return Map.of("status", "success", "items", results.stream().map(Product::name).toList());
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `gradle test --tests "com.example.retail.tools.CatalogToolsTest"`
Expected: PASS, 3 tests green.

- [ ] **Step 5: Implement ProductDiscoveryAgent and its smoke-test demo**

`java/retail-support-agent/src/main/java/com/example/retail/agents/ProductDiscoveryAgent.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.example.retail.tools.CatalogTools;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionTool;

public final class ProductDiscoveryAgent {

    private ProductDiscoveryAgent() {}

    public static LlmAgent build() {
        return LlmAgent.builder()
                .name("product_discovery_agent")
                .model(GroqLlmFactory.create())
                .description("Helps customers search the catalog and get product recommendations.")
                .instruction(
                        "Use the tools to search the catalog or recommend products. Never invent"
                                + " a product that the tools didn't return.")
                .tools(
                        FunctionTool.create(CatalogTools.class, "searchCatalog"),
                        FunctionTool.create(CatalogTools.class, "getRecommendations"))
                .build();
    }
}
```

`java/retail-support-agent/src/main/java/com/example/retail/agents/ProductDiscoveryAgentDemo.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class ProductDiscoveryAgentDemo {

    public static void main(String[] args) throws Exception {
        LlmAgent agent = ProductDiscoveryAgent.build();
        RunnerSupport.runScript(
                agent,
                "product_discovery_agent",
                "Do you have any garden products in stock?",
                "What tools would you recommend?");
    }
}
```

- [ ] **Step 6: Run the live smoke demo**

Run: `gradle runCatalogDemo`
Expected: first turn lists the three garden products, second turn recommends the Cordless Drill.

- [ ] **Step 7: Commit**

```bash
git add java/retail-support-agent/src/main/java/com/example/retail/tools/CatalogTools.java java/retail-support-agent/src/main/java/com/example/retail/agents/ProductDiscoveryAgent.java java/retail-support-agent/src/main/java/com/example/retail/agents/ProductDiscoveryAgentDemo.java java/retail-support-agent/src/test/java/com/example/retail/tools/CatalogToolsTest.java
git commit -m "feat: add ProductDiscoveryAgent with catalog search and recommendations"
```

---

### Task 7: Policy MCP server (Python) + FaqPolicyAgent + live smoke demo

**Files:**
- Create: `python/project-4-retail-policy-mcp/policy_mcp_server.py`
- Create: `python/project-4-retail-policy-mcp/requirements.txt`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/FaqPolicyAgent.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/FaqPolicyAgentDemo.java`

**Interfaces:**
- Produces (Python, over MCP streamable HTTP at `http://127.0.0.1:9001/mcp`): `search_policy_docs(query: str) -> dict` with `{"status": "success", "matches": {...}}` or `{"status": "error", "report": "..."}`
- Produces: `com.example.retail.agents.FaqPolicyAgent.build()` → `LlmAgent`, named `"faq_policy_agent"` — consumed by Task 8's orchestrator

No JUnit test in this task — the MCP server is Python and the agent wiring is the same
`McpToolset`/`StreamableHttpServerParameters` mechanism already proven live in `p6_mcp_tools`; the
live smoke demo in Step 3 is the verification.

- [ ] **Step 1: Write the policy MCP server**

`python/project-4-retail-policy-mcp/requirements.txt`:
```
fastmcp
```

`python/project-4-retail-policy-mcp/policy_mcp_server.py`:
```python
from fastmcp import FastMCP

mcp = FastMCP("PolicyDocs")

POLICIES = {
    "shipping": "Standard shipping takes 3-5 business days. Express shipping takes 1-2 business days.",
    "returns": "Items can be returned within 30 days of delivery for a full refund, in original condition.",
    "warranty": "All power tools carry a 1-year manufacturer warranty covering defects.",
    "hours": "Stores are open Monday-Saturday 9am-8pm, and Sunday 10am-6pm.",
}


@mcp.tool
def search_policy_docs(query: str) -> dict:
    """Search store policy documents (shipping, returns, warranty, hours) for a keyword."""
    query_lower = query.lower()
    matches = {
        topic: text
        for topic, text in POLICIES.items()
        if query_lower in topic or query_lower in text.lower()
    }
    if not matches:
        return {"status": "error", "report": f"No policy information found for '{query}'."}
    return {"status": "success", "matches": matches}


if __name__ == "__main__":
    mcp.run(
        transport="http",
        host="127.0.0.1",
        port=9001,
        path="/mcp",
    )
```

- [ ] **Step 2: Implement FaqPolicyAgent and its smoke-test demo**

`java/retail-support-agent/src/main/java/com/example/retail/agents/FaqPolicyAgent.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.google.adk.JsonBaseModel;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StreamableHttpServerParameters;
import java.time.Duration;

public final class FaqPolicyAgent {

    private static final String MCP_SERVER_URL = "http://127.0.0.1:9001/mcp";

    private FaqPolicyAgent() {}

    public static LlmAgent build() {
        StreamableHttpServerParameters connectionParams =
                StreamableHttpServerParameters.builder()
                        .url(MCP_SERVER_URL)
                        .timeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofMinutes(5))
                        .build();

        McpToolset mcpToolset = new McpToolset(connectionParams, JsonBaseModel.getMapper());

        return LlmAgent.builder()
                .name("faq_policy_agent")
                .model(GroqLlmFactory.create())
                .description(
                        "Answers questions about shipping, returns, warranty, and store hours policy.")
                .instruction(
                        "Use the search_policy_docs tool to answer policy questions. Only answer"
                                + " from what the tool returns; do not invent policy details.")
                .tools(mcpToolset)
                .build();
    }
}
```

`java/retail-support-agent/src/main/java/com/example/retail/agents/FaqPolicyAgentDemo.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class FaqPolicyAgentDemo {

    private static final String MCP_SERVER_URL = "http://127.0.0.1:9001/mcp";

    public static void main(String[] args) throws Exception {
        try {
            LlmAgent agent = FaqPolicyAgent.build();
            RunnerSupport.runScript(
                    agent, "faq_policy_agent", "What's your return policy?", "What are your store hours?");
        } catch (Exception e) {
            System.err.println(
                    "Could not reach the policy MCP server at "
                            + MCP_SERVER_URL
                            + ". Start it first: cd python/project-4-retail-policy-mcp && pip install -r"
                            + " requirements.txt && python policy_mcp_server.py");
            throw e;
        }
    }
}
```

- [ ] **Step 3: Start the MCP server and run the live smoke demo**

Run (in one terminal): `cd python/project-4-retail-policy-mcp && pip install -r requirements.txt && python policy_mcp_server.py`
Run (in another terminal): `cd java/retail-support-agent && gradle runFaqDemo`
Expected: first turn returns the 30-day return policy text, second turn returns the store hours text, both via a `search_policy_docs` function call visible in the transcript.

- [ ] **Step 4: Stop the MCP server, commit**

```bash
git add python/project-4-retail-policy-mcp java/retail-support-agent/src/main/java/com/example/retail/agents/FaqPolicyAgent.java java/retail-support-agent/src/main/java/com/example/retail/agents/FaqPolicyAgentDemo.java
git commit -m "feat: add FaqPolicyAgent backed by a policy MCP server"
```

---

### Task 8: OrchestratorAgent + full multi-turn integration demo

**Files:**
- Create: `java/retail-support-agent/src/main/java/com/example/retail/agents/OrchestratorAgent.java`
- Create: `java/retail-support-agent/src/main/java/com/example/retail/RetailSupportDemo.java`

**Interfaces:**
- Consumes: `OrderSupportAgent.build()` (Task 4), `ReturnsAgent.build()` (Task 5), `ProductDiscoveryAgent.build()` (Task 6), `FaqPolicyAgent.build()` (Task 7), `GroqLlmFactory.create()` and `RunnerSupport.runScript(...)` (Task 1)
- Produces: `com.example.retail.agents.OrchestratorAgent.build()` → `LlmAgent`, named `"retail_support_orchestrator"`, with the four specialists as `subAgents`

No new JUnit test — this task wires together agents already individually tool-tested; the
multi-turn live demo is the verification that routing and hand-off actually work end-to-end,
the same way `p2_multi_agent` was verified.

- [ ] **Step 1: Implement OrchestratorAgent**

`java/retail-support-agent/src/main/java/com/example/retail/agents/OrchestratorAgent.java`:
```java
package com.example.retail.agents;

import com.example.retail.common.GroqLlmFactory;
import com.google.adk.agents.LlmAgent;
import com.google.common.collect.ImmutableList;

public final class OrchestratorAgent {

    private OrchestratorAgent() {}

    public static LlmAgent build() {
        return LlmAgent.builder()
                .name("retail_support_orchestrator")
                .model(GroqLlmFactory.create())
                .description("Routes retail support conversations to the right specialist.")
                .instruction(
                        "You do not answer questions yourself. Delegate every message to the"
                                + " sub-agent whose description best matches it: order status/tracking"
                                + " to order_support_agent, returns/refunds to returns_agent, product"
                                + " search/recommendations to product_discovery_agent, and policy"
                                + " questions (shipping, returns window, warranty, store hours) to"
                                + " faq_policy_agent.")
                .subAgents(
                        ImmutableList.of(
                                OrderSupportAgent.build(),
                                ReturnsAgent.build(),
                                ProductDiscoveryAgent.build(),
                                FaqPolicyAgent.build()))
                .build();
    }
}
```

- [ ] **Step 2: Implement the full multi-turn integration demo**

`java/retail-support-agent/src/main/java/com/example/retail/RetailSupportDemo.java`:
```java
package com.example.retail;

import com.example.retail.agents.OrchestratorAgent;
import com.example.retail.common.RunnerSupport;
import com.google.adk.agents.LlmAgent;

public final class RetailSupportDemo {

    public static void main(String[] args) throws Exception {
        LlmAgent orchestrator = OrchestratorAgent.build();
        RunnerSupport.runScript(
                orchestrator,
                "retail_support_orchestrator",
                "What's the status of order ORD-1001?",
                "I want to return item ITEM-2 from order ORD-1002, it's the wrong size.",
                "Do you have any garden products in stock?",
                "What's your return policy?");
    }
}
```

- [ ] **Step 3: Start the MCP server (if not already running) and run the full demo**

Run (in one terminal, if not already running): `cd python/project-4-retail-policy-mcp && python policy_mcp_server.py`
Run (in another terminal): `cd java/retail-support-agent && gradle runOrchestrator`
Expected: transcript shows four `transfer_to_agent` calls, one per turn, routing to
`order_support_agent`, `returns_agent`, `product_discovery_agent`, and `faq_policy_agent` in
that order, with each specialist's tool call and correct answer following.

- [ ] **Step 4: Commit**

```bash
git add java/retail-support-agent/src/main/java/com/example/retail/agents/OrchestratorAgent.java java/retail-support-agent/src/main/java/com/example/retail/RetailSupportDemo.java
git commit -m "feat: wire specialist agents into a routing orchestrator"
```

---

### Task 9: README

**Files:**
- Create: `java/retail-support-agent/Readme.md`

- [ ] **Step 1: Write the README**

`java/retail-support-agent/Readme.md`:
```markdown
# retail-support-agent

A retail customer-support agent built on Google ADK Java: one orchestrator routing to four
specialist sub-agents. Implements the design in
`docs/superpowers/specs/2026-09-04-retail-support-agent-design.md` (core flow only — guardrails,
human-in-the-loop confirmation, escalation, and the eval harness are a follow-up).

## Setup

```
cd java/retail-support-agent
cp .env.example .env
# edit .env and set GROQ_API_KEY
```

The FAQ/Policy agent needs its MCP server running first:

```
cd python/project-4-retail-policy-mcp
pip install -r requirements.txt
python policy_mcp_server.py
```

## Agents

| Agent | Handles | Tools |
|---|---|---|
| `order_support_agent` | Order status, shipment tracking | `getOrderStatus`, `getShipmentTracking` |
| `returns_agent` | Return eligibility, initiating returns, refund status | `checkReturnEligibility`, `initiateReturn`, `getRefundStatus` |
| `product_discovery_agent` | Catalog search, recommendations | `searchCatalog`, `getRecommendations` |
| `faq_policy_agent` | Shipping/returns/warranty/hours policy | MCP toolset → `search_policy_docs` |
| `retail_support_orchestrator` | Routes every turn to the right specialist above | `subAgents` only |

## Running

```
gradle runOrderDemo       # order support in isolation
gradle runReturnsDemo     # returns in isolation
gradle runCatalogDemo     # product discovery in isolation
gradle runFaqDemo         # FAQ/policy in isolation (needs the MCP server running)
gradle runOrchestrator    # full multi-turn conversation across all four specialists
gradle test               # backend service + tool unit tests
```

## Backend data

Everything runs against small in-memory services seeded with two orders (`ORD-1001` shipped,
`ORD-1002` delivered) and five products across garden/outdoor/tools categories — see
`src/main/java/com/example/retail/backend/`.
```

- [ ] **Step 2: Commit**

```bash
git add java/retail-support-agent/Readme.md
git commit -m "docs: add retail-support-agent README"
```

---

## Self-Review Notes

- **Spec coverage:** orchestrator + 4 specialists (spec §Agents) → Tasks 4-8. Tools table (spec
  §Tools) → Tasks 4-7, one row per tool. Session state (spec §Session state) and structured
  output (spec §Structured output) are **not** in this plan — see below.
- **Deliberate spec gaps in this plan** (all captured in the Global Constraints deferral note and
  the spec's own "Relationship to the existing playground" table as not-yet-proven items):
  guardrails, human-in-the-loop confirmation, `escalate_to_human`, eval harness. Also deferred:
  the spec's `outputSchema` structured-response pattern and the `context:*` session-state
  continuity pattern (`p3_session_state`) — the core flow here uses one-shot tool calls per
  specialist rather than cross-turn state yet. These three (structured output, session-state
  continuity, hardening) are the natural Plan 2.
- **Type consistency:** verified `Order`/`OrderItem`/`ReturnRequest`/`Product` field names and
  accessor methods are used identically everywhere they're referenced across tasks (e.g.
  `order.status()`, `order.items()`, `item.itemId()`, `request.returnId()`).
- **No placeholders:** every step has complete, concrete code — no TBD/TODO markers.
