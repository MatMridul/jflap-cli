# AI Agent Integration & Setup Guide (JFLAP 7.1 MCP)

This guide provides instructions for configuring AI assistants, autonomous coding agents, and language models (Claude, Gemini, Antigravity, Cursor, Windsurf, Zed, LangChain, CrewAI) to interact with and control **JFLAP 7.1** programmatically.

---

## 1. Integration Architectures

Agents can control JFLAP 7.1 through two primary mechanisms:

```
                          ┌──────────────────────────┐
                          │   AI Assistant / Agent   │
                          │ (Claude / Gemini / etc.) │
                          └─────────────┬────────────┘
                                        │
                 ┌──────────────────────┴──────────────────────┐
                 │                                             │
      Method A: MCP Protocol                        Method B: Headless CLI
    (JSON-RPC 2.0 over stdio)                      (Subprocess / Shell tool)
                 │                                             │
                 ▼                                             ▼
        ┌─────────────────┐                           ┌─────────────────┐
        │  jflap_mcp.py   │                           │  jflap_cli.py   │
        └────────┬────────┘                           └────────┬────────┘
                 │                                             │
                 └──────────────────────┬──────────────────────┘
                                        ▼
                             ┌─────────────────────┐
                             │  JFLAPBridge.class  │
                             └──────────┬──────────┘
                                        ▼
                             ┌─────────────────────┐
                             │    JFLAP7.1.jar     │
                             │ (Formal Algorithms) │
                             └─────────────────────┘
```

1. **Model Context Protocol (MCP)**: Native tool calling via JSON-RPC 2.0 over standard I/O. The agent sees JFLAP capabilities as callable functions with typed JSON schemas.
2. **Headless CLI / Shell Execution**: The agent executes `jflap <command> --json` via its terminal/command execution tool and parses structured JSON outputs.

---

## 2. MCP Server Configuration (Platform-Specific)

### A. Google Antigravity / Gemini CLI
Add the server entry to your global MCP configuration file:
- **Windows**: `C:\Users\<USERNAME>\.gemini\config\mcp_config.json`
- **macOS / Linux**: `~/.gemini/config/mcp_config.json`

```json
{
  "mcpServers": {
    "jflap": {
      "command": "python",
      "args": [
        "<PATH_TO_REPO>/jflap_mcp.py"
      ]
    }
  }
}
```

### B. Claude Desktop
Add to your Claude Desktop configuration file:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
- **Linux**: `~/.config/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "jflap": {
      "command": "python",
      "args": [
        "<PATH_TO_REPO>/jflap_mcp.py"
      ]
    }
  }
}
```

### C. Cursor IDE
In Cursor, create or update `.cursor/mcp.json` in your project root or navigate to **Settings > Features > MCP > Add New MCP Server**:

- **Name**: `jflap`
- **Type**: `command`
- **Command**: `python <PATH_TO_REPO>/jflap_mcp.py`

Or in `.cursor/mcp.json`:
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

### D. Windsurf (Codeium)
Add to `~/.codeium/windsurf/mcp_config.json`:
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

### E. Zed Editor
Add to your Zed `settings.json` (`~/.config/zed/settings.json`):
```json
{
  "context_servers": {
    "jflap": {
      "command": {
        "env": {},
        "path": "python",
        "args": ["<PATH_TO_REPO>/jflap_mcp.py"]
      }
    }
  }
}
```

---

## 3. Alternative: Direct Shell CLI for Autonomous Subagents

If your agent runs in an environment with bash or terminal execution tools (e.g. OpenAI Assistants API, AutoGPT, CrewAI, or subagents without MCP), configure the agent to call the CLI directly using the `--json` flag:

