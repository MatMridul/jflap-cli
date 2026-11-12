#!/usr/bin/env python3
"""
JFLAP 7.1 CLI Controller
Unified command-line interface to interact with, control, simulate, convert,
and analyze JFLAP 7.1 models headlessly and visually.
"""

import sys
import os
import json
import argparse
import subprocess
from typing import List, Dict, Any
import jflap_builder

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
JAR_PATH = os.path.join(BASE_DIR, "JFLAP7.1.jar")
BRIDGE_CLASS = "JFLAPBridge"

def ensure_environment():
    """Ensure JFLAP7.1.jar is present and JFLAPBridge.java is compiled."""
    if not os.path.exists(JAR_PATH):
        print(f"[jflap] Downloading JFLAP7.1.jar from official site...", file=sys.stderr)
        import urllib.request
        url = "https://www.jflap.org/jflaptmp/july27-18/JFLAP7.1.jar"
        urllib.request.urlretrieve(url, JAR_PATH)
        print(f"[jflap] Downloaded JFLAP7.1.jar successfully.", file=sys.stderr)
        
    class_path = os.path.join(BASE_DIR, "JFLAPBridge.class")
    java_path = os.path.join(BASE_DIR, "JFLAPBridge.java")
    if not os.path.exists(class_path) or (os.path.exists(java_path) and os.path.getmtime(java_path) > os.path.getmtime(class_path)):
        cp = f"{BASE_DIR};{JAR_PATH}" if os.name == "nt" else f"{BASE_DIR}:{JAR_PATH}"
        subprocess.run(["javac", "-cp", cp, java_path], check=True)

def run_bridge(cmd: str, *args) -> Dict[str, Any]:
    """Execute command on JFLAPBridge and parse JSON output."""
    ensure_environment()
    cp = f"{BASE_DIR};{JAR_PATH}" if os.name == "nt" else f"{BASE_DIR}:{JAR_PATH}"
    cmd_args = ["java", "-Djava.awt.headless=true", "-cp", cp, BRIDGE_CLASS, cmd, *args]
    try:
        proc = subprocess.run(cmd_args, capture_output=True, text=True, check=False)
        out = proc.stdout.strip()
        if not out:
            return {"error": proc.stderr.strip() or "No output from JFLAPBridge"}
        
        # In case there are warning lines before JSON, find first '{'
        idx = out.find("{")
        if idx != -1:
            json_text = out[idx:]
            return json.loads(json_text)
        return {"raw_output": out, "stderr": proc.stderr}
    except Exception as e:
        return {"error": str(e)}

def print_table(headers: List[str], rows: List[List[Any]]):
    """Simple ASCII table formatter."""
    if not rows:
        print("(None)")
        return
    col_widths = [len(h) for h in headers]
    for row in rows:
        for i, val in enumerate(row):
            col_widths[i] = max(col_widths[i], len(str(val)))
            
    header_line = " | ".join(h.ljust(col_widths[i]) for i, h in enumerate(headers))
    sep_line = "-+-".join("-" * col_widths[i] for i in range(len(headers)))
    print(header_line)
    print(sep_line)
    for row in rows:
        print(" | ".join(str(val).ljust(col_widths[i]) for i, val in enumerate(row)))

# ---------------------------------------------------------------------------
# Command Handlers
# ---------------------------------------------------------------------------

