# SRM Institute of Science and Technology
### Department of Computational Intelligence / Computer Science & Engineering
## 21CSC301T — Formal Language and Automata Theory
### JFLAP Practical Assessment | SET 14 (Units 3–5)

| Student Details | Submission Metadata |
| :--- | :--- |
| **Student Name:** Mridul Mathur | **Course Code:** 21CSC301T |
| **Registration / Roll No.:** RA2411026010137 | **Course Title:** Formal Language and Automata Theory |
| **Section:** AD1 | **Question Set:** SET 14 |
| **Assessment:** Practical Assessment / Lab Evaluation | **Total Marks:** 20 Marks (4 Questions × 5 Marks) |

---

## Index of Files & Deliverables
All models have been created, inspected, and verified against JFLAP 7.1:

| Question | Topic | Model Type | Submission File | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Q1 (U3)** | $\{w w^R \mid w \in \{0, 1\}^*\}$, parse `1001` | CFG | [`q1_cfg_wwR.jff`](./q1_cfg_wwR.jff) |  Verified |
| **Q2 (U4)** | Nested `()`, `[]`, `{}` Brackets | PDA | [`q2_pda_brackets.jff`](./q2_pda_brackets.jff) |  Verified |
| **Q3 (U4)** | Equal $a$'s and $b$'s in arbitrary order | PDA | [`q3_pda_equal_ab.jff`](./q3_pda_equal_ab.jff) |  Verified |
| **Q4 (U5)** | Deciding $\{w w \mid w \in \{0, 1\}^*\}$ | Turing Machine | [`q4_tm_ww.jff`](./q4_tm_ww.jff) |  Verified |

---

## Question 1 (Unit 3): CFG for Even-Length Palindromes & Parsing 1001

### 1. Problem Statement
Create a Context-Free Grammar (CFG) for the language $L = \{ w w^R \mid w \in \{0, 1\}^* \}$ and parse the input string `1001`. (5 Marks)

### 2. Formal Grammar Definition
A CFG is defined as a 4-tuple $G = (V, \Sigma, R, S)$:
- **Variables ($V$):** $\{ S \}$
- **Terminals ($\Sigma$):** $\{ 0, 1 \}$
- **Start Variable:** $S$
- **Production Rules ($R$):**
  1. $S \to 0 S 0$
  2. $S \to 1 S 1$
  3. $S \to \lambda$ (empty string $\epsilon$)

### 3. Derivation of `1001`
To derive the string `1001`:
$$\begin{aligned}
S &\implies 1 S 1 && \text{(Rule 2: } S \to 1S1\text{)} \\
  &\implies 1 0 S 0 1 && \text{(Rule 1: } S \to 0S0\text{)} \\
  &\implies 1 0 \lambda 0 1 && \text{(Rule 3: } S \to \lambda\text{)} \\
  &= 1001 && \text{(Yield)}
\end{aligned}$$

### 4. Test Suite Execution Evidence
Verified via JFLAP 7.1 Headless CYK / Brute-Force Parser:

| String | Type | Expected | JFLAP Parser Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| `1001` | Target ($w=10$) | ACCEPT | **ACCEPTED** |  PASS |
| `00` | Palindrome ($w=0$) | ACCEPT | **ACCEPTED** |  PASS |
| `11` | Palindrome ($w=1$) | ACCEPT | **ACCEPTED** |  PASS |
| `0110` | Palindrome ($w=01$) | ACCEPT | **ACCEPTED** |  PASS |
| `110011`| Palindrome ($w=110$)| ACCEPT | **ACCEPTED** |  PASS |
| `101` | Odd palindrome | REJECT | **REJECTED** |  PASS |
| `1010` | Asymmetric | REJECT | **REJECTED** |  PASS |
| `110` | Asymmetric | REJECT | **REJECTED** |  PASS |
| `010` | Odd length | REJECT | **REJECTED** |  PASS |

---

## Question 2 (Unit 4): PDA for Nested `()`, `[]`, and `{}` Bracket Pairs