```bash
# Inspection
python jflap_cli.py inspect <file.jff> --json

# Simulation
python jflap_cli.py simulate <file.jff> <input1> <input2> --json

# Step-by-step tracing
python jflap_cli.py trace <file.jff> <input> --json

# Language Equivalence Proof
python jflap_cli.py fsa equals <file1.jff> <file2.jff> --json

# NFA to DFA
python jflap_cli.py fsa nfa-to-dfa <input_nfa.jff> <output_dfa.jff> --json

# DFA Minimization
python jflap_cli.py fsa minimize <input_dfa.jff> <output_min.jff> --json

# FSA to Regular Expression
python jflap_cli.py convert fsa-to-re <input_fsa.jff> --json

# Grammar Parsing & FIRST/FOLLOW Analysis
python jflap_cli.py grammar first-follow <grammar.jff> --json
python jflap_cli.py grammar parse <grammar.jff> <string> --json

# L-System Expansion
python jflap_cli.py lsystem expand <lsystem.jff> <level> --json
```

All commands output standard JSON dictionaries to `stdout` and exit with code `0` on success or non-zero with error explanations on `stderr`.

---

## 4. System Prompt Snippet for Agents

To instruct an agent on how and when to use JFLAP 7.1 tools, add the following prompt snippet to your agent's system prompt or guidelines:

```markdown
### JFLAP 7.1 Formal Languages Engine
You have access to the JFLAP 7.1 suite for formal languages, automata theory, and compiler design.
When asked to analyze, design, simulate, or verify theoretical computing models, use the `jflap_*` tools:
- `jflap_inspect`: Inspect state diagrams, transitions, alphabet, and determinism.
- `jflap_simulate`: Test input strings on DFAs, NFAs, PDAs, Turing Machines, Mealy, and Moore machines.
- `jflap_trace`: Trace step-by-step execution (including stack memory for PDAs and tape cells for Turing Machines).
- `jflap_fsa_operation`:
  - `equals`: Formally prove whether two state machines accept the exact same language.
  - `minimize`: Minimize a DFA to its canonical minimum state count.
  - `nfa_to_dfa`: Convert non-deterministic finite automata to deterministic ones.
  - `combine`: Merge two automata into one composite machine.
- `jflap_grammar_operation`:
  - `first_follow`: Calculate FIRST & FOLLOW sets and check whether a grammar is LL(1).
  - `parse`: Parse input strings using CYK (for CNF) or Brute-Force derivation.
  - `clean`: Remove lambda-productions, unit productions, and useless symbols.
- `jflap_convert_model`:
  - `fsa_to_re`: Extract the regular expression from an FSA using state elimination.
  - `fsa_to_rg` / `rg_to_fsa`: Convert between FSAs and Regular Grammars.
  - `cfg_to_pda` / `pda_to_cfg`: Convert between CFGs and Pushdown Automata.
- `jflap_lsystem_expand`: Compute parallel rewriting derivations for Lindenmayer systems.
- `jflap_create_model`: Programmatically build new `.jff` XML model files from JSON specs.
- `jflap_export_dot`: Generate Graphviz DOT diagrams of automata for visualization.

Always prefer simulating and proving equivalence programmatically using these tools rather than manually guessing transitions.
```

---

## 5. Prerequisites & Auto-Bootstrap

The server is self-bootstrapping:
1. **Java 8+**: JRE or JDK (`java -version`).
2. **Python 3.8+**: Standard library only; no external pip dependencies are strictly required for the core MCP server.
3. **Automatic Compilation**: When `jflap_mcp.py` starts, it automatically detects if `third_party/jflap/JFLAP7.1.jar` is present (downloading it from official Duke servers if missing) and runs `javac JFLAPBridge.java` if `JFLAPBridge.class` is absent or out-of-date.

---

## 6. Verification Test

To verify that an agent's MCP connection is operating correctly:
1. Start the server via your agent client.
2. Call `jflap_inspect` on `examples/contains_ab_nfa.jff`.
3. Expected output:
```json
{
  "category": "automaton",
  "type": "fa",
  "isDeterministic": false,
  "initialState": 0,
  "finalStates": [2],
  "states": [
    {"id": 0, "name": "q0", "initial": true, "final": false},
    {"id": 1, "name": "q1", "initial": false, "final": false},
    {"id": 2, "name": "q2", "initial": false, "final": true}
  ]
}
```
If this JSON is returned, the integration is verified and ready for autonomous agent use.
