"""
JFLAP XML (.jff) Builder
Builds valid JFLAP 7.1 XML structure files for Finite Automata, Pushdown Automata,
Turing Machines, and Grammars.
"""

import xml.etree.ElementTree as ET
from xml.dom import minidom
import os

def _prettify(elem: ET.Element) -> str:
    rough_string = ET.tostring(elem, encoding="utf-8")
    reparsed = minidom.parseString(rough_string)
    lines = reparsed.toprettyxml(indent="\t").split("\n")
    # Clean up empty lines
    cleaned = [line for line in lines if line.strip()]
    return "\n".join(cleaned) + "\n"

def build_fa(states: list, initial: str, finals: list, transitions: list, output_path: str = None) -> str:
    """
    states: list of state names (e.g. ['q0', 'q1', 'q2']) or dicts with id, name, x, y
    initial: state name or id
    finals: list of final state names or ids
    transitions: list of dicts: {'from': 'q0', 'to': 'q1', 'read': 'a'}
                 or tuples: ('q0', 'q1', 'a')
    """
    structure = ET.Element("structure")
    ET.SubElement(structure, "type").text = "fa"
    automaton = ET.SubElement(structure, "automaton")
    
    # State mapping
    name_to_id = {}
    for i, s in enumerate(states):
        if isinstance(s, dict):
            sid = str(s.get("id", i))
            sname = s.get("name", f"q{sid}")
            x = str(s.get("x", 100 + i * 120))
            y = str(s.get("y", 150))
        else:
            sid = str(i)
            sname = str(s)
            x = str(100 + i * 120)
            y = "150.0"
        
        name_to_id[sname] = sid
        name_to_id[sid] = sid
        
        state_el = ET.SubElement(automaton, "state", id=sid, name=sname)
        ET.SubElement(state_el, "x").text = x
        ET.SubElement(state_el, "y").text = y
        
        if str(initial) in (sname, sid):
            ET.SubElement(state_el, "initial")
        if any(str(f) in (sname, sid) for f in finals):
            ET.SubElement(state_el, "final")
            
    for t in transitions:
        if isinstance(t, (list, tuple)):
            from_st, to_st, read_sym = t[0], t[1], t[2] if len(t) > 2 else ""
        else:
            from_st = t.get("from")
            to_st = t.get("to")
            read_sym = t.get("read", "")
            
        from_id = name_to_id.get(str(from_st), str(from_st))
        to_id = name_to_id.get(str(to_st), str(to_st))
        
        t_el = ET.SubElement(automaton, "transition")
        ET.SubElement(t_el, "from").text = str(from_id)
        ET.SubElement(t_el, "to").text = str(to_id)
        ET.SubElement(t_el, "read").text = str(read_sym) if read_sym else ""

    xml_str = _prettify(structure)
    if output_path:
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(xml_str)
    return xml_str

def build_pda(states: list, initial: str, finals: list, transitions: list, output_path: str = None) -> str:
    """
    transitions: list of dicts: {'from': 'q0', 'to': 'q1', 'read': 'a', 'pop': 'Z', 'push': 'aZ'}
                 or tuples: ('q0', 'q1', 'a', 'Z', 'aZ')
    """
    structure = ET.Element("structure")
    ET.SubElement(structure, "type").text = "pda"
    automaton = ET.SubElement(structure, "automaton")
    
    name_to_id = {}
    for i, s in enumerate(states):
        if isinstance(s, dict):
            sid = str(s.get("id", i))
            sname = s.get("name", f"q{sid}")
            x = str(s.get("x", 100 + i * 120))
            y = str(s.get("y", 150))
        else:
            sid = str(i)
            sname = str(s)
            x = str(100 + i * 120)
            y = "150.0"
        
        name_to_id[sname] = sid
        name_to_id[sid] = sid
        
        state_el = ET.SubElement(automaton, "state", id=sid, name=sname)
        ET.SubElement(state_el, "x").text = x
        ET.SubElement(state_el, "y").text = y
        
        if str(initial) in (sname, sid):
            ET.SubElement(state_el, "initial")
        if any(str(f) in (sname, sid) for f in finals):
            ET.SubElement(state_el, "final")
            
    for t in transitions:
        if isinstance(t, (list, tuple)):
            from_st, to_st = t[0], t[1]
            read_sym = t[2] if len(t) > 2 else ""
            pop_sym = t[3] if len(t) > 3 else ""
            push_sym = t[4] if len(t) > 4 else ""
        else:
            from_st = t.get("from")
            to_st = t.get("to")
            read_sym = t.get("read", "")
            pop_sym = t.get("pop", "")
            push_sym = t.get("push", "")
            
        from_id = name_to_id.get(str(from_st), str(from_st))
        to_id = name_to_id.get(str(to_st), str(to_st))
        
        t_el = ET.SubElement(automaton, "transition")
        ET.SubElement(t_el, "from").text = str(from_id)
        ET.SubElement(t_el, "to").text = str(to_id)
        ET.SubElement(t_el, "read").text = str(read_sym) if read_sym else ""
        ET.SubElement(t_el, "pop").text = str(pop_sym) if pop_sym else ""
        ET.SubElement(t_el, "push").text = str(push_sym) if push_sym else ""

    xml_str = _prettify(structure)
    if output_path:
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(xml_str)
    return xml_str

