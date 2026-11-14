#!/usr/bin/env python3
"""
Long-lived JVM Bridge Client for JFLAP 7.1.
Maintains a warm, persistent background Java process communicating via stdio
to reduce invocation latency from ~270ms down to ~2-4ms, and provides high-speed
batch simulation for thousands of inputs in a single call.
"""

import os
import sys
import json
import threading
import subprocess
import atexit
from typing import Dict, Any, List, Optional

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# Check third_party/jflap/ first, then root fallback
JAR_PATH = os.path.join(BASE_DIR, "third_party", "jflap", "JFLAP7.1.jar")
if not os.path.exists(JAR_PATH):
    JAR_PATH = os.path.join(BASE_DIR, "JFLAP7.1.jar")
BRIDGE_CLASS = "JFLAPBridge"


def ensure_environment():
    """Ensure JFLAP7.1.jar is present and JFLAPBridge.java is compiled."""
    if not os.path.exists(JAR_PATH):
        os.makedirs(os.path.dirname(JAR_PATH), exist_ok=True)
        import urllib.request
        url = "https://www.jflap.org/jflaptmp/july27-18/JFLAP7.1.jar"
        urllib.request.urlretrieve(url, JAR_PATH)

    class_path = os.path.join(BASE_DIR, "JFLAPBridge.class")
    java_path = os.path.join(BASE_DIR, "JFLAPBridge.java")
    if not os.path.exists(class_path) or (os.path.exists(java_path) and os.path.getmtime(java_path) > os.path.getmtime(class_path)):
        cp = f"{BASE_DIR};{JAR_PATH}" if os.name == "nt" else f"{BASE_DIR}:{JAR_PATH}"
        subprocess.run(["javac", "-cp", cp, java_path], check=True)


class JVMBridgeClient:
    """Manages a single long-lived JVM bridge process communicating via line-delimited JSON."""

    def __init__(self):
        self._proc: Optional[subprocess.Popen] = None
        self._lock = threading.Lock()
        atexit.register(self.close)

    def _start_process(self):
        ensure_environment()
        cp = f"{BASE_DIR};{JAR_PATH}" if os.name == "nt" else f"{BASE_DIR}:{JAR_PATH}"
        cmd = ["java", "-Djava.awt.headless=true", "-cp", cp, BRIDGE_CLASS, "--server"]
        self._proc = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
            bufsize=1
        )

    def _ensure_proc(self):
        if self._proc is None or self._proc.poll() is not None:
            self._start_process()

    def send(self, req: Dict[str, Any]) -> Dict[str, Any]:
        """Send a JSON request to the persistent JVM and parse the response line."""
        with self._lock:
            try:
                self._ensure_proc()
                req_line = json.dumps(req) + "\n"
                self._proc.stdin.write(req_line)
                self._proc.stdin.flush()

                resp_line = self._proc.stdout.readline()
                if not resp_line:
                    self._proc = None
                    return {"error": "JVM bridge terminated unexpectedly", "error_type": "bridge_terminated"}

                resp_line = resp_line.strip()
                idx = resp_line.find("{")
                if idx != -1:
                    return json.loads(resp_line[idx:])
                return {"raw_output": resp_line}
            except Exception as e:
                if self._proc:
                    try:
                        self._proc.kill()
                    except Exception:
                        pass
                self._proc = None
                return {"error": str(e), "error_type": "bridge_exception"}

    def run_command(self, cmd: str, *args) -> Dict[str, Any]:
        """Run an arbitrary bridge command with arguments."""
        str_args = [str(a) for a in args]
        return self.send({"cmd": cmd, "args": str_args})

    def simulate_batch(self, model: str, inputs: List[str], acceptance_mode: Optional[str] = None) -> Dict[str, Any]:
        """Execute high-performance batch simulation."""
        req = {
            "cmd": "simulate_batch",
            "model": model,
            "inputs": inputs
        }
        if acceptance_mode:
            req["acceptance_mode"] = acceptance_mode
        return self.send(req)

    def close(self):
        """Cleanly terminate the background JVM."""
        with self._lock:
            if self._proc and self._proc.poll() is None:
                try:
                    self._proc.stdin.write("quit\n")
                    self._proc.stdin.flush()
                    self._proc.wait(timeout=1.0)
                except Exception:
                    self._proc.kill()
            self._proc = None


# Global singleton instance
_CLIENT: Optional[JVMBridgeClient] = None

def get_bridge() -> JVMBridgeClient:
    global _CLIENT
    if _CLIENT is None:
        _CLIENT = JVMBridgeClient()
    return _CLIENT

def run_bridge(cmd: str, *args) -> Dict[str, Any]:
    return get_bridge().run_command(cmd, *args)

def run_simulate_batch(model: str, inputs: List[str], acceptance_mode: Optional[str] = None) -> Dict[str, Any]:
    return get_bridge().simulate_batch(model, inputs, acceptance_mode)