def handle_inspect(args):
    data = run_bridge("inspect", args.file)
    if args.json:
        print(json.dumps(data, indent=2))
        return
    if "error" in data:
        print(f"Error: {data['error']}", file=sys.stderr)
        sys.exit(1)

    print(f"\n=======================================================")
    print(f" JFLAP Model Inspection: {os.path.basename(data.get('file', ''))}")
    print(f"=======================================================")
    print(f"Category : {data.get('category', '').upper()}")
    print(f"Type     : {data.get('type', data.get('grammarType', 'N/A')).upper()}")
    
    if data.get("category") == "automaton":
        print(f"Deterministic : {'YES' if data.get('isDeterministic') else 'NO (Nondeterministic)'}")
        print(f"Initial State : q{data.get('initialState')}")
        finals = [f"q{s}" for s in data.get('finalStates', [])]
        print(f"Final States  : {', '.join(finals) if finals else 'None'}\n")
        
        print("States:")
        state_rows = []
        for s in data.get("states", []):
            flags = []
            if s.get("initial"): flags.append("INITIAL")
            if s.get("final"): flags.append("FINAL")
            state_rows.append([f"q{s['id']}", s.get("name", ""), f"({s.get('x')}, {s.get('y')})", ", ".join(flags) or "-"])
        print_table(["State", "Name", "Coordinates", "Flags"], state_rows)
        
        print("\nTransitions:")
        trans_rows = []
        for t in data.get("transitions", []):
            from_st = f"q{t['from']}"
            to_st = f"q{t['to']}"
            if "write" in t: # Turing
                lbl = f"read: '{t.get('read') or '□'}', write: '{t.get('write') or '□'}', dir: {t.get('direction')}"
            elif "pop" in t: # PDA
                lbl = f"read: '{t.get('read') or 'λ'}', pop: '{t.get('pop') or 'λ'}', push: '{t.get('push') or 'λ'}'"
            elif "output" in t: # Mealy
                lbl = f"read: '{t.get('read')}', output: '{t.get('output')}'"
            else:
                lbl = f"read: '{t.get('read', t.get('label', 'λ')) or 'λ'}'"
            trans_rows.append([from_st, "->", to_st, lbl])
        print_table(["From", "", "To", "Transition Rule"], trans_rows)

    elif data.get("category") == "grammar":
        print(f"Start Variable : {data.get('startVariable')}")
        print(f"Grammar Type   : {data.get('grammarType')}\n")
        print("Productions:")
        prod_rows = []
        for p in data.get("productions", []):
            rhs = p.get('right')
            prod_rows.append([p.get('left'), "->", rhs if rhs else "λ"])
        print_table(["LHS", "", "RHS"], prod_rows)

def handle_simulate(args):
    res = run_bridge("simulate", args.file, *args.inputs)
    if args.json:
        print(json.dumps(res, indent=2))
        return
    if "error" in res:
        print(f"Error: {res['error']}", file=sys.stderr)
        sys.exit(1)

    print(f"\nSimulation Results for: {os.path.basename(res.get('file', ''))}")
    rows = []
    has_output = any("output" in r or "finalTape" in r for r in res.get("results", []))
    for r in res.get("results", []):
        inp = r.get("input")
        inp_str = f"'{inp}'" if inp != "" else "λ (empty)"
        status = "ACCEPTED" if r.get("accepted") else "REJECTED"
        extra = r.get("output", r.get("finalTape", "-"))
        if has_output:
            rows.append([inp_str, status, extra])
        else:
            rows.append([inp_str, status])
            
    headers = ["Input String", "Result"] + (["Output / Tape"] if has_output else [])
    print_table(headers, rows)

def handle_trace(args):
    res = run_bridge("trace", args.file, args.input)
    if args.json:
        print(json.dumps(res, indent=2))
        return
    if "error" in res:
        print(f"Error: {res['error']}", file=sys.stderr)
        sys.exit(1)

    inp = res.get("input", "")
    print(f"\nExecution Trace for Input: '{inp}' on {os.path.basename(res.get('file', ''))}")
    print(f"Final Outcome: {'ACCEPTED' if res.get('accepted') else 'REJECTED'}\n")
    
    rows = []
    has_stack = any("stack" in s for s in res.get("steps", []))
    has_tape = any("tape" in s for s in res.get("steps", []))
    for s in res.get("steps", []):
        step_num = str(s.get("step"))
        st = f"q{s.get('state')}"
        rem = f"'{s.get('unprocessedInput', '')}'"
        acc = "YES" if s.get("isAccept") else ""
        row = [step_num, st, rem]
        if has_stack:
            row.append(f"[{s.get('stack', '')}]")
        if has_tape:
            row.append(f"[{s.get('tape', '')}]")
        row.append(acc)
        rows.append(row)
        
    headers = ["Step", "State", "Remaining Input"]
    if has_stack: headers.append("Stack")
    if has_tape: headers.append("Tape")
    headers.append("Accept?")
    print_table(headers, rows)

