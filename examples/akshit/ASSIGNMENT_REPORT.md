# SRM Institute of Science and Technology
### Department of Computational Intelligence / Computer Science & Engineering
## 21CSC301T — Formal Language and Automata Theory
### JFLAP Practical Assessment (Units 3–5)

| Student Information | Academic & Assessment Details |
| :--- | :--- |
| **Student Name:** Akshit | **Course Code:** 21CSC301T |
| **Section:** AD1 | **Course Title:** Formal Language and Automata Theory |
| **Assessment Type:** Practical / Lab Evaluation | **Total Evaluation Marks:** 20 Marks (4 Questions × 5 Marks) |

---

## Index of Files & Deliverables
All models have been created, inspected, and verified against JFLAP 7.1:

| Question | Topic | Model Type | Submission File | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Q1 (U3)** | For $S \to aSb \mid ab$, construct parse tree for `aaabbb` | CFG | [`q1_cfg_aaabbb.jff`](./q1_cfg_aaabbb.jff) |  Verified |
| **Q2 (U4)** | Construct a PDA for $\{a^n b^n c^m \mid n, m \ge 0\}$ | PDA | [`q2_pda_an_bn_cm.jff`](./q2_pda_an_bn_cm.jff) |  Verified |
| **Q3 (U5)** | Construct a TM to increment unary number $1^n \to 1^{n+1}$ | Turing Machine | [`q3_tm_unary_increment.jff`](./q3_tm_unary_increment.jff) |  Verified |
| **Q4 (U3)** | For $S \to aSb \mid \epsilon$, show leftmost & rightmost derivations of `aabb` | CFG | [`q4_cfg_aabb.jff`](./q4_cfg_aabb.jff) |  Verified |

---

## Question 1 (Unit 3): Parse Tree for `aaabbb` using $S \to aSb \mid ab$

### 1. Problem Statement
For the grammar $S \to aSb \mid ab$, construct the derivation and parse tree for the input string `aaabbb`. (5 Marks)

### 2. Formal Grammar Definition
A Context-Free Grammar is defined as $G = (V, \Sigma, R, S)$:
- **Variables ($V$):** $\{ S \}$
- **Terminals ($\Sigma$):** $\{ a, b \}$
- **Start Variable:** $S$
- **Production Rules ($R$):**
  1. $S \to a S b$
  2. $S \to a b$

*(Language generated: $L(G) = \{ a^n b^n \mid n \ge 1 \}$. Note that $\epsilon$ is not in the language because the base production is $S \to ab$.)*

### 3. Derivation Steps for `aaabbb`
$$\begin{aligned}
S &\implies a S b && \text{(Apply Rule 1: } S \to aSb\text{)} \\
  &\implies a (a S b) b = a a S b b && \text{(Apply Rule 1: } S \to aSb\text{)} \\
  &\implies a a (a b) b b = a a a b b b && \text{(Apply Rule 2: } S \to ab\text{)} \\
  &= aaabbb && \text{(Target Yield Derived)}
\end{aligned}$$

### 4. Parse Tree Construction
```text
            S
          / | \
         a  S  b
          / | \
         a  S  b
          /   \
         a     b
```
- **Leaves (left to right):** $a \to a \to a \to b \to b \to b$
- **Yield:** $a \cdot a \cdot a \cdot b \cdot b \cdot b = aaabbb$

### 5. JFLAP Simulation & Verification Evidence

| Input String | Length | Expected | JFLAP Parser Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **`aaabbb`** | 6 | ACCEPT | **ACCEPTED** *(Parsed via CYK / BruteParser)* |  PASS |
| **`ab`** | 2 | ACCEPT | **ACCEPTED** |  PASS |
| **`aabb`** | 4 | ACCEPT | **ACCEPTED** |  PASS |
| **`aaaabbbb`**| 8 | ACCEPT | **ACCEPTED** |  PASS |
| **`λ (empty)`**| 0 | REJECT | **REJECTED** *(Base is $ab$, $\epsilon \notin L$)* |  PASS |
| **`a`** | 1 | REJECT | **REJECTED** |  PASS |
| **`b`** | 1 | REJECT | **REJECTED** |  PASS |
| **`aab`** | 3 | REJECT | **REJECTED** |  PASS |
| **`abb`** | 3 | REJECT | **REJECTED** |  PASS |
| **`ba`** | 2 | REJECT | **REJECTED** |  PASS |

---

## Question 2 (Unit 4): PDA for $\{a^n b^n c^m \mid n, m \ge 0\}$

### 1. Problem Statement
Construct a Pushdown Automaton (PDA) recognizing the language $L = \{ a^n b^n c^m \mid n, m \ge 0 \}$. (5 Marks)

