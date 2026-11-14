"""
Automated End-to-End Test Suite for JFLAP 7.1 CLI & MCP Server
Tests all CLI subcommands, Java bridge methods, model conversions,
simulation engines (FSA, PDA, TM), and MCP JSON-RPC protocol.
"""

import unittest
import os
import sys
import json
import subprocess
import shutil

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EXAMPLES_DIR = os.path.join(BASE_DIR, "examples")
CLI_PATH = os.path.join(BASE_DIR, "jflap_cli.py")
MCP_PATH = os.path.join(BASE_DIR, "jflap_mcp.py")

class TestJFLAPSuite(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.temp_dir = os.path.join(BASE_DIR, "tests", "temp")
        os.makedirs(cls.temp_dir, exist_ok=True)

    @classmethod
    def tearDownClass(cls):
        if os.path.exists(cls.temp_dir):
            shutil.rmtree(cls.temp_dir, ignore_errors=True)

    def run_cli(self, *args):
        cmd = [sys.executable, CLI_PATH, *args]
        res = subprocess.run(cmd, capture_output=True, text=True, cwd=BASE_DIR)
        return res

    # -------------------------------------------------------------
    # 1. Inspection Tests
    # -------------------------------------------------------------
    def test_01_inspect_fsa(self):
        fsa_file = os.path.join(EXAMPLES_DIR, "contains_ab_nfa.jff")
        res = self.run_cli("inspect", fsa_file, "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        self.assertEqual(data.get("category"), "automaton")
        self.assertEqual(data.get("type"), "fa")
        self.assertFalse(data.get("isDeterministic"))
        self.assertEqual(data.get("initialState"), 0)
        self.assertIn(2, data.get("finalStates"))
        self.assertEqual(len(data.get("states")), 3)

    def test_02_inspect_turing(self):
        tm_file = os.path.join(EXAMPLES_DIR, "an_bn_cn_turing.jff")
        res = self.run_cli("inspect", tm_file, "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        self.assertEqual(data.get("category"), "automaton")
        self.assertEqual(data.get("type"), "turing")
        self.assertTrue(data.get("isDeterministic"))
        self.assertEqual(len(data.get("states")), 6)

    def test_03_inspect_grammar(self):
        cfg_file = os.path.join(EXAMPLES_DIR, "an_bn_cfg.jff")
        res = self.run_cli("inspect", cfg_file, "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        self.assertEqual(data.get("category"), "grammar")
        self.assertEqual(data.get("grammarType"), "context_free")
        self.assertEqual(len(data.get("productions")), 2)

    # -------------------------------------------------------------
    # 2. Simulation Tests (FSA, PDA, Turing Machine)
    # -------------------------------------------------------------
    def test_04_simulate_fsa(self):
        fsa_file = os.path.join(EXAMPLES_DIR, "contains_ab_nfa.jff")
        res = self.run_cli("simulate", fsa_file, "ab", "aab", "bab", "bba", "xyz", "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        results = {r["input"]: r["accepted"] for r in data["results"]}
        self.assertTrue(results["ab"])
        self.assertTrue(results["aab"])
        self.assertTrue(results["bab"])
        self.assertFalse(results["bba"])
        self.assertFalse(results["xyz"])

    def test_05_simulate_pda(self):
        pda_file = os.path.join(EXAMPLES_DIR, "an_bn_pda.jff")
        res = self.run_cli("simulate", pda_file, "ab", "aabb", "aaabbb", "aab", "abb", "bbaa", "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        results = {r["input"]: r["accepted"] for r in data["results"]}
        self.assertTrue(results["ab"])
        self.assertTrue(results["aabb"])
        self.assertTrue(results["aaabbb"])
        self.assertFalse(results["aab"])
        self.assertFalse(results["abb"])
        self.assertFalse(results["bbaa"])

    def test_06_simulate_turing(self):
        tm_file = os.path.join(EXAMPLES_DIR, "an_bn_cn_turing.jff")
        res = self.run_cli("simulate", tm_file, "abc", "aabbcc", "ab", "aabbc", "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        results = {r["input"]: r["accepted"] for r in data["results"]}
        self.assertTrue(results["abc"])
        self.assertTrue(results["aabbcc"])
        self.assertFalse(results["ab"])
        self.assertFalse(results["aabbc"])

    # -------------------------------------------------------------
    # 3. Tracing Tests
    # -------------------------------------------------------------
    def test_07_trace_pda(self):
        pda_file = os.path.join(EXAMPLES_DIR, "an_bn_pda.jff")
        res = self.run_cli("trace", pda_file, "aabb", "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        self.assertTrue(data.get("accepted"))
        steps = data.get("steps", [])
        self.assertGreater(len(steps), 3)
        self.assertEqual(steps[0]["stack"], "Z")
        self.assertTrue(steps[-1]["isAccept"])

    # -------------------------------------------------------------
    # 4. FSA Transformations & Equivalence
    # -------------------------------------------------------------
    def test_08_nfa_to_dfa_and_minimize(self):
        nfa_file = os.path.join(EXAMPLES_DIR, "contains_ab_nfa.jff")
        dfa_file = os.path.join(self.temp_dir, "test_dfa.jff")
        min_file = os.path.join(self.temp_dir, "test_min.jff")

        # NFA -> DFA
        res1 = self.run_cli("fsa", "nfa-to-dfa", nfa_file, dfa_file, "--json")
        self.assertEqual(res1.returncode, 0)
        d1 = json.loads(res1.stdout)
        self.assertTrue(d1.get("success"))
        self.assertEqual(d1.get("dfaStates"), 4)

        # Minimize
        res2 = self.run_cli("fsa", "minimize", dfa_file, min_file, "--json")
        self.assertEqual(res2.returncode, 0)
        d2 = json.loads(res2.stdout)
        self.assertTrue(d2.get("success"))
        self.assertEqual(d2.get("minimizedStates"), 3)

        # Equivalence Check between original NFA and Minimized DFA
        res3 = self.run_cli("fsa", "equals", nfa_file, min_file, "--json")
        self.assertEqual(res3.returncode, 0)
        d3 = json.loads(res3.stdout)
        self.assertTrue(d3.get("equivalent"))

    # -------------------------------------------------------------
    # 5. Grammar & Model Conversions
    # -------------------------------------------------------------
    def test_09_grammar_parse_and_conversions(self):
        # 1. Convert FSA to Regular Grammar
        fsa_file = os.path.join(EXAMPLES_DIR, "contains_ab_min_dfa.jff")
        rg_file = os.path.join(self.temp_dir, "test_rg.jff")
        res1 = self.run_cli("convert", "fsa-to-rg", fsa_file, rg_file, "--json")
        self.assertEqual(res1.returncode, 0)
        self.assertTrue(json.loads(res1.stdout).get("success"))

        # 2. Parse string using the regular grammar
        res2 = self.run_cli("grammar", "parse", rg_file, "aab", "--json")
        self.assertEqual(res2.returncode, 0)
        d2 = json.loads(res2.stdout)
        self.assertTrue(d2.get("accepted"))

        # 3. Convert Regular Grammar back to FSA
        fsa_roundtrip = os.path.join(self.temp_dir, "test_fsa_rt.jff")
        res3 = self.run_cli("convert", "rg-to-fsa", rg_file, fsa_roundtrip, "--json")
        self.assertEqual(res3.returncode, 0)
        self.assertTrue(json.loads(res3.stdout).get("success"))

        # 4. Equivalence check of round-trip
        res4 = self.run_cli("fsa", "equals", fsa_file, fsa_roundtrip, "--json")
        self.assertEqual(res4.returncode, 0)
        self.assertTrue(json.loads(res4.stdout).get("equivalent"))

    # -------------------------------------------------------------
    # 6. Programmatic Creation & DOT Export
    # -------------------------------------------------------------
    def test_10_create_model_and_dot(self):
        created_file = os.path.join(self.temp_dir, "created_toggle.jff")
        res1 = self.run_cli(
            "create", "fa",
            "--states", "q0,q1",
            "--initial", "q0",
            "--finals", "q1",
            "--transitions", "q0,q1,0; q1,q0,1; q0,q0,1; q1,q1,0",
            "-o", created_file
        )
        self.assertEqual(res1.returncode, 0)
        self.assertTrue(os.path.exists(created_file))

        # Test simulation on created model
        res2 = self.run_cli("simulate", created_file, "0", "010", "11", "--json")
        self.assertEqual(res2.returncode, 0)
        d2 = json.loads(res2.stdout)
        res_map = {r["input"]: r["accepted"] for r in d2["results"]}
        self.assertTrue(res_map["0"])
        self.assertTrue(res_map["010"])
        self.assertFalse(res_map["11"])

        # Test DOT export
        res3 = self.run_cli("dot", created_file)
        self.assertEqual(res3.returncode, 0)
        self.assertIn("digraph Automaton", res3.stdout)
        self.assertIn("q0 -> q1", res3.stdout)

    # -------------------------------------------------------------
    # 7. Advanced Transformations & Analysis (FSA-to-RE, FIRST/FOLLOW, Combine, L-System)
    # -------------------------------------------------------------
    def test_11_fsa_to_re(self):
        fsa_file = os.path.join(EXAMPLES_DIR, "contains_ab_dfa.jff")
        res = self.run_cli("convert", "fsa-to-re", fsa_file, "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        self.assertTrue(data.get("success"))
        self.assertIn("regularExpression", data)
        self.assertGreater(len(data["regularExpression"]), 0)

    def test_12_grammar_first_follow(self):
        cfg_file = os.path.join(EXAMPLES_DIR, "an_bn_cfg.jff")
        res = self.run_cli("grammar", "first-follow", cfg_file, "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        self.assertIn("first", data)
        self.assertIn("follow", data)
        self.assertFalse(data.get("isLL1"))  # S -> aSb | ab is not LL(1)

    def test_13_fsa_combine(self):
        fsa1 = os.path.join(EXAMPLES_DIR, "contains_ab_dfa.jff")
        fsa2 = os.path.join(EXAMPLES_DIR, "toggle_dfa.jff")
        out_comb = os.path.join(self.temp_dir, "combined.jff")
        res = self.run_cli("fsa", "combine", fsa1, fsa2, out_comb, "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        self.assertTrue(data.get("success"))
        self.assertEqual(data.get("totalStates"), 6)
        self.assertEqual(data.get("totalTransitions"), 12)

    def test_14_lsystem_expand(self):
        ls_file = os.path.join(EXAMPLES_DIR, "fibonacci_lsystem.jff")
        res = self.run_cli("lsystem", "expand", ls_file, "4", "--json")
        self.assertEqual(res.returncode, 0, f"Error: {res.stderr}")
        data = json.loads(res.stdout)
        self.assertTrue(data.get("success"))
        self.assertEqual(data.get("level"), 4)
        self.assertEqual(data.get("length"), 8)
        self.assertEqual(data.get("expandedString"), "ABAABABA")

    # -------------------------------------------------------------
    # 8. Model Context Protocol (MCP) JSON-RPC Protocol Tests
    # -------------------------------------------------------------
    def test_15_mcp_server_protocol(self):
        proc = subprocess.Popen(
            [sys.executable, MCP_PATH],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            cwd=BASE_DIR
        )

        try:
            # 1. Initialize
            init_req = {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}}
            proc.stdin.write(json.dumps(init_req) + "\n")
            proc.stdin.flush()
            init_res = json.loads(proc.stdout.readline())
            self.assertEqual(init_res["result"]["serverInfo"]["name"], "jflap-mcp")

            # 2. tools/list
            tools_req = {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}}
            proc.stdin.write(json.dumps(tools_req) + "\n")
            proc.stdin.flush()
            tools_res = json.loads(proc.stdout.readline())
            tools = tools_res["result"]["tools"]
            self.assertEqual(len(tools), 10)

            # 3. tools/call jflap_simulate
            call_req = {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {
                    "name": "jflap_simulate",
                    "arguments": {
                        "file": os.path.join(EXAMPLES_DIR, "contains_ab_nfa.jff"),
                        "inputs": ["ab", "xyz"]
                    }
                }
            }
            proc.stdin.write(json.dumps(call_req) + "\n")
            proc.stdin.flush()
            call_res = json.loads(proc.stdout.readline())
            sim_data = json.loads(call_res["result"]["content"][0]["text"])
            self.assertTrue(sim_data["results"][0]["accepted"])
            self.assertFalse(sim_data["results"][1]["accepted"])

            # 4. tools/call jflap_lsystem_expand
            ls_call = {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {
                    "name": "jflap_lsystem_expand",
                    "arguments": {
                        "file": os.path.join(EXAMPLES_DIR, "fibonacci_lsystem.jff"),
                        "level": 3
                    }
                }
            }
            proc.stdin.write(json.dumps(ls_call) + "\n")
            proc.stdin.flush()
            ls_res = json.loads(proc.stdout.readline())
            ls_data = json.loads(ls_res["result"]["content"][0]["text"])
            self.assertTrue(ls_data["success"])
            self.assertEqual(ls_data["expandedString"], "ABAAB")

        finally:
            if proc.stdin and not proc.stdin.closed:
                proc.stdin.close()
            if proc.stdout and not proc.stdout.closed:
                proc.stdout.close()
            if proc.stderr and not proc.stderr.closed:
                proc.stderr.close()
            if proc.poll() is None:
                proc.kill()
                proc.wait()

    def test_16_pda_explicit_acceptance_modes(self):
        """Test strict PDA acceptance modes: final_state vs empty_stack."""
        final_pda = os.path.join(EXAMPLES_DIR, "pda_final_only.jff")
        empty_pda = os.path.join(EXAMPLES_DIR, "pda_empty_only.jff")
        an_bn_pda = os.path.join(EXAMPLES_DIR, "an_bn_pda.jff")

        # 1. pda_final_only: accepts under final_state, rejects under empty_stack
        res_f1 = self.run_cli("simulate", final_pda, "aabb", "-a", "final_state", "--json")
        self.assertEqual(res_f1.returncode, 0)
        d_f1 = json.loads(res_f1.stdout)
        self.assertEqual(d_f1.get("acceptanceMode"), "final_state")
        self.assertTrue(d_f1["results"][0]["accepted"])

        res_f2 = self.run_cli("simulate", final_pda, "aabb", "-a", "empty_stack", "--json")
        self.assertEqual(res_f2.returncode, 0)
        d_f2 = json.loads(res_f2.stdout)
        self.assertEqual(d_f2.get("acceptanceMode"), "empty_stack")
        self.assertFalse(d_f2["results"][0]["accepted"])

        # 2. pda_empty_only: rejects under final_state, accepts under empty_stack
        res_e1 = self.run_cli("simulate", empty_pda, "aabb", "-a", "final_state", "--json")
        self.assertEqual(res_e1.returncode, 0)
        d_e1 = json.loads(res_e1.stdout)
        self.assertEqual(d_e1.get("acceptanceMode"), "final_state")
        self.assertFalse(d_e1["results"][0]["accepted"])

        res_e2 = self.run_cli("simulate", empty_pda, "aabb", "-a", "empty_stack", "--json")
        self.assertEqual(res_e2.returncode, 0)
        d_e2 = json.loads(res_e2.stdout)
        self.assertEqual(d_e2.get("acceptanceMode"), "empty_stack")
        self.assertTrue(d_e2["results"][0]["accepted"])

        # 3. Classic an_bn_pda under both modes
        res_ab1 = self.run_cli("simulate", an_bn_pda, "aabb", "-a", "final_state", "--json")
        d_ab1 = json.loads(res_ab1.stdout)
        self.assertTrue(d_ab1["results"][0]["accepted"])

        res_ab2 = self.run_cli("simulate", an_bn_pda, "aabb", "-a", "empty_stack", "--json")
        d_ab2 = json.loads(res_ab2.stdout)
        self.assertTrue(d_ab2["results"][0]["accepted"])

        # 4. Trace reflects explicit acceptance mode
        tr_f = self.run_cli("trace", final_pda, "aabb", "-a", "empty_stack", "--json")
        self.assertEqual(tr_f.returncode, 0)
        d_tr_f = json.loads(tr_f.stdout)
        self.assertEqual(d_tr_f.get("acceptanceMode"), "empty_stack")
        self.assertFalse(d_tr_f.get("accepted"))

    def test_17_simulate_batch(self):
        """Test high-performance simulate_batch with many inputs."""
        fsa_file = os.path.join(EXAMPLES_DIR, "contains_ab_dfa.jff")
        inputs = ["ab", "aab", "xyz", "bab", "bba"]
        res = self.run_cli("simulate-batch", fsa_file, *inputs, "--json")
        self.assertEqual(res.returncode, 0)
        data = json.loads(res.stdout)
        self.assertEqual(data.get("total"), 5)
        self.assertEqual(data.get("verdicts"), [True, True, False, True, False])

    def test_18_export_table_dfa_and_error_pda(self):
        """Test jflap_export_table for DFA and structured error for PDA."""
        dfa_file = os.path.join(EXAMPLES_DIR, "contains_ab_dfa.jff")
        pda_file = os.path.join(EXAMPLES_DIR, "an_bn_pda.jff")

        # DFA export table
        res1 = self.run_cli("export-table", dfa_file, "--json")
        self.assertEqual(res1.returncode, 0)
        d1 = json.loads(res1.stdout)
        self.assertEqual(d1.get("type"), "fa")
        self.assertEqual(sorted(d1.get("alphabet")), ["a", "b"])
        self.assertIn("q0", d1.get("states"))
        self.assertIn("q2", d1.get("accepting_states"))
        self.assertGreater(len(d1.get("transitions")), 0)

        # PDA export table should return structured error
        res2 = self.run_cli("export-table", pda_file, "--json")
        self.assertNotEqual(res2.returncode, 0)
        d2 = json.loads(res2.stdout)
        self.assertIn("error", d2)
        self.assertEqual(d2.get("error_type"), "unsupported_model_type")

    def test_19_equivalence_check_and_distinguishing_string(self):
        """Test equivalence check with shortest distinguishing string."""
        nfa_file = os.path.join(EXAMPLES_DIR, "contains_ab_nfa.jff")
        min_dfa_file = os.path.join(EXAMPLES_DIR, "contains_ab_min_dfa.jff")
        toggle_file = os.path.join(EXAMPLES_DIR, "toggle_dfa.jff")

        # Equivalent models
        res1 = self.run_cli("equivalence-check", nfa_file, min_dfa_file, "--json")
        self.assertEqual(res1.returncode, 0)
        d1 = json.loads(res1.stdout)
        self.assertTrue(d1.get("equivalent"))
        self.assertIsNone(d1.get("shortest_distinguishing_string"))

        # Non-equivalent models
        res2 = self.run_cli("equivalence-check", min_dfa_file, toggle_file, "--json")
        self.assertEqual(res2.returncode, 0)
        d2 = json.loads(res2.stdout)
        self.assertFalse(d2.get("equivalent"))
        self.assertIsNotNone(d2.get("shortest_distinguishing_string"))
        self.assertIn("modelA_accepts", d2)
        self.assertIn("modelB_accepts", d2)


if __name__ == "__main__":
    unittest.main()
