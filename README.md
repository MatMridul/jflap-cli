# JFLAP 7.1 CLI & Model Context Protocol (MCP) Server

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Python 3.8+](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org/downloads/)
[![Java 1.8+](https://img.shields.io/badge/java-8+-orange.svg)](https://www.java.com/)
[![MCP](https://img.shields.io/badge/MCP-JSON--RPC%202.0-green.svg)](https://modelcontextprotocol.io/)

A complete, production-grade command-line interface and **Model Context Protocol (MCP)** server for controlling and automating **JFLAP 7.1** (`JFLAP7.1.jar`).

Standard JFLAP is a legacy Java Swing application without native command-line or headless automation support. This repository bridges directly into JFLAP's internal classes (`file.XMLCodec`, `automata.*`, `grammar.*`) to provide fast, robust, headless execution, simulation, conversion, batch testing, and AI-driven control without popping up Swing dialogs.

---

## Table of Contents
- [Highlights](#highlights)
- [Quick Start](#quick-start)
- [CLI Command Overview](#cli-command-overview)
- [Model Context Protocol (MCP) Integration](#model-context-protocol-mcp-integration)
- [Documentation Index](#documentation-index)
- [Architecture & Quirks Resolved](#architecture--quirks-resolved)
- [License & Acknowledgments](#license--acknowledgments)

---

## Highlights

- ⚡ **Headless Execution**: Fast simulation of DFAs, NFAs, Pushdown Automata (PDA), Turing Machines (single and multi-tape), Mealy, and Moore machines.
- 🔬 **Formal Language Transformations**:
  - NFA $\rightarrow$ DFA conversion (`nfa-to-dfa`)
  - DFA minimization to canonical states (`minimize`)
  - Language Equivalence checking (`equals`) using minimization & isomorphism algorithms
  - FSA to Regular Expression extraction (`convert fsa-to-re`)
  - Combine multiple automata into composite machines (`fsa combine`)
- 📝 **Grammar Parsing & Transformation**:
  - FIRST and FOLLOW set computation with LL(1) parseability checking (`grammar first-follow`)
  - CYK Parser for Chomsky Normal Form (CNF) with derivation tracing
  - Brute-Force derivation parser
  - Production cleaner (eliminates $\lambda$/$\epsilon$, unit, and useless rules)
- 🌿 **L-System (Lindenmayer System) Engine**:
  - Deterministic and stochastic parallel rewriting string expansion at arbitrary derivation depth (`lsystem expand`)
- 🔄 **Cross-Model Conversions**:
  - FSA $\rightarrow$ Regular Expression
  - FSA $\leftrightarrow$ Regular Grammar (Right-Linear)
  - CFG $\rightarrow$ Pushdown Automaton (LL / LR)
  - PDA $\rightarrow$ Context-Free Grammar (CFG)
- 🛠️ **Programmatic Model Builder**: Create valid JFLAP `.jff` XML files via CLI flags or JSON definitions.
- 📊 **Graphviz Export**: Export any automaton to standard Graphviz `.dot` format.
- 🤖 **Native MCP Server**: Ready-to-use Model Context Protocol server exposing 10 tools to Claude Desktop, Antigravity, Cursor, and other agentic systems.

---

## Quick Start

### 1. Requirements
- **Java**: JRE/JDK 8 or higher (`java -version`).
- **Python**: Python 3.8 or higher (`python --version`).

### 2. Installation
Clone the repository:
```bash
git clone https://github.com/MatMridul/jflap-cli.git
cd jflap-cli
```

Optional: Install as a Python package to put `jflap` on your PATH:
```bash
pip install -e .
```

*Note: On first run, `jflap` will automatically compile the Java bridge (`JFLAPBridge.java`) if `.class` files are missing.*

---

## CLI Command Overview

Run commands with `jflap` (or `python jflap_cli.py`):

### Model Inspection
```bash
jflap inspect examples/contains_ab_nfa.jff
```
*Outputs an ASCII table showing state coordinates, initial/final flags, transition rules, and determinism.*

### Batch Simulation
```bash
jflap simulate examples/contains_ab_nfa.jff ab aabb bab bba xyz
```
*Outputs:*
```text
Simulation Results for: contains_ab_nfa.jff
Input String | Result  
-------------+---------
'ab'         | ACCEPTED
'aabb'       | ACCEPTED
'bab'        | ACCEPTED
'bba'        | REJECTED
'xyz'        | REJECTED
```

### Execution Tracing (PDA / Turing Machine)
```bash
# Step-by-step trace showing stack evolution on a Pushdown Automaton
jflap trace examples/an_bn_pda.jff aabb

# Step-by-step trace showing tape transformations on a Turing Machine
jflap trace examples/an_bn_cn_turing.jff abc
```

### Equivalence Testing
Formally prove whether a student's submission or alternative machine accepts the exact same language:
```bash
jflap fsa equals examples/contains_ab_nfa.jff examples/contains_ab_min_dfa.jff
# Result: The two automata ARE EQUIVALENT (accept the exact same language).
```

### Conversions & Regular Expressions
```bash
# Convert NFA to DFA
jflap fsa nfa-to-dfa examples/contains_ab_nfa.jff dfa.jff

# Minimize DFA
jflap fsa minimize dfa.jff min_dfa.jff

# Extract Regular Expression from FSA
jflap convert fsa-to-re examples/contains_ab_dfa.jff
# Output: Regular Expression: b*(aa*bb*+aa*bb*a(a+bb*a)*(?+bb*))

# Combine Two Automata into One
jflap fsa combine dfa1.jff dfa2.jff combined.jff

# Convert CFG to PDA (LL Mode)
jflap convert cfg-to-pda examples/an_bn_cfg.jff pda.jff --mode ll

# Convert Regular Grammar to FSA
jflap convert rg-to-fsa examples/regular_grammar_ab.jff fsa.jff
```

### Grammar Parsing & FIRST / FOLLOW Sets
```bash
# Parse string with CYK or Brute-Force
jflap grammar parse examples/regular_grammar_ab.jff aab

# Compute FIRST & FOLLOW Sets and test LL(1) parseability
jflap grammar first-follow examples/an_bn_cfg.jff
```

### L-System Expansion
```bash
# Expand L-System to level 4
jflap lsystem expand examples/fibonacci_lsystem.jff 4
# Output: ABAABABA (length 8)
```

### Graphviz DOT Export
```bash
jflap dot examples/contains_ab_min_dfa.jff -o machine.dot
```

### Programmatic Creation
```bash
# Create an FSA directly
jflap create fa --states q0,q1,q2 --initial q0 --finals q2 --transitions "q0,q1,0; q1,q2,1; q2,q2,0,1" -o parity.jff

# Or build from a JSON definition
jflap create --from-json examples/sample_spec.json -o generated.jff
```

### Official GUI Window
```bash
# Launch the standard JFLAP 7.1 graphical desktop interface
jflap gui examples/contains_ab_min_dfa.jff
```

---

## Model Context Protocol (MCP) Integration

The built-in MCP server (`jflap_mcp.py` / `jflap mcp`) allows AI assistants and autonomous agents to inspect, simulate, convert, and construct automata models directly.

### Quick Setup for AI Agents & Clients

#### 1. Google Antigravity / Gemini CLI (`~/.gemini/config/mcp_config.json`)
```json
{
  "mcpServers": {
    "jflap": {
      "command": "python",
      "args": ["<PATH_TO_REPO>/jflap_mcp.py"]
    }
  }
}
```

#### 2. Claude Desktop (`claude_desktop_config.json`)
```json
{
  "mcpServers": {
    "jflap": {
      "command": "python",
      "args": ["<PATH_TO_REPO>/jflap_mcp.py"]
    }
  }
}
```

#### 3. Cursor IDE (`.cursor/mcp.json` or Settings > Features > MCP)
```json
{
  "mcpServers": {
    "jflap": {
      "command": "python",
      "args": ["<PATH_TO_REPO>/jflap_mcp.py"]
    }
  }
}
```

*For Windsurf, Zed, and headless subagent shell execution, see the [Agent Setup Guide](docs/AGENT_SETUP.md).*

### Exposed MCP Tools
1. `jflap_inspect`: Inspect `.jff` file structure, determinism, completeness, unreachable states, and dead states.
2. `jflap_simulate`: Simulate input strings on any automaton with explicit PDA acceptance mode (`final_state` or `empty_stack`).
3. `jflap_simulate_batch`: High-performance batch simulation of hundreds or thousands of inputs in a single call.
4. `jflap_trace`: Step-by-step configuration trace (state, stack, tape, accept verdict).
5. `jflap_export_table`: Export a plain JSON transition table for DFA/NFA runtime executors.
6. `jflap_equivalence_check`: Check language equivalence between two FSAs, returning shortest distinguishing string on mismatch.
7. `jflap_fsa_operation`: NFA $\rightarrow$ DFA, minimization, language equivalence check, and automata combination (`combine`).
8. `jflap_grammar_operation`: CYK / Brute-force grammar parsing, rule cleaning, and FIRST / FOLLOW set analysis (`first_follow`).
9. `jflap_convert_model`: Cross-formalism conversion (FSA $\rightarrow$ RE, FSA $\leftrightarrow$ RG, CFG $\leftrightarrow$ PDA).
10. `jflap_lsystem_expand`: Parallel L-System symbol rewriting and derivation to depth $n$.
11. `jflap_create_model`: Programmatically build a `.jff` file from JSON.
12. `jflap_export_dot`: Export an automaton to Graphviz DOT.
13. `jflap_launch_gui`: Open the model in the interactive JFLAP 7.1 desktop window.

See [docs/MCP_SPECIFICATION.md](docs/MCP_SPECIFICATION.md) for full schemas and sample payloads.

---

## Performance & Latency Benchmarks

To meet the zero-latency safety requirements of build-time verification (such as the Chimeric Automaton verifier), the bridge features a warm, long-lived background JVM worker process communicating via line-delimited JSON over stdio pipes.

### Measured Latency Comparison (Windows 11, Intel Core i7, warm JVM)

| Mode | Calls / Inputs | Total Time | Latency per Call / Input | Speedup |
|:---|:---:|:---:|:---:|:---:|
| **Spawn-per-call (previous baseline)** | 10 calls | 2,743 ms | **274.3 ms** (min 252.0ms, max 309.2ms) | 1.0x (baseline) |
| **Persistent JVM Bridge** | 10 calls | 38.9 ms | **3.89 ms** (min 1.83ms, max 6.66ms) | **~70x faster** |
| **Batch Mode (`jflap_simulate_batch`)** | 1,000 inputs | 628.71 ms | **0.628 ms per input** | **>400x faster** |

*All figures are directly measured using `time.perf_counter()` on the test automaton `contains_ab_dfa.jff`.*

---

## Documentation Index

- [AI Agent Setup Guide](docs/AGENT_SETUP.md): Configuration instructions for Antigravity, Claude, Cursor, Windsurf, Zed, and subagents.
- [Architecture & Technical Design](docs/ARCHITECTURE.md): How the Java headless bridge works and how Swing modal blocks were neutralized.
- [MCP Specification](docs/MCP_SPECIFICATION.md): Tool-by-tool JSON schema, request parameters, and response structures.
- [CLI Reference Manual](docs/CLI_REFERENCE.md): Comprehensive documentation of all subcommands, arguments, and flags.

---

## Architecture & Quirks Resolved

1. **Bypassing Embedded `JOptionPane` Dialogs**:
   - In JFLAP's `PDAStepByStateSimulator`, the constructor pops up a modal dialog asking whether to accept by "Final State" or "Empty Stack". We engineered a `HeadlessPDASimulator` extending `AutomatonSimulator` that operates silently without GUI requirements.
   - In `TransducerFactory`, single-tape Turing machines prompt a modal dialog asking whether to open as standard TM or Building Blocks. We preemptively detect Turing models in DOM and instantiate `TMTransducer` directly.
2. **Explicit PDA Acceptance Modes**:
   - PDA simulation evaluates strictly one acceptance criterion: `"final_state"` (input consumed AND current state in $F$) or `"empty_stack"` (input consumed AND stack height is 0). Both criteria are never conflated into an unsound "either/or" check.
3. **Infinite Loop Prevention**:
   - Bounded BFS state exploration ensures that non-deterministic pushdown automata with $\lambda$-transitions or non-terminating Turing machines do not hang or exhaust system memory.
4. **Automated On-Demand Build**:
   - The CLI and MCP server automatically invoke `javac` if `JFLAPBridge.class` is absent or older than `JFLAPBridge.java`.

---

## License & Legal Compliance

This repository maintains a clean, transparent license split:

1. **CLI, MCP Server, Python Utilities & Java Bridge Wrappers**:
   - Licensed under the open-source **[MIT License](LICENSE)**.
   - Copyright (c) 2026 Mridul Mathur. Free for commercial and non-commercial use, modification, and integration.

2. **JFLAP 7.1 Upstream Binary & License** (`third_party/jflap/`):
   - JFLAP 7.1 is academic software developed by Prof. Susan H. Rodger and student contributors at Duke University ([https://www.jflap.org/](https://www.jflap.org/)).
   - Isolated cleanly in `third_party/jflap/JFLAP7.1.jar`.
   - Covered by the verbatim [third_party/jflap/LICENSE-JFLAP](third_party/jflap/LICENSE-JFLAP):
     - Copyright (c) 2002–2009 Susan H. Rodger, Duke University. All rights reserved.
     - **Key Terms**:
       1. The verbatim license text is bundled at `third_party/jflap/LICENSE-JFLAP`.
       2. No fee may be charged for any product containing JFLAP in source or binary form.
       3. Duke University and author names may not be used to endorse or promote derived products without prior written permission.