### 2. Formal Machine Architecture
A deterministic PDA $M = (Q, \Sigma, \Gamma, \delta, q_0, Z_0, F)$:
- **States ($Q$):** $\{ q_0, q_1, q_2, q_3, q_4 \}$
  - $q_0$: Initial state
  - $q_1$: Reading $a$'s and pushing $a$ onto stack
  - $q_2$: Reading $b$'s and popping $a$ from stack
  - $q_3$: Reading $c$'s (arbitrary count $m \ge 0$, stack unchanged at $Z$)
  - $q_4$: Final accepting state (`<final/>`)
- **Input Alphabet ($\Sigma$):** $\{ a, b, c \}$
- **Stack Alphabet ($\Gamma$):** $\{ Z, a \}$ ($Z$ marks the bottom of the stack)
- **Start State:** $q_0$
- **Initial Stack Symbol:** $Z$
- **Final States ($F$):** $\{ q_4 \}$

### 3. Transition Function ($\delta$)
- **Start State ($q_0$):**
  - $\delta(q_0, \lambda, Z) = \{ (q_4, Z) \}$ (Accept empty string $\epsilon$, where $n=0, m=0$)
  - $\delta(q_0, a, Z) = \{ (q_1, aZ) \}$ (For $n \ge 1$: read first $a$, push $a$)
  - $\delta(q_0, c, Z) = \{ (q_3, Z) \}$ (For $n = 0, m \ge 1$: read $c$ directly)