### 1. Problem Statement
Construct a Pushdown Automaton (PDA) recognizing the Dyck language of correctly nested and matched parentheses `()`, square brackets `[]`, and curly braces `{}`. (5 Marks)

### 2. Formal Definition
A PDA is defined as a 7-tuple $M = (Q, \Sigma, \Gamma, \delta, q_0, Z_0, F)$:
- **States ($Q$):** $\{ q_0, q_1, q_2 \}$
- **Input Alphabet ($\Sigma$):** $\{ (, ), [, ], \{, \} \}$
- **Stack Alphabet ($\Gamma$):** $\{ Z, (, [, \{ \}$ ($Z$ is the initial stack bottom marker)
- **Start State:** $q_0$
- **Initial Stack Symbol:** $Z$
- **Final States ($F$):** $\{ q_2 \}$

### 3. Transition Function ($\delta$)
- **Initialization:**
  - $\delta(q_0, \lambda, Z) = \{ (q_1, Z) \}$ (Start processing)
  - $\delta(q_0, \lambda, Z) = \{ (q_2, Z) \}$ (Accept empty string $\epsilon$)
- **Push Transitions (on reading any opening bracket):**
  - $\delta(q_1, (, X) = \{ (q_1, (X) \}$ for all $X \in \{ Z, (, [, \{ \}$
  - $\delta(q_1, [, X) = \{ (q_1, [X) \}$ for all $X \in \{ Z, (, [, \{ \}$
  - $\delta(q_1, \{, X) = \{ (q_1, \{X) \}$ for all $X \in \{ Z, (, [, \{ \}$
- **Pop Transitions (matching closing brackets):**
  - $\delta(q_1, ), () = \{ (q_1, \lambda) \}$
  - $\delta(q_1, ], [) = \{ (q_1, \lambda) \}$
  - $\delta(q_1, \}, \{) = \{ (q_1, \lambda) \}$
- **Acceptance:**
  - $\delta(q_1, \lambda, Z) = \{ (q_2, Z) \}$ (All open brackets resolved; enter final state $q_2$)

### 4. Step-by-Step Configuration Trace for `([])`
$$\begin{array}{ccllc}
\hline
\textbf{Step} & \textbf{State} & \textbf{Remaining Input} & \textbf{Stack} & \textbf{Action / Rule} \\
\hline
0 & q_0 & \text{'([])'} & [Z] & \text{Initial configuration} \\
1 & q_1 & \text{'([])'} & [Z] & \delta(q_0, \lambda, Z) \to (q_1, Z) \\
2 & q_1 & \text{'[])'} & [(Z] & \text{Read '(', push onto stack} \\
3 & q_1 & \text{'])'} & [[(Z] & \text{Read '[', push onto stack} \\
4 & q_1 & \text{')'} & [(Z] & \text{Read ']', matches '[' } \implies \text{pop} \\
5 & q_1 & \text{''} & [Z] & \text{Read ')', matches '(' } \implies \text{pop} \\
6 & q_2 & \text{''} & [Z] & \delta(q_1, \lambda, Z) \to (q_2, Z) \implies \textbf{ACCEPT (Final State)} \\
\hline
\end{array}$$

### 5. Test Suite Execution Evidence

| Input String | Description | Expected | JFLAP PDA Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| `()` | Single pair | ACCEPT | **ACCEPTED** |  PASS |
| `([])` | Nested bracket | ACCEPT | **ACCEPTED** |  PASS |
| `{[()]}` | All three nested | ACCEPT | **ACCEPTED** |  PASS |
| `(){}[()]` | Concatenated pairs | ACCEPT | **ACCEPTED** |  PASS |
| `(]` | Mismatched closing | REJECT | **REJECTED** |  PASS |
| `([)]` | Cross-nested | REJECT | **REJECTED** |  PASS |
| `(((` | Unclosed brackets | REJECT | **REJECTED** |  PASS |
| `())` | Excess closing | REJECT | **REJECTED** |  PASS |
| `}{` | Inverted order | REJECT | **REJECTED** |  PASS |

---

## Question 3 (Unit 4): PDA for Equal Numbers of $a$ and $b$ in Arbitrary Order

### 1. Problem Statement
Construct a Pushdown Automaton (PDA) for the language $L = \{ w \in \{a, b\}^* \mid N_a(w) = N_b(w) \}$, where $a$'s and $b$'s appear in arbitrary order. (5 Marks)

### 2. Formal Definition
A PDA is defined as a 7-tuple $M = (Q, \Sigma, \Gamma, \delta, q_0, Z_0, F)$:
- **States ($Q$):** $\{ q_0, q_1, q_2 \}$
- **Input Alphabet ($\Sigma$):** $\{ a, b \}$
- **Stack Alphabet ($\Gamma$):** $\{ Z, a, b \}$ ($Z$ marks the balanced stack bottom)
- **Start State:** $q_0$
- **Initial Stack Symbol:** $Z$
- **Final States ($F$):** $\{ q_2 \}$

### 3. Transition Function ($\delta$)
- **Initialization & Empty String:**
  - $\delta(q_0, \lambda, Z) = \{ (q_1, Z), (q_2, Z) \}$
- **When Stack is Balanced (Top is $Z$):**
  - $\delta(q_1, a, Z) = \{ (q_1, aZ) \}$ (Excess of $a$)
  - $\delta(q_1, b, Z) = \{ (q_1, bZ) \}$ (Excess of $b$)
- **When Top of Stack is $a$ (Excess of $a$'s so far):**
  - $\delta(q_1, a, a) = \{ (q_1, aa) \}$ (Accumulate more $a$)
  - $\delta(q_1, b, a) = \{ (q_1, \lambda) \}$ (Incoming $b$ cancels an $a$)
- **When Top of Stack is $b$ (Excess of $b$'s so far):**
  - $\delta(q_1, b, b) = \{ (q_1, bb) \}$ (Accumulate more $b$)
  - $\delta(q_1, a, b) = \{ (q_1, \lambda) \}$ (Incoming $a$ cancels a $b$)
- **Acceptance:**
  - $\delta(q_1, \lambda, Z) = \{ (q_2, Z) \}$ (All $a$'s and $b$'s matched; enter final state $q_2$)

### 4. Step-by-Step Configuration Trace for `baab`
$$\begin{array}{ccllc}
\hline
\textbf{Step} & \textbf{State} & \textbf{Remaining Input} & \textbf{Stack} & \textbf{Action / Rule} \\
\hline
0 & q_0 & \text{'baab'} & [Z] & \text{Start configuration} \\
1 & q_1 & \text{'baab'} & [Z] & \delta(q_0, \lambda, Z) \to (q_1, Z) \\
2 & q_1 & \text{'aab'} & [bZ] & \text{Read 'b', push 'bZ'} \\
3 & q_1 & \text{'ab'} & [Z] & \text{Read 'a', pop 'b' } \implies \text{balanced} \\
4 & q_1 & \text{'b'} & [aZ] & \text{Read 'a', push 'aZ'} \\
5 & q_1 & \text{''} & [Z] & \text{Read 'b', pop 'a' } \implies \text{balanced} \\
6 & q_2 & \text{''} & [Z] & \delta(q_1, \lambda, Z) \to (q_2, Z) \implies \textbf{ACCEPT} \\
\hline
\end{array}$$

### 5. Test Suite Execution Evidence

| Input String | Counts ($N_a, N_b$) | Expected | JFLAP PDA Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| `ab` | (1, 1) | ACCEPT | **ACCEPTED** |  PASS |
| `ba` | (1, 1) | ACCEPT | **ACCEPTED** |  PASS |
| `aabb` | (2, 2) | ACCEPT | **ACCEPTED** |  PASS |
| `abab` | (2, 2) | ACCEPT | **ACCEPTED** |  PASS |
| `bbaa` | (2, 2) | ACCEPT | **ACCEPTED** |  PASS |
| `baab` | (2, 2) | ACCEPT | **ACCEPTED** |  PASS |
| `ababbaba` | (4, 4) | ACCEPT | **ACCEPTED** |  PASS |
| `a` | (1, 0) | REJECT | **REJECTED** |  PASS |
| `b` | (0, 1) | REJECT | **REJECTED** |  PASS |
| `aab` | (2, 1) | REJECT | **REJECTED** |  PASS |
| `abb` | (1, 2) | REJECT | **REJECTED** |  PASS |
| `aaabb` | (3, 2) | REJECT | **REJECTED** |  PASS |
| `bba` | (1, 2) | REJECT | **REJECTED** |  PASS |

---

## Question 4 (Unit 5): Turing Machine Deciding $\{w w \mid w \in \{0, 1\}^*\}$

### 1. Problem Statement
Construct a Turing Machine (TM) deciding the non-context-free language $L = \{ w w \mid w \in \{0, 1\}^* \}$. (5 Marks)

### 2. Algorithm & Machine Architecture
A deterministic 1-tape Turing Machine $M = (Q, \Sigma, \Gamma, \delta, q_0, \square, F)$:
- **Input Alphabet ($\Sigma$):** $\{ 0, 1 \}$
- **Tape Alphabet ($\Gamma$):** $\{ 0, 1, X, Y, A, B, \square \}$
  - $X, Y$: Marked symbols for first half (representing $0$ and $1$).
  - $A, B$: Marked symbols for second half (representing $0$ and $1$).
  - $\square$: Blank cell.
- **Start State:** $q_0$
- **Accept State ($F$):** $\{ q_{12} \}$ (or `q_accept`)

### 3. Execution Phases
1. **Phase 1: Center Detection (Even-Length & Midpoint Partitioning):**
   - Mark leftmost symbol (`0` $\to X$, `1` $\to Y$).
   - Scan right to the last unmarked symbol, marking it (`0` $\to A$, `1` $\to B$).
   - Scan left to the next unmarked symbol and repeat.
   - If an odd symbol has no partner $\implies$ halt without accepting (**REJECT**).
   - When all symbols are paired, the first half consists of $X, Y$ and the second half consists of $A, B$.
   - Rewind to tape start, restoring $X \to 0$ and $Y \to 1$. Tape is now $w$ followed by second-half marked $w$ ($A, B$).
2. **Phase 2: Character-by-Character Matching:**
   - Read first unprocessed symbol from first half:
     - If `0`: mark as $X$, scan right past first half to verify the first unprocessed second-half symbol is $A$. Mark $A \to X$.
     - If `1`: mark as $Y$, scan right past first half to verify the first unprocessed second-half symbol is $B$. Mark $B \to Y$.
   - Return left to the next unprocessed first-half symbol.
   - Repeat until all symbols in the first half are processed.
3. **Phase 3: Final Acceptance:**
   - Verify all second-half symbols have been matched (only $X, Y$ remain before blank $\square$).
   - Transition to $q_{12}$ (`q_accept`) $\implies$ **ACCEPT**.

### 4. Step-by-Step Configuration Trace for `0101`
$$\begin{array}{ccllc}
\hline
\textbf{Step} & \textbf{State} & \textbf{Action} & \textbf{Tape State} & \textbf{Accept?} \\
\hline
0 & q_0 & \text{Read '0'} \to \text{write 'X', move R} & \text{[?0101?]} & \\
1\text{--}4 & q_{\text{find\_end}} & \text{Move right to end of tape} & \text{[?X101?]} & \\
5 & q_{\text{mark\_right}} & \text{Read '1'} \to \text{write 'B', move L} & \text{[?X10B?]} & \\
6\text{--}8 & q_{\text{return\_left}} & \text{Move left back to 'X'} & \text{[?X10B?]} & \\
9 & q_{\text{next\_left}} & \text{Read '1'} \to \text{write 'Y', move R} & \text{[?XY0B?]} & \\
10\text{--}11 & q_{\text{find\_end}} & \text{Move right until 'B'} & \text{[?XY0B?]} & \\
12 & q_{\text{mark\_right}} & \text{Read '0'} \to \text{write 'A', move L} & \text{[?XYAB?]} & \\
13\text{--}14 & q_{\text{return\_left}} & \text{Move left to 'Y'} & \text{[?XYAB?]} & \\
15 & q_{\text{next\_left}} & \text{Sees 'A'} \implies \textbf{Midpoint Found!} & \text{[?XYAB?]} & \\
16\text{--}18 & q_{\text{rewind}} & \text{Restore } Y \to 1, X \to 0 & \text{[?01AB?]} & \\
19\text{--}25 & q_{\text{match\_0}} & \text{Match 1st '0' with 'A' } \implies \text{mark both X} & \text{[?X1XB?]} & \\
26\text{--}36 & q_{\text{match\_1}} & \text{Match 2nd '1' with 'B' } \implies \text{mark both Y} & \text{[?XYXY?]} & \\
37 & q_{\text{accept}} & \text{Hits } \square \text{ with all matched} & \text{[?XYXY??]} & \textbf{YES} \\
\hline
\end{array}$$

### 5. Test Suite Execution Evidence

| Input String | Length | $w$ | Expected | JFLAP TM Result | Final Tape | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `00` | 2 | `0` | ACCEPT | **ACCEPTED** | `?XX??` |  PASS |
| `11` | 2 | `1` | ACCEPT | **ACCEPTED** | `?YY??` |  PASS |
| `0101` | 4 | `01` | ACCEPT | **ACCEPTED** | `?XYXY??` |  PASS |
| `1010` | 4 | `10` | ACCEPT | **ACCEPTED** | `?YXYX??` |  PASS |
| `110110` | 6 | `110` | ACCEPT | **ACCEPTED** | `?YYXYYX??` |  PASS |
| `001001` | 6 | `001` | ACCEPT | **ACCEPTED** | `?XXYXXY??` |  PASS |
| `0` | 1 | - | REJECT | **REJECTED** | `X?` (Odd length) |  PASS |
| `1` | 1 | - | REJECT | **REJECTED** | `Y?` (Odd length) |  PASS |
| `01` | 2 | - | REJECT | **REJECTED** | `?XB?` ($0 \ne 1$) |  PASS |
| `0110` | 4 | - | REJECT | **REJECTED** | `?X1BA?` ($ww^R$) |  PASS |
| `1001` | 4 | - | REJECT | **REJECTED** | `?Y0AB?` ($ww^R$) |  PASS |
| `010` | 3 | - | REJECT | **REJECTED** | `XYA?` (Odd length) |  PASS |
| `110` | 3 | - | REJECT | **REJECTED** | `YYA?` (Odd length) |  PASS |
| `01011` | 5 | - | REJECT | **REJECTED** | `XYXBB?` (Odd length)|  PASS |

---

## Instructions for Opening in Desktop JFLAP (GUI)

If your instructor or lab submission portal requires visual screenshots of the canvas:
1. Ensure Java is installed on your machine.
2. From the repository directory, open any model in the official JFLAP desktop window:
   ```bash
   # Open Question 1 CFG
   jflap gui C:\Users\mridu\jflap-assignment\q1_cfg_wwR.jff

   # Open Question 2 PDA
   jflap gui C:\Users\mridu\jflap-assignment\q2_pda_brackets.jff

   # Open Question 3 PDA
   jflap gui C:\Users\mridu\jflap-assignment\q3_pda_equal_ab.jff

   # Open Question 4 Turing Machine
   jflap gui C:\Users\mridu\jflap-assignment\q4_tm_ww.jff
   ```
3. In JFLAP:
   - Navigate to **Input > Multiple Run** (or **Step with State**).
   - Type in the test strings listed above and click **Run Inputs** to see the green **Accept** and red **Reject** labels on screen.