def build_turing(states: list, initial: str, finals: list, transitions: list, tapes: int = 1, output_path: str = None) -> str:
    """
    transitions: list of dicts: {'from': 'q0', 'to': 'q1', 'read': 'a', 'write': 'X', 'direction': 'R'}
                 or tuples: ('q0', 'q1', 'a', 'X', 'R')
    """
    structure = ET.Element("structure")
    ET.SubElement(structure, "type").text = "turing"
    if tapes > 1:
        ET.SubElement(structure, "tapes").text = str(tapes)
    automaton = ET.SubElement(structure, "automaton")
    
    name_to_id = {}
    for i, s in enumerate(states):
        if isinstance(s, dict):
            sid = str(s.get("id", i))
            sname = s.get("name", f"q{sid}")
            x = str(s.get("x", 100 + i * 120))
            y = str(s.get("y", 150))
        else:
            sid = str(i)
            sname = str(s)
            x = str(100 + i * 120)
            y = "150.0"
        
        name_to_id[sname] = sid
        name_to_id[sid] = sid
        
        state_el = ET.SubElement(automaton, "state", id=sid, name=sname)
        ET.SubElement(state_el, "x").text = x
        ET.SubElement(state_el, "y").text = y
        
        if str(initial) in (sname, sid):
            ET.SubElement(state_el, "initial")
        if any(str(f) in (sname, sid) for f in finals):
            ET.SubElement(state_el, "final")
            
    for t in transitions:
        if isinstance(t, (list, tuple)):
            from_st, to_st = t[0], t[1]
            read_sym = t[2] if len(t) > 2 else ""
            write_sym = t[3] if len(t) > 3 else ""
            move_dir = t[4] if len(t) > 4 else "R"
        else:
            from_st = t.get("from")
            to_st = t.get("to")
            read_sym = t.get("read", "")
            write_sym = t.get("write", "")
            move_dir = t.get("direction", t.get("move", "R"))
            
        from_id = name_to_id.get(str(from_st), str(from_st))
        to_id = name_to_id.get(str(to_st), str(to_st))
        
        t_el = ET.SubElement(automaton, "transition")
        ET.SubElement(t_el, "from").text = str(from_id)
        ET.SubElement(t_el, "to").text = str(to_id)
        ET.SubElement(t_el, "read").text = str(read_sym) if read_sym else ""
        ET.SubElement(t_el, "write").text = str(write_sym) if write_sym else ""
        ET.SubElement(t_el, "move").text = str(move_dir) if move_dir else "R"

    xml_str = _prettify(structure)
    if output_path:
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(xml_str)
    return xml_str

def build_grammar(productions: list, start_var: str = "S", output_path: str = None) -> str:
    """
    productions: list of tuples [('S', 'aSb'), ('S', 'ab')] or strings ["S -> aSb", "S -> ab"]
    """
    structure = ET.Element("structure")
    ET.SubElement(structure, "type").text = "grammar"
    
    # Process string productions
    prods_list = []
    for p in productions:
        if isinstance(p, str):
            if "->" in p:
                lhs, rhs_all = p.split("->", 1)
                lhs = lhs.strip()
                for rhs in rhs_all.split("|"):
                    rhs = rhs.strip()
                    if rhs in ("lambda", "epsilon", "λ", "ε", ""):
                        rhs = ""
                    prods_list.append((lhs, rhs))
            else:
                parts = p.split()
                if len(parts) >= 2:
                    prods_list.append((parts[0], parts[1]))
        elif isinstance(p, (list, tuple)):
            prods_list.append((str(p[0]).strip(), str(p[1]).strip() if len(p) > 1 else ""))
        elif isinstance(p, dict):
            prods_list.append((str(p.get("left", "")).strip(), str(p.get("right", "")).strip()))

    for lhs, rhs in prods_list:
        p_el = ET.SubElement(structure, "production")
        ET.SubElement(p_el, "left").text = lhs
        ET.SubElement(p_el, "right").text = rhs

    xml_str = _prettify(structure)
    if output_path:
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(xml_str)
    return xml_str

def build_from_dict(data: dict, output_path: str = None) -> str:
    mtype = data.get("type", "fa").lower()
    if mtype in ("fa", "fsa", "dfa", "nfa"):
        return build_fa(
            states=data.get("states", []),
            initial=data.get("initial", data.get("initialState", "q0")),
            finals=data.get("finals", data.get("finalStates", [])),
            transitions=data.get("transitions", []),
            output_path=output_path
        )
    elif mtype == "pda":
        return build_pda(
            states=data.get("states", []),
            initial=data.get("initial", data.get("initialState", "q0")),
            finals=data.get("finals", data.get("finalStates", [])),
            transitions=data.get("transitions", []),
            output_path=output_path
        )
    elif mtype in ("turing", "tm"):
        return build_turing(
            states=data.get("states", []),
            initial=data.get("initial", data.get("initialState", "q0")),
            finals=data.get("finals", data.get("finalStates", [])),
            transitions=data.get("transitions", []),
            tapes=data.get("tapes", 1),
            output_path=output_path
        )
    elif mtype in ("grammar", "cfg", "rg"):
        return build_grammar(
            productions=data.get("productions", []),
            start_var=data.get("start", data.get("startVariable", "S")),
            output_path=output_path
        )
    else:
        raise ValueError(f"Unknown structure type: {mtype}")