- **State $q_1$ (Pushing $a$'s):**
  - $\delta(q_1, a, a) = \{ (q_1, aa) \}$ (Push successive $a$'s)
  - $\delta(q_1, b, a) = \{ (q_2, \lambda) \}$ (First $b$: start popping $a$)
- **State $q_2$ (Matching $b$'s with $a$'s):**
  - $\delta(q_2, b, a) = \{ (q_2, \lambda) \}$ (Pop matching $a$ for each $b$)
  - $\delta(q_2, \lambda, Z) = \{ (q_4, Z) \}$ (All $a$'s matched, $m=0 \implies$ accept)
  - $\delta(q_2, c, Z) = \{ (q_3, Z) \}$ (All $a$'s matched, stack at $Z$; read first $c$)
- **State $q_3$ (Consuming $c$'s):**
  - $\delta(q_3, c, Z) = \{ (q_3, Z) \}$ (Read arbitrary $c$'s)
  - $\delta(q_3, \lambda, Z) = \{ (q_4, Z) \}$ (Accept after consuming $c$'s)

### 4. Step-by-Step Configuration Trace for `aabbc`
$$\begin{array}{ccllc}
\hline
\textbf{Step} & \textbf{State} & \textbf{Remaining Input} & \textbf{Stack} & \textbf{Action / Rule} \\
\hline
0 & q_0 & \text{'aabbc'} & [Z] & \text{Initial configuration} \\
1 & q_1 & \text{'abbc'} & [aZ] & \text{Read 'a', push 'aZ'} \\
2 & q_1 & \text{'bbc'} & [aaZ] & \text{Read 'a', push 'aa'} \\
3 & q_2 & \text{'bc'} & [aZ] & \text{Read 'b', pop 'a'} \\
4 & q_2 & \text{'c'} & [Z] & \text{Read 'b', pop 'a' } \implies \text{stack balanced} \\
5 & q_3 & \text{''} & [Z] & \text{Read 'c', transition to } q_3 \\
6 & q_4 & \text{''} & [Z] & \delta(q_3, \lambda, Z) \to (q_4, Z) \implies \textbf{ACCEPT (Final State)} \\
\hline
\end{array}$$

### 5. JFLAP Simulation Evidence

| Input String | Parameters ($n, m$) | Expected | JFLAP PDA Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **`λ (empty)`** | $n=0, m=0$ | ACCEPT | **ACCEPTED** |  PASS |
| **`c`** | $n=0, m=1$ | ACCEPT | **ACCEPTED** |  PASS |
| **`ccc`** | $n=0, m=3$ | ACCEPT | **ACCEPTED** |  PASS |
| **`ab`** | $n=1, m=0$ | ACCEPT | **ACCEPTED** |  PASS |
| **`abc`** | $n=1, m=1$ | ACCEPT | **ACCEPTED** |  PASS |
| **`aabb`** | $n=2, m=0$ | ACCEPT | **ACCEPTED** |  PASS |
| **`aabbc`** | $n=2, m=1$ | ACCEPT | **ACCEPTED** |  PASS |
| **`aabbccc`** | $n=2, m=3$ | ACCEPT | **ACCEPTED** |  PASS |
| **`aaabbb`** | $n=3, m=0$ | ACCEPT | **ACCEPTED** |  PASS |
| **`a`** | Invalid | REJECT | **REJECTED** |  PASS |
| **`b`** | Invalid | REJECT | **REJECTED** |  PASS |
| **`aab`** | $2 \ne 1$ | REJECT | **REJECTED** |  PASS |
| **`abb`** | $1 \ne 2$ | REJECT | **REJECTED** |  PASS |
| **`ac`** | Missing $b$ | REJECT | **REJECTED** |  PASS |
| **`ba`** | Out of order | REJECT | **REJECTED** |  PASS |
| **`bc`** | Missing $a$ | REJECT | **REJECTED** |  PASS |
| **`abca`** | $a$ after $c$ | REJECT | **REJECTED** |  PASS |
| **`aacc`** | Missing $b$ | REJECT | **REJECTED** |  PASS |

---

## Question 3 (Unit 5): Turing Machine to Increment Unary Number $1^n \to 1^{n+1}$

### 1. Problem Statement
Construct a Turing Machine (TM) transducer that increments a unary number $1^n$ to $1^{n+1}$ (i.e. computes $f(1^n) = 1^{n+1}$ for $n \ge 0$). (5 Marks)

### 2. Formal Machine Architecture
A deterministic 1-tape Turing Machine transducer $M = (Q, \Sigma, \Gamma, \delta, q_0, \square, F)$:
- **Input Alphabet ($\Sigma$):** $\{ 1 \}$
- **Tape Alphabet ($\Gamma$):** $\{ 1, \square \}$
- **States ($Q$):** $\{ q_0, q_1 \}$
  - $q_0$: Initial state (scanning right to the end of the input sequence)
  - $q_1$: Final accepting state (`q_accept`)
- **Start State:** $q_0$
- **Accept State ($F$):** $\{ q_1 \}$

### 3. Transition Function ($\delta$)
$$\begin{array}{ccccc}
\hline
\textbf{Current State} & \textbf{Read Symbol} & \textbf{Next State} & \textbf{Write Symbol} & \textbf{Head Direction} \\
\hline
q_0 & 1 & q_0 & 1 & \text{R} \\
q_0 & \square & q_1 & 1 & \text{R} \\
\hline
\end{array}$$

- **Operational Logic:**
  1. In state $q_0$, the head reads `1`, leaves `1` unchanged, and advances right (`R`) across all $n$ ones.
  2. When the head encounters the blank cell $\square$ immediately following the string, it overwrites $\square$ with `1` and transitions to $q_1$ (`q_accept`).
  3. The number of ones on the tape is now precisely $n + 1$.
  4. If $n = 0$ (tape is blank $\square$), $q_0$ immediately writes `1` and halts in $q_1$, yielding $1^1 = 1$.

### 4. Step-by-Step Configuration Trace for `111` ($n=3 \to n=4$)
$$\begin{array}{ccllc}
\hline
\textbf{Step} & \textbf{State} & \textbf{Head Position} & \textbf{Tape Contents} & \textbf{Action} \\
\hline
0 & q_0 & \text{Index 0} & \text{[111?]} & \text{Read '1'} \to \text{write '1', move R} \\
1 & q_0 & \text{Index 1} & \text{[111?]} & \text{Read '1'} \to \text{write '1', move R} \\
2 & q_0 & \text{Index 2} & \text{[111?]} & \text{Read '1'} \to \text{write '1', move R} \\
3 & q_0 & \text{Index 3} & \text{[111?]} & \text{Read } \square \to \text{write '1', move R} \\
4 & q_1 & \text{Index 4} & \text{[1111?]} & \textbf{Final state reached } \implies \textbf{HALT \& ACCEPT} \\
\hline
\end{array}$$
**Result:** Initial unary value $3$ (`111`) is successfully incremented to $4$ (`1111`).

### 5. JFLAP Simulation Evidence

| Input ($1^n$) | Input Value ($n$) | JFLAP Result | Output Tape ($1^{n+1}$) | Output Value ($n+1$) | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`λ (empty)`** | $0$ | **ACCEPTED** | `1?` | $1$ |  PASS |
| **`1`** | $1$ | **ACCEPTED** | `11?` | $2$ |  PASS |
| **`11`** | $2$ | **ACCEPTED** | `111?` | $3$ |  PASS |
| **`111`** | $3$ | **ACCEPTED** | `1111?` | $4$ |  PASS |
| **`1111`** | $4$ | **ACCEPTED** | `11111?` | $5$ |  PASS |
| **`0`** | Invalid input | **REJECTED** | `0` | - |  PASS |
| **`10`** | Invalid input | **REJECTED** | `10` | - |  PASS |

---

## Question 4 (Unit 3): Leftmost and Rightmost Derivations of `aabb` for $S \to aSb \mid \epsilon$

### 1. Problem Statement
For the grammar $S \to aSb \mid \epsilon$, show the leftmost derivation and rightmost derivation of the string `aabb`. (5 Marks)

### 2. Formal Grammar Definition
A Context-Free Grammar $G = (V, \Sigma, R, S)$:
- **Variables ($V$):** $\{ S \}$
- **Terminals ($\Sigma$):** $\{ a, b \}$
- **Start Variable:** $S$
- **Production Rules ($R$):**
  1. $S \to a S b$
  2. $S \to \epsilon$ (or $\lambda$)

### 3. Leftmost Derivation (LMD)
In a leftmost derivation, at each step, the non-terminal chosen for substitution is the **leftmost** non-terminal variable in the sentential form:
$$\begin{aligned}
S &\implies_{lm} a S b && \text{(Apply Rule 1: } S \to aSb\text{)} \\
  &\implies_{lm} a (a S b) b = a a S b b && \text{(Apply Rule 1 to leftmost variable } S\text{)} \\
  &\implies_{lm} a a (\epsilon) b b = a a b b && \text{(Apply Rule 2: } S \to \epsilon\text{)} \\
  &= aabb && \text{(Yield Derived)}
\end{aligned}$$

### 4. Rightmost Derivation (RMD)
In a rightmost derivation, at each step, the non-terminal chosen for substitution is the **rightmost** non-terminal variable in the sentential form:
$$\begin{aligned}
S &\implies_{rm} a S b && \text{(Apply Rule 1: } S \to aSb\text{)} \\
  &\implies_{rm} a (a S b) b = a a S b b && \text{(Apply Rule 1 to rightmost variable } S\text{)} \\
  &\implies_{rm} a a (\epsilon) b b = a a b b && \text{(Apply Rule 2: } S \to \epsilon\text{)} \\
  &= aabb && \text{(Yield Derived)}
\end{aligned}$$

### 5. Theoretical Justification (Linear Grammar Equivalence)
- **Linear Grammar Property:** A context-free grammar is defined as linear if every production rule contains at most one non-terminal on its right-hand side.
- In this grammar, every intermediate sentential form ($S$, $aSb$, $aaSbb$) contains **exactly one** non-terminal variable ($S$).
- Because there is never more than one non-terminal variable present at any derivation step, that variable is simultaneously both the leftmost non-terminal and the rightmost non-terminal.
- Therefore, the sequence of sentential forms in the leftmost derivation and the rightmost derivation are **identical**:
  $$S \implies aSb \implies aaSbb \implies aabb$$
- The grammar generates an **unambiguous** parse tree:
```text
            S
          / | \
         a  S  b
          / | \
         a  S  b
            |
            ε
```

### 6. JFLAP Simulation Evidence

| String | Length | Expected | JFLAP Parser Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| **`aabb`** | 4 | ACCEPT | **ACCEPTED** *(Parsed via CYK / BruteParser)* |  PASS |
| **`λ (empty)`**| 0 | ACCEPT | **ACCEPTED** *(Via Rule $S \to \epsilon$)* |  PASS |
| **`ab`** | 2 | ACCEPT | **ACCEPTED** |  PASS |
| **`aaabbb`** | 6 | ACCEPT | **ACCEPTED** |  PASS |
| **`a`** | 1 | REJECT | **REJECTED** |  PASS |
| **`b`** | 1 | REJECT | **REJECTED** |  PASS |
| **`aab`** | 3 | REJECT | **REJECTED** |  PASS |
| **`abb`** | 3 | REJECT | **REJECTED** |  PASS |
| **`ba`** | 2 | REJECT | **REJECTED** |  PASS |

---

## Instructions for Opening in Desktop JFLAP (GUI)

If visual screenshots of the canvas are required:
```bash
# Open Q1 CFG
jflap gui C:\Users\mridu\jflap-assignment-akshit\q1_cfg_aaabbb.jff

# Open Q2 PDA
jflap gui C:\Users\mridu\jflap-assignment-akshit\q2_pda_an_bn_cm.jff

# Open Q3 Turing Machine
jflap gui C:\Users\mridu\jflap-assignment-akshit\q3_tm_unary_increment.jff

# Open Q4 CFG
jflap gui C:\Users\mridu\jflap-assignment-akshit\q4_cfg_aabb.jff
```
In the window, go to **Input > Multiple Run**, enter the test strings, and click **Run Inputs** to observe the simulation.