def handle_fsa(args):
    sub = args.fsa_command
    if sub == "nfa-to-dfa":
        res = run_bridge("nfa_to_dfa", args.input_file, args.output_file)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Converted NFA ({res.get('originalStates')} states) to DFA ({res.get('dfaStates')} states). Saved to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub == "minimize":
        res = run_bridge("minimize", args.input_file, args.output_file)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Minimized DFA from {res.get('originalStates')} to {res.get('minimizedStates')} states ({res.get('minimizedTransitions')} transitions). Saved to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub in ("equals", "equivalent"):
        res = run_bridge("fsa_equals", args.file1, args.file2)
        if args.json:
            print(json.dumps(res, indent=2))
        elif "equivalent" in res:
            eq = res.get("equivalent")
            msg = "ARE EQUIVALENT (accept the exact same language)" if eq else "ARE NOT EQUIVALENT"
            print(f"Result: The two automata {msg}.")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub == "combine":
        res = run_bridge("combine", args.file1, args.file2, args.output_file)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Combined automata into {res.get('totalStates')} states, {res.get('totalTransitions')} transitions. Saved to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)

def handle_grammar(args):
    sub = args.grammar_command
    if sub == "parse":
        res = run_bridge("grammar_parse", args.file, args.input)
        if args.json:
            print(json.dumps(res, indent=2))
        elif "accepted" in res:
            status = "ACCEPTED" if res.get("accepted") else "REJECTED"
            print(f"Parsing '{args.input}' via {res.get('parser')}: {status}")
            if res.get("derivation"):
                print("Derivation Trace:")
                for step in res.get("derivation"):
                    print(f"  {step}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub == "clean":
        res = run_bridge("grammar_clean", args.input_file, args.output_file)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Cleaned grammar (lambda/unit/useless removed): {res.get('originalProductions')} -> {res.get('cleanedProductions')} productions. Saved to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub == "first-follow":
        res = run_bridge("first_follow", args.file)
        if args.json:
            print(json.dumps(res, indent=2))
        elif "first" in res:
            print(f"\nGrammar Parsing Analysis for: {os.path.basename(args.file)}")
            print(f"Is LL(1)? : {'YES' if res.get('isLL1') else 'NO'}\n")
            f_rows = [[k, ", ".join(sorted(v))] for k, v in res.get("first", {}).items()]
            print("FIRST Sets:")
            print_table(["Symbol", "FIRST Set"], f_rows)
            print("\nFOLLOW Sets:")
            fl_rows = [[k, ", ".join(sorted(v))] for k, v in res.get("follow", {}).items()]
            print_table(["Variable", "FOLLOW Set"], fl_rows)
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)

def handle_convert(args):
    sub = args.convert_command
    if sub == "fsa-to-re":
        cmd_args = [args.input_file]
        if args.output_file:
            cmd_args.append(args.output_file)
        res = run_bridge("fsa_to_re", *cmd_args)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Regular Expression: {res.get('regularExpression')}")
            if res.get("outputFile"):
                print(f"Saved RE to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub == "fsa-to-rg":
        res = run_bridge("fsa_to_rg", args.input_file, args.output_file)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Converted FSA to Regular Grammar ({res.get('productions')} productions). Saved to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub == "rg-to-fsa":
        res = run_bridge("rg_to_fsa", args.input_file, args.output_file)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Converted Regular Grammar to FSA ({res.get('states')} states, {res.get('transitions')} transitions). Saved to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub == "cfg-to-pda":
        mode = getattr(args, "mode", "ll")
        res = run_bridge("cfg_to_pda", args.input_file, args.output_file, mode)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Converted CFG to PDA [{mode.upper()} mode] ({res.get('states')} states, {res.get('transitions')} transitions). Saved to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)
    elif sub == "pda-to-cfg":
        res = run_bridge("pda_to_cfg", args.input_file, args.output_file)
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"Converted PDA to Context-Free Grammar ({res.get('productions')} productions). Saved to: {res.get('outputFile')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)

