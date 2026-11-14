# Technical Architecture: JFLAP 7.1 Headless Bridge

## 1. Problem Statement
**JFLAP** (Java Formal Languages and Automata Package) is the gold standard educational tool for automata theory and formal languages, developed under Prof. Susan H. Rodger at Duke University.

However, standard JFLAP has historically suffered from critical automation limitations:
1. **Coupled Swing GUI**: All actions, simulations, and converters were designed around `JFrame`, `JOptionPane`, and `JPanel`.
2. **Modal Dialog Hangs**: Key simulation classes (such as `automata.pda.PDAStepByStateSimulator` and `file.xml.TransducerFactory`) contain hardcoded calls to `JOptionPane.showInputDialog` or `JOptionPane.showConfirmDialog`. In headless environments (CI/CD, CLI, or AI agent servers), these either throw `HeadlessException` or freeze indefinitely waiting for user clicks.
3. **Absence of Modern APIs**: No CLI flags for simulation or batch grading, no JSON output, and no standard RPC/MCP interface for AI systems.

---

## 2. Solution Design

`jflap-cli` provides a dual-layer architecture:

```text
┌─────────────────────────────────────────────────────────────┐
│                    AI Agents / Users                        │
│   (Claude Desktop, Antigravity, Cursor, Terminal, CI/CD)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
       ┌─────────────────┐           ┌─────────────────┐
       │   CLI Client    │           │   MCP Server    │
       │ (jflap_cli.py)  │           │  (jflap_mcp.py) │
       └────────┬────────┘           └────────┬────────┘
                │                             │
                └──────────────┬──────────────┘
                               │ JSON-RPC / Subprocess
                               ▼
       ┌───────────────────────────────────────────────┐
       │               Java Headless Bridge            │
       │               (JFLAPBridge.java)              │
       │                                               │
       │  • HeadlessPDASimulator (Bypasses Swing GUI)  │
       │  • Bounded BFS Path Explorer (Anti-loop)      │
       │  • Safe XML Transducer Loader                 │
       │  • JSON Serialization Engine                  │
       └───────────────────────┬───────────────────────┘
                               │ Direct Classpath Calls
                               ▼
       ┌───────────────────────────────────────────────┐
       │                  JFLAP7.1.jar                 │
       │  • file.XMLCodec                              │
       │  • automata.fsa.NFAToDFA                      │
       │  • automata.fsa.Minimizer                     │
       │  • automata.graph.FSAEqualityChecker          │
       │  • grammar.parse.CYKParser                    │
       │  • grammar.parse.BruteParser                  │
       └───────────────────────────────────────────────┘
```

---

## 3. Key Technical Innovations

### A. Neutralizing Swing Dialog Traps
In `PDAStepByStateSimulator.java`, the constructor executes:
```java
Object[] possibleValues = {"Final State", "Empty Stack"};
Object selectedValue = JOptionPane.showInputDialog(null, "Accept by", ...);
```
And in `TransducerFactory.java`, opening single-tape Turing machines prompts:
```java
Object selectedValue = JOptionPane.showInputDialog(null, "What type of Turing machine...", ...);
```

**How We Fixed It:**
- **Custom `HeadlessPDASimulator`**: We implemented a lightweight simulator extending `AutomatonSimulator` that defaults to final-state or empty-stack acceptance without prompting Swing dialogs.
- **Preemptive DOM Inspection**: `JFLAPBridge` parses the `.jff` XML DOM first. If `<type>turing</type>` is detected without building blocks, it directly instantiates `new TMTransducer().fromDOM(doc)`, completely bypassing the dialog.

### B. Bounded Breadth-First State Search
Simulating non-deterministic pushdown automata with $\lambda$-transitions or non-terminating Turing machines risks infinite loops and heap exhaustion.

`JFLAPBridge` executes a BFS simulation loop with:
- **Visited State Tracking**: Configuration state hashing via `config.toString()`.
- **Stack Depth Guard**: For PDAs, stack growth is capped at 50 to prevent unbounded left-recursion.
- **Maximum Step Limit**: Execution terminates gracefully after a bounded threshold (default: 10,000 configurations).

### C. Automated Compilation & Zero-Config Setup
Both `jflap_cli.py` and `jflap_mcp.py` implement `ensure_environment()`:
- Automatically detects if `third_party/jflap/JFLAP7.1.jar` exists; if missing, securely downloads it from Duke University.
- Detects if `JFLAPBridge.class` is absent or outdated compared to `JFLAPBridge.java` and invokes `javac` automatically.
- No build step or manual compilation is required after cloning.
