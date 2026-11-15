# SRM Institute of Science and Technology
### Department of Computational Intelligence / Computer Science & Engineering
## 21CSC301T — Formal Language and Automata Theory
### JFLAP Practical Assessment — SET NO. 14 (Units 3–5)

| Student Information | Academic & Assessment Details |
| :--- | :--- |
| **Student Name:** Mridul Mathur | **Course Code:** 21CSC301T |
| **Registration / Roll No.:** RA2411026010137 | **Course Title:** Formal Language and Automata Theory |
| **Section:** AD1 | **Assigned Set Number:** **SET NO. 14** |
| **Assessment Type:** Practical / Lab Evaluation | **Total Evaluation Marks:** 20 Marks (4 Questions × 5 Marks) |

---

### Assessment Criteria Compliance Checklist

| Criterion No. | Evaluation Requirement | Implementation Evidence in this Submission | Status |
| :---: | :--- | :--- | :---: |
| **1** | Answer all four questions using JFLAP ($4 \times 5 = 20$ marks). | Complete solutions provided for Q1 (CFG), Q2 (PDA), Q3 (PDA), and Q4 (TM). | **COMPLIANT** |
| **2** | Construct required CFG, PDA, or TM and demonstrate execution in JFLAP. | JFLAP 7.1 XML (`.jff`) models constructed, validated, and executed via JFLAP engine. | **COMPLIANT** |
| **3** | Test at least three accepted and three rejected strings for language recognition. | Tested between 8 and 14 strings (at least 4 accepted and 4 rejected) for every question. | **COMPLIANT** |
| **4** | Submit JFLAP `.jff` files and execution evidence (simulation traces). | Standalone `.jff` files packaged in submission ZIP; detailed step-by-step traces included. | **COMPLIANT** |
| **5** | $\epsilon$ denotes empty string; $n, m$ follow bounds specified. | $\epsilon$ / $\lambda$ conventions strictly verified; boundary cases ($w=\epsilon$, zero counts) tested. | **COMPLIANT** |

---

## Index of Files & Deliverables (Set No. 14)

All models have been created, inspected, and verified against JFLAP 7.1:

| Question (Set 14) | Topic | Model Type | Submission File | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Q1 (U3)** | $\{w w^R \mid w \in \{0, 1\}^*\}$, parse `1001` | CFG | [`q1_cfg_wwR.jff`](./q1_cfg_wwR.jff) |  Verified |
| **Q2 (U4)** | Nested `()`, `[]`, `{}` Brackets | PDA | [`q2_pda_brackets.jff`](./q2_pda_brackets.jff) |  Verified |
| **Q3 (U4)** | Equal $a$'s and $b$'s in arbitrary order | PDA | [`q3_pda_equal_ab.jff`](./q3_pda_equal_ab.jff) |  Verified |
| **Q4 (U5)** | Deciding $\{w w \mid w \in \{0, 1\}^*\}$ | Turing Machine | [`q4_tm_ww.jff`](./q4_tm_ww.jff) |  Verified |

---

## Set No. 14 — Question 1 (Unit 3): CFG for Even-Length Palindromes & Parsing `1001`

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

### 4. Visual Parse Tree Representation
```
            S
          / | \
         1  S  1
          / | \
         0  S  0
            |
            λ

Yield of Tree: 1 · 0 · λ · 0 · 1 = 1001
```

### 5. JFLAP Grammar Parser Execution Evidence
```text
================================================================================
JFLAP 7.1 Grammar Parser Execution Trace
Target String : "1001"
Parser Engine : BruteForceParser / CYK
Grammar File  : q1_cfg_wwR.jff
Derivation Trace:
  [Step 0] S                  (Start variable)
  [Step 1] 1 S 1              (Apply production S -> 1S1)
  [Step 2] 1 0 S 0 1          (Apply production S -> 0S0)
  [Step 3] 1 0 λ 0 1          (Apply production S -> λ)
  [Result] 1001               ==> ACCEPTED (Yield matches target input string)
================================================================================
```

### 6. Test Suite Execution Evidence (Meets Criterion 3: $\ge 3$ Accepted, $\ge 3$ Rejected)

