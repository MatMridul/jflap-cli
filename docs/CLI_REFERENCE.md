# JFLAP 7.1 CLI Reference Manual

The `jflap` command provides complete command-line access to JFLAP 7.1 formal language operations.

---

## Command Syntax
```bash
jflap <command> [subcommand] [arguments...] [options]
```

---

## 1. `inspect`
Inspects any JFLAP model file (`.jff`) and displays its states, transitions, initial/final configurations, and determinism.

```bash
jflap inspect <file.jff> [--json]
```

- `<file.jff>`: Path to the target JFLAP file.
- `--json`: Output raw JSON instead of human-readable ASCII tables.

---

## 2. `simulate`
Simulates one or more input strings on an automaton.

```bash
jflap simulate <file.jff> [input1 input2 ...] [--json]
```

- `<file.jff>`: Path to the `.jff` file.
- `[input1 input2 ...]`: Space-separated input strings. If omitted, the empty string ($\lambda$) is simulated.
- `--json`: Output raw JSON.

---

## 3. `trace`
Performs step-by-step configuration tracing for an input string.

```bash
jflap trace <file.jff> [input] [--json]
```

- Output columns include: `Step`, `State`, `Remaining Input`, `Stack` (PDA only), `Tape` (TM only), and `Accept?`.

---

## 4. `fsa`
Finite State Automata operations.

### `jflap fsa nfa-to-dfa`
Converts a nondeterministic finite automaton to a deterministic finite automaton.
```bash
jflap fsa nfa-to-dfa <input_nfa.jff> <output_dfa.jff> [--json]
```

### `jflap fsa minimize`
Minimizes a DFA to its canonical minimum state count.
```bash
jflap fsa minimize <input_dfa.jff> <output_min_dfa.jff> [--json]
```

### `jflap fsa equals`
Checks whether two automata accept the exact same formal language.
```bash
jflap fsa equals <file1.jff> <file2.jff> [--json]
```

### `jflap fsa combine`
Combines two automata into a single composite machine.
```bash
jflap fsa combine <file1.jff> <file2.jff> <output.jff> [--json]
```

---

## 5. `grammar`
Grammar operations.

### `jflap grammar parse`
Parses an input string using CYK (if in CNF) or JFLAP's Brute-Force parser, returning the derivation.
```bash
jflap grammar parse <grammar.jff> <input_string> [--json]
```

### `jflap grammar clean`
Removes lambda/epsilon, unit, and useless productions from a grammar.
```bash
jflap grammar clean <input.jff> <output.jff> [--json]
```

### `jflap grammar first-follow`
Computes the FIRST and FOLLOW sets for all grammar symbols, and formally tests whether the grammar satisfies the LL(1) condition.
```bash
jflap grammar first-follow <grammar.jff> [--json]
```

---

## 6. `convert`
Cross-formalism model conversions.

### `jflap convert fsa-to-re`
Converts a Finite State Automaton to an equivalent Regular Expression using generalized transition graph (GTG) state-elimination.
```bash
jflap convert fsa-to-re <input_fsa.jff> [output_file] [--json]
```

### `jflap convert fsa-to-rg`
Converts a Finite State Automaton to an equivalent Right-Linear Regular Grammar.
```bash
jflap convert fsa-to-rg <input_fsa.jff> <output_grammar.jff>
```

### `jflap convert rg-to-fsa`
Converts a Right-Linear Regular Grammar to an equivalent FSA.
```bash
jflap convert rg-to-fsa <input_grammar.jff> <output_fsa.jff>
```

### `jflap convert cfg-to-pda`
Converts a Context-Free Grammar to a Pushdown Automaton.
```bash
jflap convert cfg-to-pda <input_cfg.jff> <output_pda.jff> [--mode {ll,lr}]
```
- `--mode`: Parsing technique (`ll` or `lr`). Default: `ll`.

### `jflap convert pda-to-cfg`
Converts a Pushdown Automaton to a Context-Free Grammar.
```bash
jflap convert pda-to-cfg <input_pda.jff> <output_cfg.jff>
```

---

## 7. `create`
Programmatically generates a `.jff` XML file.

### Via Command-Line Flags
```bash
# Create an FSA
jflap create fa --states q0,q1,q2 --initial q0 --finals q2 --transitions "q0,q1,a; q1,q2,b" -o my_fa.jff

# Create a PDA
jflap create pda --states q0,q1,q2 --initial q0 --finals q2 --transitions "q0,q1,a,Z,aZ; q1,q2,b,a,λ" -o my_pda.jff

# Create a Grammar
jflap create grammar --start S --productions "S->aSb, S->ab" -o my_cfg.jff
```

### From a JSON Specification File
```bash
jflap create --from-json spec.json -o my_model.jff
```

---

## 8. `dot`
Exports an automaton into Graphviz DOT syntax.

```bash
jflap dot <input.jff> [-o output.dot]
```

---

## 9. `lsystem`
L-System (Lindenmayer System) operations.

### `jflap lsystem expand`
Expands an L-System axiom through parallel rule substitution to a specified recursion level.
```bash
jflap lsystem expand <file.jff> [level] [--json]
```
- `<file.jff>`: Path to L-System `.jff` file.
- `[level]`: Expansion depth (default: 3).

---

## 10. `gui`
Launches the official JFLAP 7.1 graphical desktop window.

```bash
jflap gui [file.jff]
```

---

## 11. `mcp`
Starts the JSON-RPC stdio Model Context Protocol (MCP) server for AI assistants.

```bash
jflap mcp
```
