#!/usr/bin/env python3
"""
JFLAP 7.1 Model Context Protocol (MCP) Server
Standard stdio JSON-RPC 2.0 server allowing LLMs and AI Agents to inspect,
simulate, convert, and control JFLAP 7.1 models programmatically.
"""

import sys
import os
import json
import subprocess
from typing import Dict, Any, List
import jflap_builder
from jflap_bridge_client import run_bridge, run_simulate_batch, JAR_PATH, ensure_environment

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# ---------------------------------------------------------------------------
# MCP Tool Definitions
# ---------------------------------------------------------------------------

TOOLS = [
    {
        "name": "jflap_inspect",
        "description": "Inspect a JFLAP 7.1 (.jff) file. Returns structure metadata, states, initial/final states, transitions, determinism, or grammar productions.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "file": {
                    "type": "string",
                    "description": "Absolute or relative path to the JFLAP .jff file"
                }
            },
            "required": ["file"]
        }
    },
    {
        "name": "jflap_simulate",
        "description": "Simulate one or more input strings on a JFLAP automaton (DFA, NFA, PDA, Turing Machine, Mealy, Moore). Returns accept/reject verdicts and output strings/final tapes. For PDAs, acceptance mode can be explicitly set to 'final_state' or 'empty_stack'.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "file": {
                    "type": "string",
                    "description": "Path to the .jff file"
                },
                "inputs": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "List of input strings to simulate (e.g. ['ab', 'aabb', ''])"
                },
                "acceptance_mode": {
                    "type": "string",
                    "enum": ["final_state", "empty_stack"],
                    "description": "For PDAs: explicit acceptance criterion ('final_state' or 'empty_stack'). Only one criterion is applied."
                }
            },
            "required": ["file", "inputs"]
        }
    },
    {
        "name": "jflap_simulate_batch",
        "description": "High-performance batch simulation of hundreds or thousands of input strings on an automaton in a single call. Returns verdicts[] array.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "model": {
                    "type": "string",
                    "description": "Path to the .jff automaton file"
                },
                "inputs": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "List of input strings to simulate"
                },
                "acceptance_mode": {
                    "type": "string",
                    "enum": ["final_state", "empty_stack"],
                    "description": "For PDAs: explicit acceptance criterion ('final_state' or 'empty_stack')."
                }
            },
            "required": ["model", "inputs"]
        }
    },
    {
        "name": "jflap_trace",
        "description": "Execute a step-by-step configuration trace of an input string on an automaton. Returns the exact trace of states, remaining input, stack contents (for PDA), or tape contents (for Turing machine). For PDAs, acceptance mode can be explicitly set to 'final_state' or 'empty_stack'.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "file": {
                    "type": "string",
                    "description": "Path to the .jff file"
                },
                "input": {
                    "type": "string",
                    "description": "Input string to trace (default empty string)"
                },
                "acceptance_mode": {
                    "type": "string",
                    "enum": ["final_state", "empty_stack"],
                    "description": "For PDAs: explicit acceptance criterion ('final_state' or 'empty_stack')."
                }
            },
            "required": ["file"]
        }
    },
    {
        "name": "jflap_fsa_operation",
        "description": "Perform Finite State Automata transformations: convert NFA to DFA ('nfa_to_dfa'), minimize a DFA ('minimize'), verify language equivalence between two FSAs ('equals'), or combine two automata into one ('combine').",
        "inputSchema": {
            "type": "object",
            "properties": {
                "operation": {
                    "type": "string",
                    "enum": ["nfa_to_dfa", "minimize", "equals", "combine"],
                    "description": "Operation to perform"
                },
                "input_file": {
                    "type": "string",
                    "description": "Primary input .jff file"
                },
                "output_file": {
                    "type": "string",
                    "description": "Destination .jff file (for nfa_to_dfa, minimize, and combine)"
                },
                "file2": {
                    "type": "string",
                    "description": "Second .jff file (required for equals and combine)"
                }
            },
            "required": ["operation", "input_file"]
        }
    },
    {
        "name": "jflap_grammar_operation",
        "description": "Execute grammar operations in JFLAP: parse a string ('parse') using CYK or Brute-force parser, remove lambda, unit, and useless productions ('clean'), or compute FIRST and FOLLOW sets and test LL(1) parseability ('first_follow').",
        "inputSchema": {
            "type": "object",
            "properties": {
                "operation": {
                    "type": "string",
                    "enum": ["parse", "clean", "first_follow"],
                    "description": "Grammar operation to execute"
                },
                "file": {
                    "type": "string",
                    "description": "Path to the grammar .jff file"
                },
                "input": {
                    "type": "string",
                    "description": "Target string to parse (required for 'parse')"
                },
                "output_file": {
                    "type": "string",
                    "description": "Destination .jff file (required for 'clean')"
                }
            },
            "required": ["operation", "file"]
        }
    },
    {
        "name": "jflap_convert_model",
        "description": "Perform cross-formalism conversion: FSA to Regular Expression ('fsa_to_re'), FSA to Regular Grammar ('fsa_to_rg'), Regular Grammar to FSA ('rg_to_fsa'), CFG to PDA ('cfg_to_pda'), or PDA to CFG ('pda_to_cfg').",
        "inputSchema": {
            "type": "object",
            "properties": {
                "conversion": {
                    "type": "string",
                    "enum": ["fsa_to_re", "fsa_to_rg", "rg_to_fsa", "cfg_to_pda", "pda_to_cfg"],
                    "description": "Type of conversion"
                },
                "input_file": {
                    "type": "string",
                    "description": "Input model .jff file"
                },
                "output_file": {
                    "type": "string",
                    "description": "Destination model .jff file (optional for fsa_to_re)"
                },
                "mode": {
                    "type": "string",
                    "enum": ["ll", "lr"],
                    "description": "LL or LR mode for cfg_to_pda (default 'll')"
                }
            },
            "required": ["conversion", "input_file"]
        }
    },
    {
        "name": "jflap_lsystem_expand",
        "description": "Expand an L-System (Lindenmayer system) to a given recursion depth/level.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "file": {
                    "type": "string",
                    "description": "Path to the L-System .jff file"
                },
                "level": {
                    "type": "integer",
                    "description": "Recursion depth / derivation level (default 3)"
                }
            },
            "required": ["file"]
        }
    },
    {
        "name": "jflap_create_model",
        "description": "Programmatically generate a new JFLAP .jff file from a JSON model specification (Finite Automaton, PDA, Turing Machine, or Grammar).",
        "inputSchema": {
            "type": "object",
            "properties": {
                "model": {
                    "type": "object",
                    "description": "Model dictionary containing 'type' ('fa', 'pda', 'turing', 'grammar'), states, initial, finals, transitions/productions"
                },
                "output_file": {
                    "type": "string",
                    "description": "Path where the .jff file will be saved"
                }
            },
            "required": ["model", "output_file"]
        }
    },
    {
        "name": "jflap_export_dot",
        "description": "Export an automaton to Graphviz DOT format for rendering diagrams.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "file": {
                    "type": "string",
                    "description": "Path to the .jff automaton file"
                },
                "output_file": {
                    "type": "string",
                    "description": "Optional destination path for the .dot file"
                }
            },
            "required": ["file"]
        }
    },
    {
        "name": "jflap_export_table",
        "description": "Export a plain JSON transition table (states, alphabet, start_state, accepting_states, transitions) for a DFA/NFA model. Returns a structured error for models that cannot be represented as finite state automata.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "model": {
                    "type": "string",
                    "description": "Path to the .jff automaton file"
                }
            },
            "required": ["model"]
        }
    },
    {
        "name": "jflap_equivalence_check",
        "description": "Verify language equivalence between two automata (modelA, modelB). Returns equivalent boolean, plus the shortest distinguishing string if they differ.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "modelA": {
                    "type": "string",
                    "description": "Path to first .jff automaton file"
                },
                "modelB": {
                    "type": "string",
                    "description": "Path to second .jff automaton file"
                }
            },
            "required": ["modelA", "modelB"]
        }
    },
    {
        "name": "jflap_launch_gui",
        "description": "Launch the interactive JFLAP 7.1 graphical user interface (Swing desktop window), optionally opening a .jff file.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "file": {
                    "type": "string",
                    "description": "Optional path to a .jff file to open upon launch"
                }
            }
        }
    }
]