| Input String | Type / Category | Expected | JFLAP Parser Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| `1001` | Target String ($w=10$) | ACCEPT | **ACCEPTED** |  PASS |
| `00` | Even Palindrome ($w=0$) | ACCEPT | **ACCEPTED** |  PASS |
| `11` | Even Palindrome ($w=1$) | ACCEPT | **ACCEPTED** |  PASS |
| `0110` | Even Palindrome ($w=01$) | ACCEPT | **ACCEPTED** |  PASS |
| `110011` | Even Palindrome ($w=110$) | ACCEPT | **ACCEPTED** |  PASS |
| `101` | Odd Palindrome (not in $w w^R$) | REJECT | **REJECTED** |  PASS |
| `1010` | Asymmetric String | REJECT | **REJECTED** |  PASS |
| `110` | Odd Length | REJECT | **REJECTED** |  PASS |
| `010` | Odd Length Palindrome | REJECT | **REJECTED** |  PASS |

---

## Set No. 14 — Question 2 (Unit 4): PDA for Nested `()`, `[]`, and `{}` Bracket Pairs

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
- **Acceptance Mode:** Final State (`final_state`)

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

| Step | Current State | Remaining Input | Stack Contents | Action / Transition Rule |
| :---: | :---: | :---: | :---: | :---|
| **0** | `q0` | `([])` | `[Z]` | Initial configuration with stack bottom marker $Z$ |
| **1** | `q1` | `([])` | `[Z]` | $\delta(q_0, \lambda, Z) \to (q_1, Z)$ |
| **2** | `q1` | `[])` | `[(Z]` | Read `(`, push onto stack |
| **3** | `q1` | `])` | `[[ (Z]` | Read `[`, push onto stack |
| **4** | `q1` | `)` | `[(Z]` | Read `]`, matches top `[` $\implies$ pop `[` |
| **5** | `q1` | $\epsilon$ (empty) | `[Z]` | Read `)`, matches top `(` $\implies$ pop `(` |
| **6** | `q2` | $\epsilon$ (empty) | `[Z]` | $\delta(q_1, \lambda, Z) \to (q_2, Z)$ **[ACCEPTED in Final State]** |

### 5. JFLAP Simulator Console Output
```text
================================================================================
Execution Trace for Input: '([])' on q2_pda_brackets.jff [PDA Acceptance: final_state]
Final Outcome: ACCEPTED

Step | State | Remaining Input | Stack | Accept?
-----+-------+-----------------+-------+--------
0    | q0    | '([])'          | [Z]   |        
1    | q1    | '([])'          | [Z]   |        
2    | q1    | '[])'           | [(Z]  |        
3    | q1    | '])'            | [[(Z] |        
4    | q1    | ')'             | [(Z]  |        
5    | q1    | ''              | [Z]   |        
6    | q2    | ''              | [Z]   | YES    
================================================================================
```

### 6. Test Suite Execution Evidence (Meets Criterion 3: $\ge 3$ Accepted, $\ge 3$ Rejected)

| Input String | Description / Category | Expected | JFLAP PDA Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| `()` | Single parenthesized pair | ACCEPT | **ACCEPTED** |  PASS |
| `([])` | Nested bracket pair | ACCEPT | **ACCEPTED** |  PASS |
| `{[()]}` | All three nested types | ACCEPT | **ACCEPTED** |  PASS |
| `(){}[()]` | Concatenated balanced pairs | ACCEPT | **ACCEPTED** |  PASS |
| `(]` | Mismatched closing delimiter | REJECT | **REJECTED** |  PASS |
| `([)]` | Cross-nested (illegal interleaving) | REJECT | **REJECTED** |  PASS |
| `(((` | Unclosed opening brackets | REJECT | **REJECTED** |  PASS |
| `())` | Excess closing delimiter | REJECT | **REJECTED** |  PASS |
| `}{` | Inverted closing/opening order | REJECT | **REJECTED** |  PASS |

---

## Set No. 14 — Question 3 (Unit 4): PDA for Equal Numbers of $a$ and $b$ in Arbitrary Order

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
- **Acceptance Mode:** Final State (`final_state`)

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

| Step | Current State | Remaining Input | Stack Contents | Action / Transition Rule |
| :---: | :---: | :---: | :---: | :---|
| **0** | `q0` | `baab` | `[Z]` | Initial configuration with stack bottom marker $Z$ |
| **1** | `q1` | `baab` | `[Z]` | $\delta(q_0, \lambda, Z) \to (q_1, Z)$ |
| **2** | `q1` | `aab` | `[bZ]` | Read `b`, push `b` onto stack |
| **3** | `q1` | `ab` | `[Z]` | Read `a`, matches top `b` $\implies$ pop `b` (balanced) |
| **4** | `q1` | `b` | `[aZ]` | Read `a`, push `a` onto stack |
| **5** | `q1` | $\epsilon$ (empty) | `[Z]` | Read `b`, matches top `a` $\implies$ pop `a` (balanced) |
| **6** | `q2` | $\epsilon$ (empty) | `[Z]` | $\delta(q_1, \lambda, Z) \to (q_2, Z)$ **[ACCEPTED in Final State]** |

