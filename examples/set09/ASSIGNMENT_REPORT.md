# SRM Institute of Science and Technology
### Department of Computational Intelligence / Computer Science & Engineering
## 21CSC301T — Formal Language and Automata Theory
### JFLAP Practical Assessment — SET NO. 09 (Units 3–5)

| Student Information | Academic & Assessment Details |
| :--- | :--- |
| **Student Name:** Anushka Priyani Nayak | **Course Code:** 21CSC301T |
| **Registration / Roll No.:** RA2411026010132 | **Course Title:** Formal Language and Automata Theory |
| **Section:** AD1 | **Assigned Set Number:** **SET NO. 09** |
| **Assessment Type:** Practical / Lab Evaluation | **Total Evaluation Marks:** 20 Marks (4 Questions × 5 Marks) |

---

## Index of Files & Deliverables (Set No. 09)
All models have been created, inspected, and verified against JFLAP 7.1:

| Question (Set 09) | Topic | Model Type | Submission File | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Q1 (U5)** | TM deciding all binary palindromes | Turing Machine | [`q1_tm_all_palindromes.jff`](./q1_tm_all_palindromes.jff) |  Verified |
| **Q2 (U5)** | TM deciding even-length binary palindromes | Turing Machine | [`q2_tm_even_palindromes.jff`](./q2_tm_even_palindromes.jff) |  Verified |
| **Q3 (U3)** | CFG for odd-length palindromes over $\{a,b\}$, parse `ababa` | CFG | [`q3_cfg_odd_palindromes.jff`](./q3_cfg_odd_palindromes.jff) |  Verified |
| **Q4 (U4)** | PDA recognizing odd-length palindromes over $\{a,b\}$ | PDA | [`q4_pda_odd_palindromes.jff`](./q4_pda_odd_palindromes.jff) |  Verified |

---

## Set No. 09 — Question 1 (Unit 5): Turing Machine Deciding All Binary Palindromes

### 1. Problem Statement
Construct a Turing Machine (TM) deciding the language of all binary palindromes $L = \{ w \in \{0, 1\}^* \mid w = w^R \}$, including both even-length and odd-length palindromes. (5 Marks)

### 2. Formal Machine Architecture
A deterministic 1-tape Turing Machine $M = (Q, \Sigma, \Gamma, \delta, q_0, \square, F)$:
- **Input Alphabet ($\Sigma$):** $\{ 0, 1 \}$
- **Tape Alphabet ($\Gamma$):** $\{ 0, 1, \square \}$
- **States ($Q$):** $\{ q_0, q_1, q_2, q_3, q_4, q_5, q_6 \}$
- **Start State:** $q_0$
- **Accept State ($F$):** $\{ q_6 \}$ (`q_accept`)

### 3. Transition Function ($\delta$)
- **Start State ($q_0$):**
  - $\delta(q_0, \square) = (q_6, \square, R)$ (Empty string $\epsilon$ is accepted)
  - $\delta(q_0, 0) = (q_1, \square, R)$ (Consume left `0`, move right to find matching right `0`)
  - $\delta(q_0, 1) = (q_3, \square, R)$ (Consume left `1`, move right to find matching right `1`)
- **Rightward Scanning ($q_1, q_3$):**
  - $\delta(q_1, 0) = (q_1, 0, R); \quad \delta(q_1, 1) = (q_1, 1, R); \quad \delta(q_1, \square) = (q_2, \square, L)$
  - $\delta(q_3, 0) = (q_3, 0, R); \quad \delta(q_3, 1) = (q_3, 1, R); \quad \delta(q_3, \square) = (q_4, \square, L)$