# ---------------------------------------------------------------------------
# Tool Execution Dispatcher
# ---------------------------------------------------------------------------

def call_tool(name: str, args: Dict[str, Any]) -> Dict[str, Any]:
    try:
        if name == "jflap_inspect":
            f = args["file"]
            res = run_bridge("inspect", f)
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}]}

        elif name == "jflap_simulate":
            f = args["file"]
            inputs = args.get("inputs", [""])
            bridge_args = []
            if args.get("acceptance_mode"):
                bridge_args.extend(["--acceptance", args["acceptance_mode"]])
            bridge_args.extend(inputs)
            res = run_bridge("simulate", f, *bridge_args)
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}]}

        elif name == "jflap_simulate_batch":
            model = args.get("model") or args.get("file")
            inputs = args.get("inputs", [])
            acc_mode = args.get("acceptance_mode")
            res = run_simulate_batch(model, inputs, acc_mode)
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}]}

        elif name == "jflap_trace":
            f = args["file"]
            inp = args.get("input", "")
            bridge_args = [inp]
            if args.get("acceptance_mode"):
                bridge_args.extend(["--acceptance", args["acceptance_mode"]])
            res = run_bridge("trace", f, *bridge_args)
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}]}

        elif name == "jflap_fsa_operation":
            op = args["operation"]
            in_file = args["input_file"]
            if op == "nfa_to_dfa":
                out_file = args.get("output_file", in_file.replace(".jff", "_dfa.jff"))
                res = run_bridge("nfa_to_dfa", in_file, out_file)
            elif op == "minimize":
                out_file = args.get("output_file", in_file.replace(".jff", "_min.jff"))
                res = run_bridge("minimize", in_file, out_file)
            elif op == "equals":
                f2 = args.get("file2")
                if not f2:
                    return {"content": [{"type": "text", "text": "Error: 'file2' is required for 'equals' operation."}], "isError": True}
                res = run_bridge("fsa_equals", in_file, f2)
            elif op == "combine":
                f2 = args.get("file2")
                if not f2:
                    return {"content": [{"type": "text", "text": "Error: 'file2' is required for 'combine' operation."}], "isError": True}
                out_file = args.get("output_file", in_file.replace(".jff", "_combined.jff"))
                res = run_bridge("combine", in_file, f2, out_file)
            else:
                return {"content": [{"type": "text", "text": f"Unknown operation: {op}"}], "isError": True}
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}]}

        elif name == "jflap_grammar_operation":
            op = args["operation"]
            f = args["file"]
            if op == "parse":
                inp = args.get("input", "")
                res = run_bridge("grammar_parse", f, inp)
            elif op == "clean":
                out_file = args.get("output_file", f.replace(".jff", "_clean.jff"))
                res = run_bridge("grammar_clean", f, out_file)
            elif op == "first_follow":
                res = run_bridge("first_follow", f)
            else:
                return {"content": [{"type": "text", "text": f"Unknown grammar operation: {op}"}], "isError": True}
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}]}

        elif name == "jflap_convert_model":
            conv = args["conversion"]
            in_file = args["input_file"]
            out_file = args.get("output_file")
            mode = args.get("mode", "ll")
            if conv == "fsa_to_re":
                res = run_bridge("fsa_to_re", in_file, out_file or "")
            elif conv == "fsa_to_rg":
                if not out_file:
                    out_file = in_file.replace(".jff", "_rg.jff")
                res = run_bridge("fsa_to_rg", in_file, out_file)
            elif conv == "rg_to_fsa":
                if not out_file:
                    out_file = in_file.replace(".jff", "_fsa.jff")
                res = run_bridge("rg_to_fsa", in_file, out_file)
            elif conv == "cfg_to_pda":
                if not out_file:
                    out_file = in_file.replace(".jff", "_pda.jff")
                res = run_bridge("cfg_to_pda", in_file, out_file, mode)
            elif conv == "pda_to_cfg":
                if not out_file:
                    out_file = in_file.replace(".jff", "_cfg.jff")
                res = run_bridge("pda_to_cfg", in_file, out_file)
            else:
                return {"content": [{"type": "text", "text": f"Unknown conversion: {conv}"}], "isError": True}
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}]}

        elif name == "jflap_lsystem_expand":
            f = args["file"]
            lvl = args.get("level", 3)
            res = run_bridge("lsystem_expand", f, str(lvl))
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}]}

        elif name == "jflap_create_model":
            model_dict = args["model"]
            out_file = args["output_file"]
            jflap_builder.build_from_dict(model_dict, out_file)
            insp = run_bridge("inspect", out_file)
            return {"content": [{
                "type": "text",
                "text": f"Successfully created {out_file}.\nModel inspection:\n{json.dumps(insp, indent=2)}"
            }]}

        elif name == "jflap_export_dot":
            f = args["file"]
            res = run_bridge("to_dot", f)
            dot_str = res.get("dot", "")
            if args.get("output_file"):
                with open(args["output_file"], "w", encoding="utf-8") as out_f:
                    out_f.write(dot_str)
                return {"content": [{"type": "text", "text": f"Exported DOT graph to {args['output_file']}"}]}
            return {"content": [{"type": "text", "text": dot_str}]}

        elif name == "jflap_export_table":
            model = args.get("model") or args.get("file")
            res = run_bridge("export_table", model)
            is_err = "error" in res
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}], "isError": is_err}

        elif name == "jflap_equivalence_check":
            mA = args.get("modelA") or args.get("file1")
            mB = args.get("modelB") or args.get("file2")
            res = run_bridge("equivalence_check", mA, mB)
            is_err = "error" in res
            return {"content": [{"type": "text", "text": json.dumps(res, indent=2)}], "isError": is_err}

        elif name == "jflap_launch_gui":
            cmd = ["java", "-jar", JAR_PATH]
            if args.get("file"):
                cmd.append(args["file"])
            subprocess.Popen(cmd)
            return {"content": [{"type": "text", "text": f"Launched JFLAP 7.1 GUI: {' '.join(cmd)}"}]}

        else:
            err_obj = {"error": f"Unknown tool: {name}", "error_type": "unknown_tool"}
            return {"content": [{"type": "text", "text": json.dumps(err_obj, indent=2)}], "isError": True}

    except Exception as e:
        err_obj = {"error": f"Error executing {name}: {str(e)}", "error_type": "tool_execution_error"}
        return {"content": [{"type": "text", "text": json.dumps(err_obj, indent=2)}], "isError": True}