def handle_dot(args):
    res = run_bridge("to_dot", args.file)
    if "error" in res:
        print(f"Error: {res['error']}", file=sys.stderr)
        sys.exit(1)
    dot_str = res.get("dot", "")
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(dot_str)
        print(f"Exported Graphviz DOT file to: {args.output}")
    else:
        print(dot_str)

def handle_create(args):
    if args.from_json:
        with open(args.from_json, "r", encoding="utf-8") as f:
            data = json.load(f)
        out_path = args.output or data.get("output", "automaton.jff")
        jflap_builder.build_from_dict(data, out_path)
        print(f"Created JFLAP model from JSON definition at: {out_path}")
        return

    mtype = args.model_type
    out_path = args.output or f"{mtype}.jff"
    if mtype == "fa":
        states = [s.strip() for s in args.states.split(",")]
        finals = [s.strip() for s in args.finals.split(",")] if args.finals else []
        trans = []
        if args.transitions:
            for item in args.transitions.split(";"):
                item = item.strip()
                if not item: continue
                parts = [p.strip() for p in item.split(",")]
                if len(parts) >= 3:
                    for sym in parts[2:]:
                        trans.append({"from": parts[0], "to": parts[1], "read": sym})
        jflap_builder.build_fa(states, args.initial, finals, trans, out_path)
        print(f"Created Finite Automaton at: {out_path}")

    elif mtype == "pda":
        states = [s.strip() for s in args.states.split(",")]
        finals = [s.strip() for s in args.finals.split(",")] if args.finals else []
        trans = []
        if args.transitions:
            for item in args.transitions.split(";"):
                item = item.strip()
                if not item: continue
                parts = [p.strip() for p in item.split(",")]
                if len(parts) >= 5:
                    trans.append({"from": parts[0], "to": parts[1], "read": parts[2], "pop": parts[3], "push": parts[4]})
        jflap_builder.build_pda(states, args.initial, finals, trans, out_path)
        print(f"Created Pushdown Automaton at: {out_path}")

    elif mtype == "grammar":
        prods = [p.strip() for p in args.productions.split(",")]
        jflap_builder.build_grammar(prods, args.start, out_path)
        print(f"Created Grammar at: {out_path}")

def handle_lsystem(args):
    sub = args.lsystem_command
    if sub == "expand":
        res = run_bridge("lsystem_expand", args.file, str(args.level))
        if args.json:
            print(json.dumps(res, indent=2))
        elif res.get("success"):
            print(f"\nL-System Expansion [Level {res.get('level')}]:")
            print(f"Length: {res.get('length')} symbols")
            print(f"String: {res.get('expandedString')}")
        else:
            print(f"Error: {res.get('error')}", file=sys.stderr)

def handle_gui(args):
    """Launch official JFLAP 7.1 GUI."""
    cmd = ["java", "-jar", JAR_PATH]
    if args.file:
        cmd.append(args.file)
    print(f"Launching JFLAP 7.1 GUI: {' '.join(cmd)}")
    subprocess.Popen(cmd)

def handle_mcp(args):
    """Launch Model Context Protocol (MCP) server."""
    import jflap_mcp
    jflap_mcp.main()