- **Right Symbol Verification ($q_2, q_4$):**
  - $\delta(q_2, 0) = (q_5, \square, L)$ (Matches left `0`, erase and return left)
  - $\delta(q_2, \square) = (q_6, \square, R)$ (Odd-length center! String of length 1 is accepted)
  - $\delta(q_4, 1) = (q_5, \square, L)$ (Matches left `1`, erase and return left)
  - $\delta(q_4, \square) = (q_6, \square, R)$ (Odd-length center! String of length 1 is accepted)
  *(Any mismatch halts with no transition $\implies$ REJECT)*
- **Leftward Rewind ($q_5$):**
  - $\delta(q_5, 0) = (q_5, 0, L); \quad \delta(q_5, 1) = (q_5, 1, L); \quad \delta(q_5, \square) = (q_0, \square, R)$

### 4. Step-by-Step Configuration Trace for `010`
$$\begin{array}{ccllc}
\hline
\textbf{Step} & \textbf{State} & \textbf{Tape State} & \textbf{Head Action} \\
\hline
0 & q_0 & \text{[?010?]} & \text{Read '0'} \to \text{write } \square \text{, move R to index 1} \\
1 & q_1 & \text{[? 10?]} & \text{Read '1'} \to \text{move R to index 2} \\
2 & q_1 & \text{[?  0?]} & \text{Read '0'} \to \text{move R to index 3 (blank)} \\
3 & q_1 & \text{[?   ?]} & \text{Read } \square \to \text{move L to index 2} \\
4 & q_2 & \text{[?  0?]} & \text{Read '0' (matches!)} \to \text{write } \square \text{, move L to index 1} \\
5 & q_5 & \text{[? 1 ?]} & \text{Read '1'} \to \text{move L to index 0 (blank)} \\
6 & q_5 & \text{[?   ?]} & \text{Read } \square \to \text{move R to index 1} \\
7 & q_0 & \text{[? 1 ?]} & \text{Read '1'} \to \text{write } \square \text{, move R to index 2 (blank)} \\
8 & q_3 & \text{[?   ?]} & \text{Read } \square \to \text{move L to index 1 (blank)} \\
9 & q_4 & \text{[?   ?]} & \text{Read } \square \implies \textbf{Center symbol verified! Move R} \\
10 & q_6 & \text{[?   ?]} & \textbf{Final state reached } \implies \textbf{ACCEPT} \\
\hline
\end{array}$$

### 5. JFLAP Simulation Evidence

| Input String | Length | Parity | Expected | JFLAP TM Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `0` | 1 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| `1` | 1 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| `00` | 2 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `11` | 2 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `010` | 3 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| `101` | 3 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| `0110` | 4 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `1001` | 4 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `11011` | 5 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| `01` | 2 | Even | REJECT | **REJECTED** |  PASS |
| `10` | 2 | Even | REJECT | **REJECTED** |  PASS |
| `011` | 3 | Odd | REJECT | **REJECTED** |  PASS |
| `100` | 3 | Odd | REJECT | **REJECTED** |  PASS |
| `0101` | 4 | Even | REJECT | **REJECTED** |  PASS |
| `1100` | 4 | Even | REJECT | **REJECTED** |  PASS |
| `1010` | 4 | Even | REJECT | **REJECTED** |  PASS |

---

## Set No. 09 — Question 2 (Unit 5): Turing Machine Deciding Even-Length Binary Palindromes

### 1. Problem Statement
Construct a Turing Machine (TM) deciding the language of even-length binary palindromes $L = \{ w \in \{0, 1\}^* \mid w = w^R \text{ and } |w| \text{ is even} \} = \{ w w^R \mid w \in \{0, 1\}^* \}$. (5 Marks)

### 2. Formal Machine Architecture & Parity Enforcement
- **Key Difference from Q1:** In Q1, encountering a blank $\square$ on $q_2$ or $q_4$ indicates an odd-length palindrome center and accepts. In Q2, **odd-length palindromes are strictly disallowed**!
- Therefore, the transitions $\delta(q_2, \square) \to q_6$ and $\delta(q_4, \square) \to q_6$ are removed. If an unpaired middle symbol is reached, the TM encounters no transition and halts (**REJECT**).
- Only strings whose symbols are entirely paired and exhausted by $q_0 \xrightarrow{\square} q_6$ enter the final state.