### 5. JFLAP Simulator Console Output
```text
================================================================================
Execution Trace for Input: 'baab' on q3_pda_equal_ab.jff [PDA Acceptance: final_state]
Final Outcome: ACCEPTED

Step | State | Remaining Input | Stack | Accept?
-----+-------+-----------------+-------+--------
0    | q0    | 'baab'          | [Z]   |        
1    | q1    | 'baab'          | [Z]   |        
2    | q1    | 'aab'           | [bZ]  |        
3    | q1    | 'ab'            | [Z]   |        
4    | q1    | 'b'             | [aZ]  |        
5    | q1    | ''              | [Z]   |        
6    | q2    | ''              | [Z]   | YES    
================================================================================
```

### 6. Test Suite Execution Evidence (Meets Criterion 3: $\ge 3$ Accepted, $\ge 3$ Rejected)

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

## Set No. 14 — Question 4 (Unit 5): Turing Machine Deciding $\{w w \mid w \in \{0, 1\}^*\}$

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
- **Accept State ($F$):** $\{ q_{12} \}$ (`q_accept`)

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

| Step | Current State | Action Performed | Tape Configuration | Accept? |
| :---: | :---: | :---| :---: | :---: |
| **0** | `q0` | Read `0` $\to$ write `X`, move R | `[?0101?]` | |
| **1–4** | `q1` (`q_find_end`) | Scan right over `101` to end of input tape | `[?X101?]` | |
| **5** | `q2` (`q_mark_right`) | Read `1` $\to$ write `B`, move L | `[?X10B?]` | |
| **6–8** | `q3` (`q_return_left`) | Scan left back to marked `X` | `[?X10B?]` | |
| **9** | `q4` (`q_next_left`) | Read `1` $\to$ write `Y`, move R | `[?XY0B?]` | |
| **10–11** | `q1` (`q_find_end`) | Scan right until boundary marker `B` | `[?XY0B?]` | |
| **12** | `q2` (`q_mark_right`) | Read `0` $\to$ write `A`, move L | `[?XYAB?]` | |
| **13–14** | `q3` (`q_return_left`) | Scan left back to `Y` | `[?XYAB?]` | |
| **15** | `q4` (`q_next_left`) | Reads `A` $\implies$ **Midpoint Partitioning Complete!** | `[?XYAB?]` | |
| **16–18** | `q5` (`q_rewind`) | Rewind head to start, restoring `Y` $\to 1$, `X` $\to 0$ | `[?01AB?]` | |
| **19–25** | `q7` (`q_match_0`) | Match 1st symbol `0` with midpoint symbol `A` $\implies$ mark both `X` | `[?X1XB?]` | |
| **26–36** | `q8` (`q_match_1`) | Match 2nd symbol `1` with midpoint symbol `B` $\implies$ mark both `Y` | `[?XYXY?]` | |
| **37** | `q12` (`q_accept`) | Hits blank $\square$ with all symbols verified | `[?XYXY??]` | **ACCEPTED** |

### 5. JFLAP Simulator Console Output
```text
================================================================================
Execution Trace for Input: '0101' on q4_tm_ww.jff
Final Outcome: ACCEPTED (Halted in accepting state q12 after 37 transitions)

Key Transition Sequence:
  [Step  0] State q0            Tape: [?0101?]   --> Mark '0' as X, move R
  [Step  5] State q_mark_right  Tape: [?X10B?]   --> Mark last '1' as B, move L
  [Step  9] State q_next_left   Tape: [?XY0B?]   --> Mark '1' as Y, move R
  [Step 12] State q_mark_right  Tape: [?XYAB?]   --> Mark remaining '0' as A, move L
  [Step 15] State q_next_left   Tape: [?XYAB?]   --> Sees 'A' ==> Midpoint Found!
  [Step 18] State q_rewind      Tape: [?01AB?]   --> Reset first half to original symbols
  [Step 25] State q_match_0     Tape: [?X1XB?]   --> First '0' matches 'A' ==> both marked X
  [Step 36] State q_match_1     Tape: [?XYXY?]   --> Second '1' matches 'B' ==> both marked Y
  [Step 37] State q_accept      Tape: [?XYXY??]  ==> ACCEPTED (End of tape verified)
================================================================================
```

### 6. Test Suite Execution Evidence (Meets Criterion 3: $\ge 3$ Accepted, $\ge 3$ Rejected)

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

If your instructor or lab submission portal requires visual verification on the desktop canvas:
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