# ---------------------------------------------------------------------------
# CLI Argument Parser Setup
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        prog="jflap",
        description="JFLAP 7.1 CLI Controller & Automation Suite"
    )
    subparsers = parser.add_subparsers(dest="command")

    # inspect
    p_insp = subparsers.add_parser("inspect", help="Inspect a JFLAP .jff file")
    p_insp.add_argument("file", help="Path to .jff file")
    p_insp.add_argument("--json", action="store_true", help="Output raw JSON")
    p_insp.set_defaults(func=handle_inspect)

    # simulate
    p_sim = subparsers.add_parser("simulate", help="Simulate input strings against an automaton")
    p_sim.add_argument("file", help="Path to .jff file")
    p_sim.add_argument("inputs", nargs="*", default=[""], help="Input strings to simulate")
    p_sim.add_argument("--json", action="store_true", help="Output raw JSON")
    p_sim.set_defaults(func=handle_simulate)

    # trace
    p_tr = subparsers.add_parser("trace", help="Get step-by-step configuration execution trace")
    p_tr.add_argument("file", help="Path to .jff file")
    p_tr.add_argument("input", nargs="?", default="", help="Input string to trace")
    p_tr.add_argument("--json", action="store_true", help="Output raw JSON")
    p_tr.set_defaults(func=handle_trace)

    # fsa operations
    p_fsa = subparsers.add_parser("fsa", help="Finite State Automata operations (NFA->DFA, minimize, equals)")
    fsa_subs = p_fsa.add_subparsers(dest="fsa_command")
    
    p_n2d = fsa_subs.add_parser("nfa-to-dfa", help="Convert NFA to equivalent DFA")
    p_n2d.add_argument("input_file", help="Input NFA .jff")
    p_n2d.add_argument("output_file", help="Output DFA .jff")
    p_n2d.add_argument("--json", action="store_true")
    
    p_min = fsa_subs.add_parser("minimize", help="Minimize a DFA")
    p_min.add_argument("input_file", help="Input DFA .jff")
    p_min.add_argument("output_file", help="Output Minimized DFA .jff")
    p_min.add_argument("--json", action="store_true")
    
    p_eq = fsa_subs.add_parser("equals", help="Check language equivalence between two FSAs")
    p_eq.add_argument("file1", help="First .jff file")
    p_eq.add_argument("file2", help="Second .jff file")
    p_eq.add_argument("--json", action="store_true")

    p_comb = fsa_subs.add_parser("combine", help="Combine two automata into one")
    p_comb.add_argument("file1", help="First .jff file")
    p_comb.add_argument("file2", help="Second .jff file")
    p_comb.add_argument("output_file", help="Destination combined .jff file")
    p_comb.add_argument("--json", action="store_true")
    p_fsa.set_defaults(func=handle_fsa)

    # grammar operations
    p_g = subparsers.add_parser("grammar", help="Grammar operations (parse, clean, first-follow)")
    g_subs = p_g.add_subparsers(dest="grammar_command")
    
    p_gp = g_subs.add_parser("parse", help="Parse input string with CYK or Brute-force parser")
    p_gp.add_argument("file", help="Path to grammar .jff")
    p_gp.add_argument("input", help="String to parse")
    p_gp.add_argument("--json", action="store_true")
    
    p_gc = g_subs.add_parser("clean", help="Remove lambda, unit, and useless productions")
    p_gc.add_argument("input_file", help="Input grammar .jff")
    p_gc.add_argument("output_file", help="Output cleaned grammar .jff")
    p_gc.add_argument("--json", action="store_true")

    p_ff = g_subs.add_parser("first-follow", help="Compute FIRST and FOLLOW sets, test LL(1)")
    p_ff.add_argument("file", help="Path to grammar .jff")
    p_ff.add_argument("--json", action="store_true")
    p_g.set_defaults(func=handle_grammar)

    # convert operations
    p_conv = subparsers.add_parser("convert", help="Convert between formal models")
    conv_subs = p_conv.add_subparsers(dest="convert_command")

    p_re = conv_subs.add_parser("fsa-to-re", help="Convert FSA to Regular Expression")
    p_re.add_argument("input_file", help="Input FSA .jff")
    p_re.add_argument("output_file", nargs="?", help="Optional output .jff or .re file")
    p_re.add_argument("--json", action="store_true")
    
    p_c1 = conv_subs.add_parser("fsa-to-rg", help="Convert FSA to Regular Grammar")
    p_c1.add_argument("input_file", help="Input FSA .jff")
    p_c1.add_argument("output_file", help="Output Regular Grammar .jff")
    p_c1.add_argument("--json", action="store_true")
    
    p_c2 = conv_subs.add_parser("rg-to-fsa", help="Convert Regular Grammar to FSA")
    p_c2.add_argument("input_file", help="Input Regular Grammar .jff")
    p_c2.add_argument("output_file", help="Output FSA .jff")
    p_c2.add_argument("--json", action="store_true")
    
    p_c3 = conv_subs.add_parser("cfg-to-pda", help="Convert CFG to PDA")
    p_c3.add_argument("input_file", help="Input CFG .jff")
    p_c3.add_argument("output_file", help="Output PDA .jff")
    p_c3.add_argument("--mode", choices=["ll", "lr"], default="ll", help="Parsing algorithm (default: ll)")
    p_c3.add_argument("--json", action="store_true")
    
    p_c4 = conv_subs.add_parser("pda-to-cfg", help="Convert PDA to CFG")
    p_c4.add_argument("input_file", help="Input PDA .jff")
    p_c4.add_argument("output_file", help="Output CFG .jff")
    p_c4.add_argument("--json", action="store_true")
    p_conv.set_defaults(func=handle_convert)

    # dot export
    p_dot = subparsers.add_parser("dot", help="Export automaton to Graphviz DOT format")
    p_dot.add_argument("file", help="Input .jff file")
    p_dot.add_argument("-o", "--output", help="Output .dot file")
    p_dot.set_defaults(func=handle_dot)

    # create model
    p_cr = subparsers.add_parser("create", help="Create a JFLAP .jff model file")
    p_cr.add_argument("model_type", nargs="?", choices=["fa", "pda", "grammar"], help="Type of model")
    p_cr.add_argument("--states", help="Comma-separated state names (e.g. q0,q1,q2)")
    p_cr.add_argument("--initial", default="q0", help="Initial state")
    p_cr.add_argument("--finals", help="Comma-separated final states (e.g. q2)")
    p_cr.add_argument("--transitions", help="Semicolon-separated transition rules")
    p_cr.add_argument("--start", default="S", help="Start variable for grammar")
    p_cr.add_argument("--productions", help="Comma-separated productions (e.g. 'S->aSb, S->ab')")
    p_cr.add_argument("--from-json", help="Build model from JSON specification file")
    p_cr.add_argument("-o", "--output", help="Output .jff file path")
    p_cr.set_defaults(func=handle_create)

    # lsystem
    p_ls = subparsers.add_parser("lsystem", help="L-System operations")
    ls_subs = p_ls.add_subparsers(dest="lsystem_command")
    p_lse = ls_subs.add_parser("expand", help="Expand an L-System to n levels")
    p_lse.add_argument("file", help="Path to L-System .jff")
    p_lse.add_argument("level", type=int, nargs="?", default=3, help="Expansion level (default: 3)")
    p_lse.add_argument("--json", action="store_true")
    p_ls.set_defaults(func=handle_lsystem)

    # gui launcher
    p_gui = subparsers.add_parser("gui", help="Launch official JFLAP 7.1 GUI window")
    p_gui.add_argument("file", nargs="?", help="Optional file to open in JFLAP")
    p_gui.set_defaults(func=handle_gui)

    # mcp server
    p_mcp = subparsers.add_parser("mcp", help="Run as Model Context Protocol (MCP) server")
    p_mcp.set_defaults(func=handle_mcp)

    args = parser.parse_args()
    if not hasattr(args, "func"):
        parser.print_help()
        sys.exit(1)
    args.func(args)

if __name__ == "__main__":
    main()