### 3. Transition Function ($\delta$)
- $\delta(q_0, \square) = (q_6, \square, R)$ (Empty string $|w|=0$ is even $\implies$ ACCEPT)
- $\delta(q_0, 0) = (q_1, \square, R)$
- $\delta(q_0, 1) = (q_3, \square, R)$
- $\delta(q_1, 0) = (q_1, 0, R); \quad \delta(q_1, 1) = (q_1, 1, R); \quad \delta(q_1, \square) = (q_2, \square, L)$
- $\delta(q_3, 0) = (q_3, 0, R); \quad \delta(q_3, 1) = (q_3, 1, R); \quad \delta(q_3, \square) = (q_4, \square, L)$
- $\delta(q_2, 0) = (q_5, \square, L)$ *(Strict match only; no $\square$ transition)*
- $\delta(q_4, 1) = (q_5, \square, L)$ *(Strict match only; no $\square$ transition)*
- $\delta(q_5, 0) = (q_5, 0, L); \quad \delta(q_5, 1) = (q_5, 1, L); \quad \delta(q_5, \square) = (q_0, \square, R)$

### 4. Step-by-Step Configuration Trace for `0110`
$$\begin{array}{ccllc}
\hline
\textbf{Step} & \textbf{State} & \textbf{Tape State} & \textbf{Head Action} \\
\hline
0 & q_0 & \text{[?0110?]} & \text{Read '0'} \to \text{write } \square \text{, move R to index 1} \\
1\text{--}3 & q_1 & \text{[? 110?]} & \text{Scan right past '1', '1', '0' to blank at index 4} \\
4 & q_2 & \text{[? 110?]} & \text{Read '0' (matches 1st symbol!)} \to \text{write } \square \text{, move L} \\
5\text{--}7 & q_5 & \text{[? 11 ?]} & \text{Scan left back to blank at index 0} \\
8 & q_0 & \text{[? 11 ?]} & \text{Read '1'} \to \text{write } \square \text{, move R to index 2} \\
9 & q_3 & \text{[?  1 ?]} & \text{Scan right to blank at index 3} \\
10 & q_4 & \text{[?  1 ?]} & \text{Read '1' (matches!)} \to \text{write } \square \text{, move L} \\
11 & q_5 & \text{[?    ?]} & \text{Move left to blank at index 1} \\
12 & q_0 & \text{[?    ?]} & \text{Tape empty! Read } \square \to \text{move R to final state } q_6 \\
13 & q_6 & \text{[?    ?]} & \textbf{Final state reached } \implies \textbf{ACCEPT} \\
\hline
\end{array}$$

### 5. JFLAP Simulation Evidence

| Input String | Length | Parity | Expected | JFLAP TM Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `00` | 2 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `11` | 2 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `0110` | 4 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `1001` | 4 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `0000` | 4 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `1111` | 4 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `010010` | 6 | Even | ACCEPT | **ACCEPTED** |  PASS |
| `0` | 1 | Odd | REJECT | **REJECTED** |  PASS |
| `1` | 1 | Odd | REJECT | **REJECTED** |  PASS |
| `010` | 3 | Odd | REJECT | **REJECTED** |  PASS |
| `101` | 3 | Odd | REJECT | **REJECTED** |  PASS |
| `11011` | 5 | Odd | REJECT | **REJECTED** |  PASS |
| `01` | 2 | Even | REJECT | **REJECTED** |  PASS |
| `10` | 2 | Even | REJECT | **REJECTED** |  PASS |
| `011` | 3 | Odd | REJECT | **REJECTED** |  PASS |
| `10010` | 5 | Odd | REJECT | **REJECTED** |  PASS |

---

## Set No. 09 — Question 3 (Unit 3): CFG for Odd-Length Palindromes & Parsing `ababa`