# ---------------------------------------------------------------------------
# JSON-RPC 2.0 stdio Server Loop
# ---------------------------------------------------------------------------

def main():
    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                break
            line = line.strip()
            if not line:
                continue

            req = json.loads(line)
            req_id = req.get("id")
            method = req.get("method")
            params = req.get("params", {})

            if method == "initialize":
                resp = {
                    "jsonrpc": "2.0",
                    "id": req_id,
                    "result": {
                        "protocolVersion": "2024-11-05",
                        "capabilities": {
                            "tools": {}
                        },
                        "serverInfo": {
                            "name": "jflap-mcp",
                            "version": "1.0.0"
                        }
                    }
                }
            elif method == "notifications/initialized":
                # Acknowledgement notification, no response required
                continue
            elif method == "ping":
                resp = {
                    "jsonrpc": "2.0",
                    "id": req_id,
                    "result": {}
                }
            elif method == "tools/list":
                resp = {
                    "jsonrpc": "2.0",
                    "id": req_id,
                    "result": {
                        "tools": TOOLS
                    }
                }
            elif method == "tools/call":
                t_name = params.get("name")
                t_args = params.get("arguments", {})
                tool_res = call_tool(t_name, t_args)
                resp = {
                    "jsonrpc": "2.0",
                    "id": req_id,
                    "result": tool_res
                }
            else:
                resp = {
                    "jsonrpc": "2.0",
                    "id": req_id,
                    "error": {
                        "code": -32601,
                        "message": f"Method not found: {method}"
                    }
                }

            sys.stdout.write(json.dumps(resp) + "\n")
            sys.stdout.flush()

        except json.JSONDecodeError:
            continue
        except Exception as e:
            err_resp = {
                "jsonrpc": "2.0",
                "id": None,
                "error": {
                    "code": -32603,
                    "message": f"Internal error: {str(e)}"
                }
            }
            sys.stdout.write(json.dumps(err_resp) + "\n")
            sys.stdout.flush()

if __name__ == "__main__":
    main()