### 1. Problem Statement
Create a Context-Free Grammar (CFG) for odd-length palindromes over the alphabet $\{a, b\}$ and demonstrate the derivation/parse for `ababa`. (5 Marks)

### 2. Formal Grammar Definition
A CFG is defined as $G = (V, \Sigma, R, S)$:
- **Variables ($V$):** $\{ S \}$
- **Terminals ($\Sigma$):** $\{ a, b \}$
- **Start Variable:** $S$
- **Production Rules ($R$):**
  1. $S \to a S a$
  2. $S \to b S b$
  3. $S \to a$
  4. $S \to b$
*(Note: Unlike even palindromes which end with $S \to \lambda$, odd-length palindromes terminate on the single center symbol $a$ or $b$, guaranteeing an odd length $|w| = 2k + 1$.)*

### 3. Derivation for `ababa`
$$\begin{aligned}
S &\implies a S a && \text{(Apply Rule 1: } S \to aSa\text{)} \\
  &\implies a b S b a && \text{(Apply Rule 2: } S \to bSb\text{)} \\
  &\implies a b a b a && \text{(Apply Rule 3: } S \to a\text{)} \\
  &= ababa && \text{(Target Yield Derived)}
\end{aligned}$$

### 4. JFLAP Simulation Evidence

| String | Length | Parity | Expected | JFLAP Parser Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`ababa`** | 5 | Odd | ACCEPT | **ACCEPTED** *(Parsed via BruteParser)* |  PASS |
| **`a`** | 1 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`b`** | 1 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`aba`** | 3 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`bab`** | 3 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`bbabb`** | 5 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`aa`** | 2 | Even | REJECT | **REJECTED** |  PASS |
| **`bb`** | 2 | Even | REJECT | **REJECTED** |  PASS |
| **`abba`** | 4 | Even | REJECT | **REJECTED** |  PASS |
| **`baab`** | 4 | Even | REJECT | **REJECTED** |  PASS |
| **`ab`** | 2 | Even | REJECT | **REJECTED** |  PASS |
| **`ba`** | 2 | Even | REJECT | **REJECTED** |  PASS |

---

## Set No. 09 — Question 4 (Unit 4): PDA Recognizing Odd-Length Palindromes over $\{a,b\}$

### 1. Problem Statement
Construct a Pushdown Automaton (PDA) recognizing the language of odd-length palindromes $L = \{ w c w^R \mid w \in \{a, b\}^*, c \in \{a, b\} \}$. (5 Marks)

### 2. Formal Machine Architecture
A non-deterministic Pushdown Automaton (NPDA) $M = (Q, \Sigma, \Gamma, \delta, q_0, Z_0, F)$:
- **States ($Q$):** $\{ q_0, q_1, q_2, q_3 \}$
  - $q_0$: Initial state
  - $q_1$: Pushing the first half of the string onto stack
  - $q_2$: Center character detected; popping and matching second half
  - $q_3$: Final accepting state
- **Input Alphabet ($\Sigma$):** $\{ a, b \}$
- **Stack Alphabet ($\Gamma$):** $\{ Z, a, b \}$ ($Z$ marks the bottom of the stack)
- **Start State:** $q_0$
- **Initial Stack Symbol:** $Z$
- **Final States ($F$):** $\{ q_3 \}$

### 3. Transition Function ($\delta$)
- **Initialization:** $\delta(q_0, \lambda, Z) = \{ (q_1, Z) \}$
- **Phase 1: Pushing First Half ($q_1$):**
  - $\delta(q_1, a, X) = \{ (q_1, aX) \}$ for all $X \in \{Z, a, b\}$
  - $\delta(q_1, b, X) = \{ (q_1, bX) \}$ for all $X \in \{Z, a, b\}$
- **Non-Deterministic Center Guess ($q_1 \to q_2$):**
  - Read center symbol $c \in \{a, b\}$ and transition to $q_2$ **without changing stack**:
    - $\delta(q_1, a, X) = \{ (q_2, X) \}$ for all $X \in \{Z, a, b\}$
    - $\delta(q_1, b, X) = \{ (q_2, X) \}$ for all $X \in \{Z, a, b\}$
- **Phase 2: Popping and Matching Second Half ($q_2$):**
  - $\delta(q_2, a, a) = \{ (q_2, \lambda) \}$
  - $\delta(q_2, b, b) = \{ (q_2, \lambda) \}$
- **Acceptance ($q_2 \to q_3$):**
  - $\delta(q_2, \lambda, Z) = \{ (q_3, Z) \}$ (Input empty and stack at $Z \implies$ enter final state $q_3$)

### 4. Step-by-Step Configuration Trace for `ababa`
$$\begin{array}{ccllc}
\hline
\textbf{Step} & \textbf{State} & \textbf{Remaining Input} & \textbf{Stack} & \textbf{Action / Rule} \\
\hline
0 & q_0 & \text{'ababa'} & [Z] & \text{Start configuration} \\
1 & q_1 & \text{'ababa'} & [Z] & \delta(q_0, \lambda, Z) \to (q_1, Z) \\
2 & q_1 & \text{'baba'} & [aZ] & \text{Read 'a', push 'aZ'} \\
3 & q_1 & \text{'aba'} & [baZ] & \text{Read 'b', push 'baZ'} \\
4 & q_2 & \text{'ba'} & [baZ] & \textbf{Guess center 'a'} \implies \text{keep stack, switch to } q_2 \\
5 & q_2 & \text{'a'} & [aZ] & \text{Read 'b', matches top 'b' } \implies \text{pop} \\
6 & q_2 & \text{''} & [Z] & \text{Read 'a', matches top 'a' } \implies \text{pop} \\
7 & q_3 & \text{''} & [Z] & \delta(q_2, \lambda, Z) \to (q_3, Z) \implies \textbf{ACCEPT} \\
\hline
\end{array}$$

### 5. JFLAP Simulation Evidence

| Input String | Length | Parity | Expected | JFLAP PDA Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`a`** | 1 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`b`** | 1 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`aba`** | 3 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`bab`** | 3 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`aaa`** | 3 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`bbb`** | 3 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`ababa`** | 5 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`bbabb`** | 5 | Odd | ACCEPT | **ACCEPTED** |  PASS |
| **`aa`** | 2 | Even | REJECT | **REJECTED** |  PASS |
| **`bb`** | 2 | Even | REJECT | **REJECTED** |  PASS |
| **`abba`** | 4 | Even | REJECT | **REJECTED** |  PASS |
| **`baab`** | 4 | Even | REJECT | **REJECTED** |  PASS |
| **`ab`** | 2 | Even | REJECT | **REJECTED** |  PASS |
| **`ba`** | 2 | Even | REJECT | **REJECTED** |  PASS |
| **`aab`** | 3 | Odd | REJECT | **REJECTED** |  PASS |
| **`abaa`** | 4 | Even | REJECT | **REJECTED** |  PASS |

---

## Instructions for Opening in Desktop JFLAP (GUI)

If visual screenshots of the canvas are required:
```bash
# Open Q1 Turing Machine
jflap gui C:\Users\mridu\jflap-assignment-set09\q1_tm_all_palindromes.jff

# Open Q2 Turing Machine
jflap gui C:\Users\mridu\jflap-assignment-set09\q2_tm_even_palindromes.jff

# Open Q3 CFG
jflap gui C:\Users\mridu\jflap-assignment-set09\q3_cfg_odd_palindromes.jff

# Open Q4 PDA
jflap gui C:\Users\mridu\jflap-assignment-set09\q4_pda_odd_palindromes.jff
```
In the window, go to **Input > Multiple Run**, enter the test strings, and click **Run Inputs** to observe the simulation.
