import grammar.lsystem.*;
import regular.RegularExpression;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import file.xml.Transducer;
import file.xml.TransducerFactory;
import file.xml.TMTransducer;
import file.xml.TMBBTransducer;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.awt.Point;
import javax.swing.tree.DefaultTreeModel;

import automata.*;
import automata.fsa.*;
import automata.pda.*;
import automata.turing.*;
import automata.mealy.*;
import automata.graph.FSAEqualityChecker;
import grammar.*;
import grammar.cfg.*;
import grammar.reg.*;
import grammar.parse.*;
import file.XMLCodec;
import file.Codec;

public class JFLAPBridge {

    private static final XMLCodec CODEC = new XMLCodec();
    private static boolean isServerMode = false;

    public static class BridgeException extends Exception {
        private final String errorType;
        public BridgeException(String errorType, String message) {
            super(message);
            this.errorType = errorType;
        }
        public String getErrorType() { return errorType; }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            printError("invalid_argument", "No command specified. Usage: java JFLAPBridge <command> [args...]");
            System.exit(1);
        }

        if ("--server".equals(args[0]) || "server".equals(args[0])) {
            isServerMode = true;
            runServer();
            return;
        }

        try {
            dispatchCommand(args);
        } catch (Throwable t) {
            handleThrowable(t);
            System.exit(1);
        }
    }

    private static void runServer() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                    break;
                }
                try {
                    handleServerLine(line);
                } catch (Throwable t) {
                    handleThrowable(t);
                }
            }
        } catch (Exception e) {
            // Clean exit when stdin closes
        }
    }

    private static void handleServerLine(String line) throws Exception {
        if (line.startsWith("{")) {
            Map<String, Object> req = parseSimpleJson(line);
            String cmd = (String) req.get("cmd");
            if (cmd == null) {
                printError("invalid_argument", "Missing 'cmd' in JSON request");
                return;
            }
            if ("simulate_batch".equalsIgnoreCase(cmd)) {
                String model = (String) req.get("model");
                if (model == null) model = (String) req.get("file");
                @SuppressWarnings("unchecked")
                List<String> inputs = (List<String>) req.get("inputs");
                if (inputs == null) inputs = Collections.emptyList();
                String accMode = (String) req.get("acceptance_mode");
                if (accMode == null) accMode = (String) req.get("acceptanceMode");
                simulateBatch(model, inputs, accMode);
                return;
            }
            @SuppressWarnings("unchecked")
            List<String> listArgs = (List<String>) req.get("args");
            if (listArgs == null) listArgs = Collections.emptyList();
            String[] args = new String[listArgs.size() + 1];
            args[0] = cmd;
            for (int i = 0; i < listArgs.size(); i++) {
                args[i + 1] = listArgs.get(i);
            }
            dispatchCommand(args);
        } else {
            String[] tokens = line.split("\\t+");
            if (tokens.length == 1) tokens = line.split("\\s+");
            dispatchCommand(tokens);
        }
    }

    public static void dispatchCommand(String[] args) throws Exception {
        String command = args[0].toLowerCase();
        switch (command) {
            case "inspect":
                inspect(args);
                break;
            case "simulate":
                simulate(args);
                break;
            case "simulate_batch":
                handleSimulateBatchCli(args);
                break;
            case "trace":
                trace(args);
                break;
            case "nfa_to_dfa":
                nfaToDfa(args);
                break;
            case "minimize":
                minimize(args);
                break;
            case "fsa_equals":
            case "equivalence_check":
                equivalenceCheck(args);
                break;
            case "export_table":
                exportTable(args);
                break;
            case "grammar_parse":
                grammarParse(args);
                break;
            case "grammar_clean":
                grammarClean(args);
                break;
            case "fsa_to_rg":
                fsaToRg(args);
                break;
            case "rg_to_fsa":
                rgToFsa(args);
                break;
            case "cfg_to_pda":
                cfgToPda(args);
                break;
            case "pda_to_cfg":
                pdaToCfg(args);
                break;
            case "to_dot":
                toDot(args);
                break;
            case "fsa_to_re":
                fsaToRe(args);
                break;
            case "first_follow":
                firstFollow(args);
                break;
            case "lsystem_expand":
                lsystemExpand(args);
                break;
            case "combine":
                combineAutomata(args);
                break;
            default:
                throw new BridgeException("unknown_command", "Unknown command: " + command);
        }
    }

    private static void handleSimulateBatchCli(String[] args) throws Exception {
        if (args.length < 2) {
            throw new BridgeException("invalid_argument", "Usage: simulate_batch <file.jff> [--acceptance final_state|empty_stack] [inputs...]");
        }
        String model = args[1];
        String explicitAcceptance = null;
        List<String> inputs = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            if ("--acceptance".equalsIgnoreCase(args[i]) || "--acceptance-mode".equalsIgnoreCase(args[i]) || "-a".equalsIgnoreCase(args[i])) {
                if (i + 1 < args.length) {
                    explicitAcceptance = args[++i];
                }
            } else if (args[i].startsWith("--acceptance=")) {
                explicitAcceptance = args[i].substring("--acceptance=".length());
            } else if (args[i].startsWith("--acceptance-mode=")) {
                explicitAcceptance = args[i].substring("--acceptance-mode=".length());
            } else {
                inputs.add(args[i]);
            }
        }
        simulateBatch(model, inputs, explicitAcceptance);
    }

    private static void simulateBatch(String modelPath, List<String> inputs, String explicitAcceptance) throws Exception {
        File file = new File(modelPath);
        if (!file.exists()) {
            throw new BridgeException("file_not_found", "File not found: " + file.getAbsolutePath());
        }

        Serializable obj;
        try {
            obj = decodeFile(file);
        } catch (Exception e) {
            throw new BridgeException("malformed_model", "Failed to decode model: " + e.getMessage());
        }

        if (!(obj instanceof Automaton)) {
            throw new BridgeException("unsupported_operation", "File is not an automaton: " + obj.getClass().getName());
        }

        Automaton auto = (Automaton) obj;
        String pdaMode = null;
        if (auto instanceof PushdownAutomaton) {
            pdaMode = resolvePdaAcceptanceMode(file, explicitAcceptance);
        }

        AutomatonSimulator sim = getAutomatonSimulator(auto, pdaMode);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(escapeJson(file.getAbsolutePath())).append("\",");
        if (pdaMode != null) {
            sb.append("\"acceptanceMode\":\"").append(pdaMode).append("\",");
        }
        sb.append("\"total\":").append(inputs.size()).append(",");
        sb.append("\"verdicts\":[");
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) sb.append(",");
            boolean accepted = false;
            try {
                accepted = runSimulation(sim, auto, inputs.get(i), 10000, pdaMode);
            } catch (Exception e) {
                accepted = false;
            }
            sb.append(accepted);
        }
        sb.append("]}");
        System.out.println(sb.toString());
        System.out.flush();
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof BridgeException) {
            BridgeException be = (BridgeException) t;
            printError(be.getErrorType(), be.getMessage());
        } else {
            String msg = t.getMessage();
            if (msg == null || msg.isEmpty()) msg = t.getClass().getSimpleName();
            printError("execution_error", msg);
        }
    }

    public static void printError(String errorType, String msg) {
        System.err.println("[" + errorType + "] " + msg);
        System.out.println("{\"error\":\"" + escapeJson(msg) + "\",\"error_type\":\"" + escapeJson(errorType) + "\"}");
        System.out.flush();
    }

    public static void printError(String msg) {
        printError("error", msg);
    }

    private static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        int i = 0;
        int len = json.length();
        while (i < len) {
            int q1 = json.indexOf('"', i);
            if (q1 == -1) break;
            int q2 = findNextQuote(json, q1 + 1);
            if (q2 == -1) break;
            String key = unescapeJson(json.substring(q1 + 1, q2));
            int colon = json.indexOf(':', q2 + 1);
            if (colon == -1) break;
            int vStart = colon + 1;
            while (vStart < len && Character.isWhitespace(json.charAt(vStart))) vStart++;
            if (vStart >= len) break;
            char firstChar = json.charAt(vStart);
            if (firstChar == '"') {
                int vEnd = findNextQuote(json, vStart + 1);
                if (vEnd == -1) break;
                String val = unescapeJson(json.substring(vStart + 1, vEnd));
                map.put(key, val);
                i = vEnd + 1;
            } else if (firstChar == '[') {
                List<String> arr = new ArrayList<>();
                int cur = vStart + 1;
                while (cur < len) {
                    while (cur < len && Character.isWhitespace(json.charAt(cur))) cur++;
                    if (cur < len && json.charAt(cur) == ']') {
                        cur++;
                        break;
                    }
                    if (cur < len && json.charAt(cur) == ',') {
                        cur++;
                        continue;
                    }
                    if (cur < len && json.charAt(cur) == '"') {
                        int strEnd = findNextQuote(json, cur + 1);
                        if (strEnd == -1) break;
                        arr.add(unescapeJson(json.substring(cur + 1, strEnd)));
                        cur = strEnd + 1;
                    } else {
                        cur++;
                    }
                }
                map.put(key, arr);
                i = cur;
            } else {
                int comma = json.indexOf(',', vStart);
                int brace = json.indexOf('}', vStart);
                int end = (comma != -1 && comma < brace) ? comma : brace;
                if (end == -1) end = len;
                String val = json.substring(vStart, end).trim();
                map.put(key, val);
                i = end + 1;
            }
        }
        return map;
    }

    private static int findNextQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '"' && s.charAt(i - 1) != '\\') {
                return i;
            }
        }
        return -1;
    }

    private static String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            try {
                                int code = Integer.parseInt(s.substring(i + 1, i + 5), 16);
                                sb.append((char) code);
                                i += 4;
                            } catch (Exception e) {
                                sb.append("\\u");
                            }
                        } else {
                            sb.append("\\u");
                        }
                        break;
                    default: sb.append(next); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------------
    // Inspect
    // -------------------------------------------------------------
    private static void inspect(String[] args) throws Exception {
        if (args.length < 2) {
            throw new BridgeException("invalid_argument", "Usage: inspect <file.jff>");
        }
        File file = new File(args[1]);
        if (!file.exists()) {
            throw new BridgeException("file_not_found", "File not found: " + file.getAbsolutePath());
        }

        Serializable obj;
        try {
            obj = decodeFile(file);
        } catch (Exception e) {
            throw new BridgeException("malformed_model", "Failed to parse model file: " + e.getMessage());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"file\":\"").append(escapeJson(file.getAbsolutePath())).append("\",");

        if (obj instanceof Automaton) {
            Automaton auto = (Automaton) obj;
            String typeStr = "unknown";
            if (auto instanceof FiniteStateAutomaton) typeStr = "fa";
            else if (auto instanceof PushdownAutomaton) typeStr = "pda";
            else if (auto instanceof TuringMachine) typeStr = "turing";
            else if (auto instanceof MealyMachine) typeStr = "mealy";
            else if (auto instanceof MooreMachine) typeStr = "moore";

            sb.append("\"category\":\"automaton\",");
            sb.append("\"type\":\"").append(typeStr).append("\",");

            if (auto instanceof PushdownAutomaton) {
                String declMode = getDeclaredAcceptanceMode(file);
                if (declMode != null) {
                    sb.append("\"declaredAcceptanceMode\":\"").append(declMode).append("\",");
                }
            }

            // States
            sb.append("\"states\":[");
            State[] states = auto.getStates();
            for (int i = 0; i < states.length; i++) {
                State s = states[i];
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"id\":").append(s.getID()).append(",");
                sb.append("\"name\":\"").append(escapeJson(s.getName())).append("\",");
                sb.append("\"initial\":").append(auto.isInitialState(s)).append(",");
                sb.append("\"final\":").append(auto.isFinalState(s)).append(",");
                Point p = s.getPoint();
                sb.append("\"x\":").append(p != null ? p.x : 0).append(",");
                sb.append("\"y\":").append(p != null ? p.y : 0);
                sb.append("}");
            }
            sb.append("],");

            // Initial and Final states
            State init = auto.getInitialState();
            sb.append("\"initialState\":").append(init != null ? init.getID() : "null").append(",");
            sb.append("\"finalStates\":[");
            State[] finals = auto.getFinalStates();
            for (int i = 0; i < finals.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(finals[i].getID());
            }
            sb.append("],");

            // Alphabet
            Set<String> alphabetSet = new TreeSet<>();
            for (Transition t : auto.getTransitions()) {
                String sym = "";
                if (t instanceof FSATransition) {
                    sym = ((FSATransition) t).getLabel();
                } else if (t instanceof PDATransition) {
                    sym = ((PDATransition) t).getInputToRead();
                } else if (t instanceof TMTransition) {
                    sym = ((TMTransition) t).getRead(0);
                } else if (t instanceof MealyTransition) {
                    sym = ((MealyTransition) t).getLabel();
                }
                if (sym != null && !sym.isEmpty()) {
                    alphabetSet.add(sym);
                }
            }
            sb.append("\"alphabet\":[");
            int aCount = 0;
            for (String a : alphabetSet) {
                if (aCount++ > 0) sb.append(",");
                sb.append("\"").append(escapeJson(a)).append("\"");
            }
            sb.append("],");

            // Determinism
            boolean isDeterministic = true;
            try {
                NondeterminismDetector d = NondeterminismDetectorFactory.getDetector(auto);
                if (d != null) {
                    State[] nd = d.getNondeterministicStates(auto);
                    if (nd.length > 0) isDeterministic = false;
                }
            } catch (Exception e) {}
            sb.append("\"isDeterministic\":").append(isDeterministic).append(",");

            // Completeness check: does every state have outgoing transitions for every alphabet symbol?
            boolean isComplete = true;
            if (!alphabetSet.isEmpty()) {
                for (State s : auto.getStates()) {
                    Set<String> stateOut = new HashSet<>();
                    for (Transition t : auto.getTransitionsFromState(s)) {
                        String sym = "";
                        if (t instanceof FSATransition) sym = ((FSATransition) t).getLabel();
                        else if (t instanceof PDATransition) sym = ((PDATransition) t).getInputToRead();
                        else if (t instanceof TMTransition) sym = ((TMTransition) t).getRead(0);
                        if (sym != null && !sym.isEmpty()) stateOut.add(sym);
                    }
                    if (!stateOut.containsAll(alphabetSet)) {
                        isComplete = false;
                        break;
                    }
                }
            }
            sb.append("\"isComplete\":").append(isComplete).append(",");

            // Unreachable states (cannot be reached from initial state)
            Set<Integer> reachable = new HashSet<>();
            State initState = auto.getInitialState();
            if (initState != null) {
                Queue<State> rQueue = new LinkedList<>();
                rQueue.add(initState);
                reachable.add(initState.getID());
                while (!rQueue.isEmpty()) {
                    State curr = rQueue.poll();
                    for (Transition t : auto.getTransitionsFromState(curr)) {
                        State toState = t.getToState();
                        if (toState != null && reachable.add(toState.getID())) {
                            rQueue.add(toState);
                        }
                    }
                }
            }

            List<Integer> unreachableStates = new ArrayList<>();
            for (State s : auto.getStates()) {
                if (!reachable.contains(s.getID())) {
                    unreachableStates.add(s.getID());
                }
            }

            sb.append("\"unreachableStates\":[");
            for (int i = 0; i < unreachableStates.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(unreachableStates.get(i));
            }
            sb.append("],");

            // Dead states (cannot reach any final state)
            Map<Integer, List<Integer>> reverseAdj = new HashMap<>();
            for (State s : auto.getStates()) {
                reverseAdj.put(s.getID(), new ArrayList<>());
            }
            for (Transition t : auto.getTransitions()) {
                State u = t.getFromState();
                State v = t.getToState();
                if (u != null && v != null && reverseAdj.containsKey(v.getID())) {
                    reverseAdj.get(v.getID()).add(u.getID());
                }
            }

            Set<Integer> canReachFinal = new HashSet<>();
            Queue<Integer> fQueue = new LinkedList<>();
            for (State fs : auto.getFinalStates()) {
                canReachFinal.add(fs.getID());
                fQueue.add(fs.getID());
            }

            while (!fQueue.isEmpty()) {
                int currId = fQueue.poll();
                List<Integer> predecessors = reverseAdj.get(currId);
                if (predecessors != null) {
                    for (int predId : predecessors) {
                        if (canReachFinal.add(predId)) {
                            fQueue.add(predId);
                        }
                    }
                }
            }

            List<Integer> deadStates = new ArrayList<>();
            for (State s : auto.getStates()) {
                if (!canReachFinal.contains(s.getID())) {
                    deadStates.add(s.getID());
                }
            }

            sb.append("\"deadStates\":[");
            for (int i = 0; i < deadStates.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(deadStates.get(i));
            }
            sb.append("],");

            // Transitions
            sb.append("\"transitions\":[");
            Transition[] transitions = auto.getTransitions();
            for (int i = 0; i < transitions.length; i++) {
                Transition t = transitions[i];
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"from\":").append(t.getFromState().getID()).append(",");
                sb.append("\"to\":").append(t.getToState().getID()).append(",");
                if (t instanceof FSATransition) {
                    FSATransition ft = (FSATransition) t;
                    sb.append("\"read\":\"").append(escapeJson(ft.getLabel())).append("\"");
                } else if (t instanceof PDATransition) {
                    PDATransition pt = (PDATransition) t;
                    sb.append("\"read\":\"").append(escapeJson(pt.getInputToRead())).append("\",");
                    sb.append("\"pop\":\"").append(escapeJson(pt.getStringToPop())).append("\",");
                    sb.append("\"push\":\"").append(escapeJson(pt.getStringToPush())).append("\"");
                } else if (t instanceof TMTransition) {
                    TMTransition tt = (TMTransition) t;
                    sb.append("\"read\":\"").append(escapeJson(tt.getRead(0))).append("\",");
                    sb.append("\"write\":\"").append(escapeJson(tt.getWrite(0))).append("\",");
                    sb.append("\"direction\":\"").append(escapeJson(tt.getDirection(0))).append("\"");
                } else if (t instanceof MealyTransition) {
                    MealyTransition mt = (MealyTransition) t;
                    sb.append("\"read\":\"").append(escapeJson(mt.getLabel())).append("\",");
                    sb.append("\"output\":\"").append(escapeJson(mt.getOutput())).append("\"");
                } else {
                    sb.append("\"label\":\"").append(escapeJson(t.getDescription())).append("\"");
                }
                sb.append("}");
            }
            sb.append("]");

        } else if (obj instanceof Grammar) {
            Grammar g = (Grammar) obj;
            sb.append("\"category\":\"grammar\",");
            sb.append("\"startVariable\":\"").append(escapeJson(g.getStartVariable())).append("\",");

            String gType = "unrestricted";
            if (GrammarChecker.isRightLinearGrammar(g)) gType = "right_linear";
            else if (GrammarChecker.isRegularGrammar(g)) gType = "regular";
            else if (GrammarChecker.isContextFreeGrammar(g)) gType = "context_free";
            sb.append("\"grammarType\":\"").append(gType).append("\",");

            sb.append("\"productions\":[");
            Production[] prods = g.getProductions();
            for (int i = 0; i < prods.length; i++) {
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"left\":\"").append(escapeJson(prods[i].getLHS())).append("\",");
                sb.append("\"right\":\"").append(escapeJson(prods[i].getRHS())).append("\"");
                sb.append("}");
            }
            sb.append("]");
        } else {
            sb.append("\"category\":\"other\",");
            sb.append("\"class\":\"").append(escapeJson(obj.getClass().getName())).append("\"");
        }

        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // Simulate
    // -------------------------------------------------------------
    private static void simulate(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Usage: simulate <file.jff> [--acceptance final_state|empty_stack] [input1 input2 ...]");
            System.exit(1);
        }
        File file = new File(args[1]);
        if (!file.exists()) {
            printError("File not found: " + file.getAbsolutePath());
            System.exit(1);
        }

        Serializable obj = decodeFile(file);
        if (!(obj instanceof Automaton)) {
            printError("File is not an automaton: " + obj.getClass().getName());
            System.exit(1);
        }

        Automaton auto = (Automaton) obj;
        String explicitAcceptance = null;
        List<String> inputs = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            if ("--acceptance".equalsIgnoreCase(args[i]) || "--acceptance-mode".equalsIgnoreCase(args[i]) || "-a".equalsIgnoreCase(args[i])) {
                if (i + 1 < args.length) {
                    explicitAcceptance = args[++i];
                }
            } else if (args[i].startsWith("--acceptance=")) {
                explicitAcceptance = args[i].substring("--acceptance=".length());
            } else if (args[i].startsWith("--acceptance-mode=")) {
                explicitAcceptance = args[i].substring("--acceptance-mode=".length());
            } else {
                inputs.add(args[i]);
            }
        }

        String pdaMode = null;
        if (auto instanceof PushdownAutomaton) {
            pdaMode = resolvePdaAcceptanceMode(file, explicitAcceptance);
        }

        AutomatonSimulator sim = getAutomatonSimulator(auto, pdaMode);
        if (inputs.isEmpty()) {
            inputs.add(""); // Empty string simulation
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"file\":\"").append(escapeJson(file.getAbsolutePath())).append("\",");
        if (pdaMode != null) {
            sb.append("\"acceptanceMode\":\"").append(pdaMode).append("\",");
        }
        sb.append("\"results\":[");

        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            if (i > 0) sb.append(",");
            boolean accepted = false;
            try {
                accepted = runSimulation(sim, auto, input, 10000, pdaMode);
            } catch (Exception e) {
                accepted = false;
            }

            sb.append("{");
            sb.append("\"input\":\"").append(escapeJson(input)).append("\",");
            sb.append("\"accepted\":").append(accepted);

            // Special outputs for Mealy / Moore / TM
            if (auto instanceof MealyMachine || auto instanceof MooreMachine) {
                // Find accept or terminal configuration output
                try {
                    Configuration[] configs = sim.getInitialConfigurations(input);
                    String lastOutput = "";
                    if (configs != null && configs.length > 0) {
                        Configuration curr = configs[0];
                        for (int step = 0; step < 1000; step++) {
                            ArrayList<Configuration> next = sim.stepConfiguration(curr);
                            if (next == null || next.isEmpty()) break;
                            curr = next.get(0);
                            if (curr instanceof MealyConfiguration) {
                                lastOutput = ((MealyConfiguration) curr).getOutput();
                            }
                        }
                    }
                    sb.append(",\"output\":\"").append(escapeJson(lastOutput)).append("\"");
                } catch (Exception ignored) {}
            } else if (auto instanceof TuringMachine) {
                try {
                    Configuration[] configs = sim.getInitialConfigurations(input);
                    String tapeOutput = "";
                    if (configs != null && configs.length > 0) {
                        Configuration curr = configs[0];
                        for (int step = 0; step < 1000; step++) {
                            ArrayList<Configuration> next = sim.stepConfiguration(curr);
                            if (next == null || next.isEmpty()) break;
                            curr = next.get(0);
                            if (curr.isAccept()) break;
                        }
                        if (curr instanceof TMConfiguration) {
                            Tape[] tapes = ((TMConfiguration) curr).getTapes();
                            if (tapes != null && tapes.length > 0) {
                                tapeOutput = tapes[0].getContents();
                            }
                        }
                    }
                    sb.append(",\"finalTape\":\"").append(escapeJson(tapeOutput)).append("\"");
                } catch (Exception ignored) {}
            }

            sb.append("}");
        }

        sb.append("]}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // Trace
    // -------------------------------------------------------------
    private static void trace(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Usage: trace <file.jff> [input] [--acceptance final_state|empty_stack]");
            System.exit(1);
        }
        File file = new File(args[1]);
        String input = "";
        String explicitAcceptance = null;
        for (int i = 2; i < args.length; i++) {
            if ("--acceptance".equalsIgnoreCase(args[i]) || "--acceptance-mode".equalsIgnoreCase(args[i]) || "-a".equalsIgnoreCase(args[i])) {
                if (i + 1 < args.length) {
                    explicitAcceptance = args[++i];
                }
            } else if (args[i].startsWith("--acceptance=")) {
                explicitAcceptance = args[i].substring("--acceptance=".length());
            } else if (args[i].startsWith("--acceptance-mode=")) {
                explicitAcceptance = args[i].substring("--acceptance-mode=".length());
            } else {
                input = args[i];
            }
        }

        Serializable obj = decodeFile(file);
        if (!(obj instanceof Automaton)) {
            printError("File is not an automaton: " + obj.getClass().getName());
            System.exit(1);
        }

        Automaton auto = (Automaton) obj;
        String pdaMode = null;
        if (auto instanceof PushdownAutomaton) {
            pdaMode = resolvePdaAcceptanceMode(file, explicitAcceptance);
        }
        AutomatonSimulator sim = getAutomatonSimulator(auto, pdaMode);

        Configuration[] initial = sim.getInitialConfigurations(input);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"file\":\"").append(escapeJson(file.getAbsolutePath())).append("\",");
        sb.append("\"input\":\"").append(escapeJson(input)).append("\",");
        if (pdaMode != null) {
            sb.append("\"acceptanceMode\":\"").append(pdaMode).append("\",");
        }

        if (initial == null || initial.length == 0) {
            sb.append("\"accepted\":false,");
            sb.append("\"steps\":[]");
            sb.append("}");
            System.out.println(sb.toString());
            return;
        }

        // BFS / DFS trace to first accept or termination
        List<Configuration> path = new ArrayList<>();
        boolean accepted = false;

        Queue<List<Configuration>> queue = new LinkedList<>();
        List<Configuration> startPath = new ArrayList<>();
        startPath.add(initial[0]);
        queue.add(startPath);

        int maxExpansions = 500;
        int expansions = 0;
        List<Configuration> lastPath = startPath;

        while (!queue.isEmpty() && expansions < maxExpansions) {
            expansions++;
            List<Configuration> currentPath = queue.poll();
            Configuration current = currentPath.get(currentPath.size() - 1);
            lastPath = currentPath;

            if (isAcceptingConfig(current, auto, pdaMode)) {
                accepted = true;
                path = currentPath;
                break;
            }

            ArrayList<Configuration> nextList = sim.stepConfiguration(current);
            if (nextList != null) {
                for (Configuration next : nextList) {
                    List<Configuration> np = new ArrayList<>(currentPath);
                    np.add(next);
                    queue.add(np);
                }
            }
        }

        if (path.isEmpty()) {
            path = lastPath;
        }

        sb.append("\"accepted\":").append(accepted).append(",");
        sb.append("\"steps\":[");
        for (int i = 0; i < path.size(); i++) {
            Configuration c = path.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"step\":").append(i).append(",");
            sb.append("\"state\":").append(c.getCurrentState() != null ? c.getCurrentState().getID() : -1).append(",");
            sb.append("\"stateName\":\"").append(c.getCurrentState() != null ? escapeJson(c.getCurrentState().getName()) : "").append("\",");
            sb.append("\"description\":\"").append(escapeJson(c.toString())).append("\",");
            sb.append("\"isAccept\":").append(isAcceptingConfig(c, auto, pdaMode));

            if (c instanceof FSAConfiguration) {
                FSAConfiguration fc = (FSAConfiguration) c;
                sb.append(",\"unprocessedInput\":\"").append(escapeJson(fc.getUnprocessedInput())).append("\"");
            } else if (c instanceof PDAConfiguration) {
                PDAConfiguration pc = (PDAConfiguration) c;
                sb.append(",\"unprocessedInput\":\"").append(escapeJson(pc.getUnprocessedInput())).append("\",");
                sb.append("\"stack\":\"").append(escapeJson(pc.getStack() != null ? pc.getStack().toString() : "")).append("\"");
            } else if (c instanceof TMConfiguration) {
                TMConfiguration tc = (TMConfiguration) c;
                Tape[] tapes = tc.getTapes();
                sb.append(",\"tape\":\"").append(tapes != null && tapes.length > 0 ? escapeJson(tapes[0].getContents()) : "").append("\"");
            }

            sb.append("}");
        }
        sb.append("]}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // NFA to DFA
    // -------------------------------------------------------------
    private static void nfaToDfa(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: nfa_to_dfa <input.jff> <output.jff>");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        File outFile = new File(args[2]);

        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof FiniteStateAutomaton)) {
            printError("Input file is not an FSA: " + inFile.getAbsolutePath());
            System.exit(1);
        }

        FiniteStateAutomaton fsa = (FiniteStateAutomaton) obj;
        int origStates = fsa.getStates().length;
        int origTransitions = fsa.getTransitions().length;

        NFAToDFA converter = new NFAToDFA();
        FiniteStateAutomaton dfa = converter.convertToDFA(fsa);

        CODEC.encode(dfa, outFile, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"originalStates\":").append(origStates).append(",");
        sb.append("\"originalTransitions\":").append(origTransitions).append(",");
        sb.append("\"dfaStates\":").append(dfa.getStates().length).append(",");
        sb.append("\"dfaTransitions\":").append(dfa.getTransitions().length).append(",");
        sb.append("\"outputFile\":\"").append(escapeJson(outFile.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // Minimize DFA
    // -------------------------------------------------------------
    private static void minimize(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: minimize <input.jff> <output.jff>");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        File outFile = new File(args[2]);

        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof FiniteStateAutomaton)) {
            printError("Input file is not an FSA: " + inFile.getAbsolutePath());
            System.exit(1);
        }

        FiniteStateAutomaton fsa = (FiniteStateAutomaton) obj;
        // Make sure it is deterministic first
        NFAToDFA nfaConverter = new NFAToDFA();
        FiniteStateAutomaton dfa = nfaConverter.convertToDFA(fsa);

        Minimizer minimizer = new Minimizer();
        minimizer.initializeMinimizer();
        Automaton minable = minimizer.getMinimizeableAutomaton(dfa);
        DefaultTreeModel tree = minimizer.getDistinguishableGroupsTree(minable);
        FiniteStateAutomaton minDfa = minimizer.getMinimumDfa(minable, tree);

        CODEC.encode(minDfa, outFile, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"originalStates\":").append(fsa.getStates().length).append(",");
        sb.append("\"minimizedStates\":").append(minDfa.getStates().length).append(",");
        sb.append("\"minimizedTransitions\":").append(minDfa.getTransitions().length).append(",");
        sb.append("\"outputFile\":\"").append(escapeJson(outFile.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // Export Transition Table (DFA/NFA)
    // -------------------------------------------------------------
    private static void exportTable(String[] args) throws Exception {
        if (args.length < 2) {
            throw new BridgeException("invalid_argument", "Usage: export_table <file.jff>");
        }
        File file = new File(args[1]);
        if (!file.exists()) {
            throw new BridgeException("file_not_found", "File not found: " + file.getAbsolutePath());
        }
        Serializable obj;
        try {
            obj = decodeFile(file);
        } catch (Exception e) {
            throw new BridgeException("malformed_model", "Failed to parse model file: " + e.getMessage());
        }
        if (!(obj instanceof FiniteStateAutomaton)) {
            String typeStr = "unknown";
            if (obj instanceof PushdownAutomaton) typeStr = "pda";
            else if (obj instanceof TuringMachine) typeStr = "turing";
            else if (obj instanceof Grammar) typeStr = "grammar";
            else if (obj != null) typeStr = obj.getClass().getSimpleName();
            throw new BridgeException("unsupported_model_type",
                "Cannot export transition table: model is of type '" + typeStr + "'. Only DFA/NFA finite state automata are supported.");
        }

        FiniteStateAutomaton fsa = (FiniteStateAutomaton) obj;
        Set<String> alphabetSet = new TreeSet<>();
        List<String> stateNames = new ArrayList<>();
        List<String> acceptingStates = new ArrayList<>();
        State init = fsa.getInitialState();
        String startState = init != null ? getStateName(init) : "";

        for (State s : fsa.getStates()) {
            String name = getStateName(s);
            stateNames.add(name);
            if (fsa.isFinalState(s)) {
                acceptingStates.add(name);
            }
        }

        boolean isDeterministic = true;
        try {
            NondeterminismDetector d = NondeterminismDetectorFactory.getDetector(fsa);
            if (d != null) {
                State[] nd = d.getNondeterministicStates(fsa);
                if (nd.length > 0) isDeterministic = false;
            }
        } catch (Exception ignored) {}

        List<Map<String, String>> transitionsList = new ArrayList<>();
        for (Transition t : fsa.getTransitions()) {
            FSATransition ft = (FSATransition) t;
            String lbl = ft.getLabel();
            if (lbl == null) lbl = "";
            if (!lbl.isEmpty()) {
                alphabetSet.add(lbl);
            }
            Map<String, String> tr = new LinkedHashMap<>();
            tr.put("from", getStateName(ft.getFromState()));
            tr.put("symbol", lbl);
            tr.put("to", getStateName(ft.getToState()));
            transitionsList.add(tr);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(escapeJson(file.getAbsolutePath())).append("\",");
        sb.append("\"type\":\"fa\",");
        sb.append("\"is_deterministic\":").append(isDeterministic).append(",");

        sb.append("\"states\":[");
        for (int i = 0; i < stateNames.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(stateNames.get(i))).append("\"");
        }
        sb.append("],");

        sb.append("\"alphabet\":[");
        int ai = 0;
        for (String a : alphabetSet) {
            if (ai++ > 0) sb.append(",");
            sb.append("\"").append(escapeJson(a)).append("\"");
        }
        sb.append("],");

        sb.append("\"start_state\":\"").append(escapeJson(startState)).append("\",");

        sb.append("\"accepting_states\":[");
        for (int i = 0; i < acceptingStates.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(acceptingStates.get(i))).append("\"");
        }
        sb.append("],");

        sb.append("\"transitions\":[");
        for (int i = 0; i < transitionsList.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, String> tr = transitionsList.get(i);
            sb.append("{");
            sb.append("\"from\":\"").append(escapeJson(tr.get("from"))).append("\",");
            sb.append("\"symbol\":\"").append(escapeJson(tr.get("symbol"))).append("\",");
            sb.append("\"to\":\"").append(escapeJson(tr.get("to"))).append("\"");
            sb.append("}");
        }
        sb.append("]");

        sb.append("}");
        System.out.println(sb.toString());
        System.out.flush();
    }

    private static String getStateName(State s) {
        if (s == null) return "";
        if (s.getName() != null && !s.getName().trim().isEmpty()) {
            return s.getName().trim();
        }
        return "q" + s.getID();
    }

    // -------------------------------------------------------------
    // FSA Equivalence Check (with shortest distinguishing string)
    // -------------------------------------------------------------
    private static void fsaEquals(String[] args) throws Exception {
        equivalenceCheck(args);
    }

    private static void equivalenceCheck(String[] args) throws Exception {
        if (args.length < 3) {
            throw new BridgeException("invalid_argument", "Usage: equivalence_check <modelA.jff> <modelB.jff>");
        }
        File f1 = new File(args[1]);
        File f2 = new File(args[2]);

        if (!f1.exists()) {
            throw new BridgeException("file_not_found", "Model A not found: " + f1.getAbsolutePath());
        }
        if (!f2.exists()) {
            throw new BridgeException("file_not_found", "Model B not found: " + f2.getAbsolutePath());
        }

        Serializable o1, o2;
        try {
            o1 = decodeFile(f1);
        } catch (Exception e) {
            throw new BridgeException("malformed_model", "Failed to parse Model A: " + e.getMessage());
        }
        try {
            o2 = decodeFile(f2);
        } catch (Exception e) {
            throw new BridgeException("malformed_model", "Failed to parse Model B: " + e.getMessage());
        }

        if (!(o1 instanceof FiniteStateAutomaton) || !(o2 instanceof FiniteStateAutomaton)) {
            String t1 = o1 instanceof Automaton ? ((Automaton)o1).getClass().getSimpleName() : "non-automaton";
            String t2 = o2 instanceof Automaton ? ((Automaton)o2).getClass().getSimpleName() : "non-automaton";
            throw new BridgeException("unsupported_model_type",
                "Equivalence check is only supported for Finite State Automata. Model A is " + t1 + ", Model B is " + t2 + ".");
        }

        FiniteStateAutomaton fsa1 = (FiniteStateAutomaton) o1;
        FiniteStateAutomaton fsa2 = (FiniteStateAutomaton) o2;

        // Convert both to DFA
        NFAToDFA converter = new NFAToDFA();
        FiniteStateAutomaton dfa1 = converter.convertToDFA(fsa1);
        FiniteStateAutomaton dfa2 = converter.convertToDFA(fsa2);

        // Collect combined alphabet
        Set<String> alphabet = new TreeSet<>();
        for (Transition t : dfa1.getTransitions()) {
            String l = ((FSATransition) t).getLabel();
            if (l != null && !l.isEmpty()) alphabet.add(l);
        }
        for (Transition t : dfa2.getTransitions()) {
            String l = ((FSATransition) t).getLabel();
            if (l != null && !l.isEmpty()) alphabet.add(l);
        }

        // BFS on product automaton to find shortest distinguishing string
        State init1 = dfa1.getInitialState();
        State init2 = dfa2.getInitialState();
        int s1 = init1 != null ? init1.getID() : -1;
        int s2 = init2 != null ? init2.getID() : -1;

        Queue<Pair> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        Map<String, String> stringForPair = new HashMap<>();

        Pair startPair = new Pair(s1, s2);
        queue.add(startPair);
        visited.add(startPair.key());
        stringForPair.put(startPair.key(), "");

        boolean equivalent = true;
        String shortestDistinguishing = null;
        boolean aAccepts = false;
        boolean bAccepts = false;

        while (!queue.isEmpty()) {
            Pair cur = queue.poll();
            String curStr = stringForPair.get(cur.key());

            boolean acc1 = (cur.a != -1) && isStateFinal(dfa1, cur.a);
            boolean acc2 = (cur.b != -1) && isStateFinal(dfa2, cur.b);

            if (acc1 != acc2) {
                equivalent = false;
                shortestDistinguishing = curStr;
                aAccepts = acc1;
                bAccepts = acc2;
                break;
            }

            for (String sym : alphabet) {
                int nextA = stepDfa(dfa1, cur.a, sym);
                int nextB = stepDfa(dfa2, cur.b, sym);
                Pair nextPair = new Pair(nextA, nextB);
                if (!visited.contains(nextPair.key())) {
                    visited.add(nextPair.key());
                    stringForPair.put(nextPair.key(), curStr + sym);
                    queue.add(nextPair);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"equivalent\":").append(equivalent).append(",");
        sb.append("\"file1\":\"").append(escapeJson(f1.getAbsolutePath())).append("\",");
        sb.append("\"file2\":\"").append(escapeJson(f2.getAbsolutePath())).append("\",");
        if (shortestDistinguishing != null) {
            sb.append("\"shortest_distinguishing_string\":\"").append(escapeJson(shortestDistinguishing)).append("\",");
            sb.append("\"modelA_accepts\":").append(aAccepts).append(",");
            sb.append("\"modelB_accepts\":").append(bAccepts);
        } else {
            sb.append("\"shortest_distinguishing_string\":null");
        }
        sb.append("}");
        System.out.println(sb.toString());
        System.out.flush();
    }

    private static boolean isStateFinal(FiniteStateAutomaton fsa, int id) {
        if (id == -1) return false;
        for (State s : fsa.getFinalStates()) {
            if (s.getID() == id) return true;
        }
        return false;
    }

    private static int stepDfa(FiniteStateAutomaton dfa, int stateId, String sym) {
        if (stateId == -1) return -1;
        State s = null;
        for (State st : dfa.getStates()) {
            if (st.getID() == stateId) { s = st; break; }
        }
        if (s == null) return -1;
        for (Transition t : dfa.getTransitionsFromState(s)) {
            FSATransition ft = (FSATransition) t;
            if (sym.equals(ft.getLabel())) {
                return ft.getToState().getID();
            }
        }
        return -1;
    }

    static class Pair {
        final int a;
        final int b;
        Pair(int a, int b) { this.a = a; this.b = b; }
        String key() { return a + "," + b; }
    }

    // -------------------------------------------------------------
    // Grammar Parse
    // -------------------------------------------------------------
    private static void grammarParse(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: grammar_parse <file.jff> <input>");
            System.exit(1);
        }
        File file = new File(args[1]);
        String input = args[2];

        Serializable obj = decodeFile(file);
        if (!(obj instanceof Grammar)) {
            printError("File is not a grammar: " + obj.getClass().getName());
            System.exit(1);
        }

        Grammar g = (Grammar) obj;
        boolean accepted = false;
        String parserUsed = "BruteParser";
        List<String> derivation = new ArrayList<>();

        // If in CNF, try CYK first
        boolean isCnf = true;
        Production[] prods = g.getProductions();
        for (Production p : prods) {
            String rhs = p.getRHS();
            if (rhs.length() == 1) {
                if (!g.isTerminal(rhs)) { isCnf = false; break; }
            } else if (rhs.length() == 2) {
                if (g.isTerminal(rhs.substring(0, 1)) || g.isTerminal(rhs.substring(1, 2))) {
                    isCnf = false; break;
                }
            } else {
                isCnf = false; break;
            }
        }

        if (isCnf && !input.isEmpty()) {
            parserUsed = "CYKParser";
            try {
                CYKParser cyk = new CYKParser(g);
                accepted = cyk.solve(input);
                if (accepted && cyk.getTrace() != null) {
                    for (Production p : cyk.getTrace()) {
                        derivation.add(p.toString());
                    }
                }
            } catch (Exception e) {
                isCnf = false;
            }
        }

        if (!isCnf) {
            parserUsed = "BruteParser";
            BruteParser bp = BruteParser.get(g, input);
            bp.start();
            long start = System.currentTimeMillis();
            while (!bp.isFinished() && (System.currentTimeMillis() - start < 5000)) {
                Thread.sleep(50);
            }
            if (!bp.isFinished()) {
                bp.pause();
            }
            ParseNode node = bp.getAnswer();
            if (node != null) {
                accepted = true;
                derivation.add(node.getDerivation());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"input\":\"").append(escapeJson(input)).append("\",");
        sb.append("\"accepted\":").append(accepted).append(",");
        sb.append("\"parser\":\"").append(parserUsed).append("\",");
        sb.append("\"derivation\":[");
        for (int i = 0; i < derivation.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(derivation.get(i))).append("\"");
        }
        sb.append("]}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // Grammar Clean (Lambda, Unit, Useless)
    // -------------------------------------------------------------
    private static void grammarClean(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: grammar_clean <input.jff> <output.jff>");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        File outFile = new File(args[2]);

        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof Grammar)) {
            printError("Input file is not a grammar: " + inFile.getAbsolutePath());
            System.exit(1);
        }

        Grammar g = (Grammar) obj;
        LambdaProductionRemover lpr = new LambdaProductionRemover();
        Grammar g1 = lpr.getLambdaProductionlessGrammar(g, lpr.getCompleteLambdaSet(g));

        UnitProductionRemover upr = new UnitProductionRemover();
        Grammar g2 = upr.getUnitProductionlessGrammar(g1, upr.getVariableDependencyGraph(g1));

        Grammar clean = UselessProductionRemover.getUselessProductionlessGrammar(g2);
        CODEC.encode(clean, outFile, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"originalProductions\":").append(g.getProductions().length).append(",");
        sb.append("\"cleanedProductions\":").append(clean.getProductions().length).append(",");
        sb.append("\"outputFile\":\"").append(escapeJson(outFile.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // FSA to Regular Grammar
    // -------------------------------------------------------------
    private static void fsaToRg(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: fsa_to_rg <input.jff> <output.jff>");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        File outFile = new File(args[2]);

        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof FiniteStateAutomaton)) {
            printError("Input file is not an FSA.");
            System.exit(1);
        }

        FiniteStateAutomaton fsa = (FiniteStateAutomaton) obj;
        FSAToRegularGrammarConverter conv = new FSAToRegularGrammarConverter();
        conv.initializeConverter(fsa);
        RegularGrammar rg = conv.convertToRegularGrammar(fsa);

        CODEC.encode(rg, outFile, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"productions\":").append(rg.getProductions().length).append(",");
        sb.append("\"outputFile\":\"").append(escapeJson(outFile.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // Regular Grammar to FSA
    // -------------------------------------------------------------
    private static void rgToFsa(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: rg_to_fsa <input.jff> <output.jff>");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        File outFile = new File(args[2]);

        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof Grammar)) {
            printError("Input file is not a grammar.");
            System.exit(1);
        }

        Grammar g = (Grammar) obj;
        RightLinearGrammarToFSAConverter conv = new RightLinearGrammarToFSAConverter();
        Automaton auto = conv.convertToAutomaton(g);
        FiniteStateAutomaton fsa = toFSA(auto);

        CODEC.encode(fsa, outFile, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"states\":").append(fsa.getStates().length).append(",");
        sb.append("\"transitions\":").append(fsa.getTransitions().length).append(",");
        sb.append("\"outputFile\":\"").append(escapeJson(outFile.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // CFG to PDA
    // -------------------------------------------------------------
    private static void cfgToPda(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: cfg_to_pda <input.jff> <output.jff> [ll|lr]");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        File outFile = new File(args[2]);
        String mode = args.length > 3 ? args[3].toLowerCase() : "ll";

        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof Grammar)) {
            printError("Input file is not a grammar.");
            System.exit(1);
        }

        Grammar g = (Grammar) obj;
        PushdownAutomaton pda = new PushdownAutomaton();
        GrammarToAutomatonConverter conv = mode.equals("lr") ? new CFGToPDALRConverter() : new CFGToPDALLConverter();
        conv.createStatesForConversion(g, pda);
        Production[] prods = g.getProductions();
        for (int i = 0; i < prods.length; i++) {
            pda.addTransition(conv.getTransitionForProduction(prods[i]));
        }

        CODEC.encode(pda, outFile, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"mode\":\"").append(mode).append("\",");
        sb.append("\"states\":").append(pda.getStates().length).append(",");
        sb.append("\"transitions\":").append(pda.getTransitions().length).append(",");
        sb.append("\"outputFile\":\"").append(escapeJson(outFile.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    private static FiniteStateAutomaton toFSA(Automaton auto) {
        if (auto instanceof FiniteStateAutomaton) return (FiniteStateAutomaton) auto;
        FiniteStateAutomaton fsa = new FiniteStateAutomaton();
        Map<State, State> stateMap = new HashMap<>();
        for (State s : auto.getStates()) {
            State ns = fsa.createStateWithId(s.getPoint() != null ? s.getPoint() : new Point(0, 0), s.getID());
            ns.setName(s.getName());
            stateMap.put(s, ns);
        }
        if (auto.getInitialState() != null) {
            fsa.setInitialState(stateMap.get(auto.getInitialState()));
        }
        for (State s : auto.getFinalStates()) {
            fsa.addFinalState(stateMap.get(s));
        }
        for (Transition t : auto.getTransitions()) {
            String label = "";
            if (t instanceof FSATransition) {
                label = ((FSATransition) t).getLabel();
            } else {
                label = t.getDescription();
            }
            fsa.addTransition(new FSATransition(stateMap.get(t.getFromState()), stateMap.get(t.getToState()), label));
        }
        return fsa;
    }

    private static PushdownAutomaton toPDA(Automaton auto) {
        if (auto instanceof PushdownAutomaton) return (PushdownAutomaton) auto;
        PushdownAutomaton pda = new PushdownAutomaton();
        Map<State, State> stateMap = new HashMap<>();
        for (State s : auto.getStates()) {
            State ns = pda.createStateWithId(s.getPoint() != null ? s.getPoint() : new Point(0, 0), s.getID());
            ns.setName(s.getName());
            stateMap.put(s, ns);
        }
        if (auto.getInitialState() != null) {
            pda.setInitialState(stateMap.get(auto.getInitialState()));
        }
        for (State s : auto.getFinalStates()) {
            pda.addFinalState(stateMap.get(s));
        }
        for (Transition t : auto.getTransitions()) {
            if (t instanceof PDATransition) {
                PDATransition pt = (PDATransition) t;
                pda.addTransition(new PDATransition(stateMap.get(t.getFromState()), stateMap.get(t.getToState()),
                    pt.getInputToRead(), pt.getStringToPop(), pt.getStringToPush()));
            }
        }
        return pda;
    }

    // -------------------------------------------------------------
    // PDA to CFG
    // -------------------------------------------------------------
    private static void pdaToCfg(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: pda_to_cfg <input.jff> <output.jff>");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        File outFile = new File(args[2]);

        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof PushdownAutomaton)) {
            printError("Input file is not a PDA.");
            System.exit(1);
        }

        PushdownAutomaton pda = (PushdownAutomaton) obj;
        PDAToCFGConverter conv = new PDAToCFGConverter();
        conv.initializeConverter();
        ContextFreeGrammar cfg = conv.convertToContextFreeGrammar(pda);

        CODEC.encode(cfg, outFile, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"productions\":").append(cfg.getProductions().length).append(",");
        sb.append("\"outputFile\":\"").append(escapeJson(outFile.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // Export to Graphviz DOT
    // -------------------------------------------------------------
    private static void toDot(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Usage: to_dot <file.jff>");
            System.exit(1);
        }
        File file = new File(args[1]);
        Serializable obj = decodeFile(file);
        if (!(obj instanceof Automaton)) {
            printError("File is not an automaton: " + obj.getClass().getName());
            System.exit(1);
        }

        Automaton auto = (Automaton) obj;
        StringBuilder dot = new StringBuilder();
        dot.append("digraph Automaton {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [fontname=\"Helvetica\", fontsize=11];\n");
        dot.append("  edge [fontname=\"Helvetica\", fontsize=10];\n\n");

        // Invisible start node
        State init = auto.getInitialState();
        if (init != null) {
            dot.append("  __start [shape=none, label=\"\", width=0, height=0];\n");
            dot.append("  __start -> q").append(init.getID()).append(";\n");
        }

        // States
        for (State s : auto.getStates()) {
            boolean isFinal = auto.isFinalState(s);
            String shape = isFinal ? "doublecircle" : "circle";
            String label = s.getName() != null && !s.getName().isEmpty() ? s.getName() : "q" + s.getID();
            dot.append("  q").append(s.getID())
               .append(" [shape=").append(shape)
               .append(", label=\"").append(label).append("\"];\n");
        }

        dot.append("\n");

        // Group transitions between same (from, to)
        Map<String, List<String>> transMap = new LinkedHashMap<>();
        for (Transition t : auto.getTransitions()) {
            String key = "q" + t.getFromState().getID() + " -> q" + t.getToState().getID();
            String label = "";
            if (t instanceof FSATransition) {
                String lbl = ((FSATransition) t).getLabel();
                label = (lbl == null || lbl.isEmpty()) ? "\u03BB" : lbl;
            } else if (t instanceof PDATransition) {
                PDATransition pt = (PDATransition) t;
                String r = pt.getInputToRead().isEmpty() ? "\u03BB" : pt.getInputToRead();
                String pop = pt.getStringToPop().isEmpty() ? "\u03BB" : pt.getStringToPop();
                String push = pt.getStringToPush().isEmpty() ? "\u03BB" : pt.getStringToPush();
                label = r + ", " + pop + " ; " + push;
            } else if (t instanceof TMTransition) {
                TMTransition tt = (TMTransition) t;
                String r = tt.getRead(0).isEmpty() ? "\u25A1" : tt.getRead(0);
                String w = tt.getWrite(0).isEmpty() ? "\u25A1" : tt.getWrite(0);
                String d = tt.getDirection(0);
                label = r + " ; " + w + ", " + d;
            } else {
                label = t.getDescription();
            }
            transMap.computeIfAbsent(key, k -> new ArrayList<>()).add(label);
        }

        for (Map.Entry<String, List<String>> entry : transMap.entrySet()) {
            String labels = String.join(", ", entry.getValue());
            dot.append("  ").append(entry.getKey()).append(" [label=\"").append(labels).append("\"];\n");
        }

        dot.append("}\n");

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"dot\":\"").append(escapeJson(dot.toString())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    public static String getDeclaredAcceptanceMode(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            Element elem = doc.getDocumentElement();
            NodeList accNodes = elem.getElementsByTagName("acceptance");
            if (accNodes.getLength() == 0) accNodes = elem.getElementsByTagName("acceptby");
            if (accNodes.getLength() == 0) accNodes = elem.getElementsByTagName("accept_by");
            if (accNodes.getLength() > 0) {
                String val = accNodes.item(0).getTextContent().trim().toLowerCase();
                if ("empty_stack".equals(val) || "emptystack".equals(val) || "empty".equals(val)) {
                    return "empty_stack";
                }
                if ("final_state".equals(val) || "finalstate".equals(val) || "final".equals(val)) {
                    return "final_state";
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String resolvePdaAcceptanceMode(File file, String explicitAcceptance) {
        if (explicitAcceptance != null && !explicitAcceptance.isEmpty()) {
            String val = explicitAcceptance.trim().toLowerCase();
            if ("final_state".equals(val) || "finalstate".equals(val) || "final".equals(val)) {
                return "final_state";
            }
            if ("empty_stack".equals(val) || "emptystack".equals(val) || "empty".equals(val)) {
                return "empty_stack";
            }
            printError("Invalid acceptance_mode: '" + explicitAcceptance + "'. Must be 'final_state' or 'empty_stack'.");
            System.exit(1);
        }
        String decl = getDeclaredAcceptanceMode(file);
        if (decl != null) return decl;
        return "final_state";
    }

    private static AutomatonSimulator getAutomatonSimulator(Automaton auto, String pdaAcceptanceMode) {
        if (auto instanceof PushdownAutomaton) {
            int acceptance = "empty_stack".equalsIgnoreCase(pdaAcceptanceMode) ? 0 : 1;
            return new HeadlessPDASimulator(auto, acceptance);
        }
        return SimulatorFactory.getSimulator(auto);
    }

    /*
     * NOTE on PDA acceptance and HeadlessPDASimulator.myAcceptance:
     * In JFLAP's original PDAConfiguration.java:
     *   EMPTY_STACK = 0
     *   FINAL_STATE = 1
     * Earlier code flipped myAcceptance to 1 in HeadlessPDASimulator with the comment
     * "// 1: Final State, 0: Empty Stack". However, because runSimulation() and trace()
     * relied on a custom isAcceptingConfig() method that directly checked both:
     *   (pc.getUnprocessedInput().length() == 0 && (auto.isFinalState(...) || pc.getStack().height() == 0))
     * instead of calling c.isAccept(), changing myAcceptance in HeadlessPDASimulator had NO effect
     * on execution behavior—only the variable value was changed while isAcceptingConfig applied
     * an unsound "either/or" check.
     * Furthermore, JFLAP's built-in PDAConfiguration.isAccept() had a known bug where it checked:
     *   this.getUnprocessedInput() == "" (using reference equality instead of length() == 0),
     * which caused empty-stack checks to fail spuriously when strings were not interned.
     * 
     * To ensure strict correctness and prevent safety failures in the Chimeric Automaton,
     * acceptance is now strictly parameter-driven:
     *   - "final_state": input is fully consumed AND currentState is in F. (Stack contents ignored).
     *   - "empty_stack": input is fully consumed AND stack height == 0. (State finality ignored).
     * Both criteria are NEVER allowed at once.
     */
    private static boolean isAcceptingConfig(Configuration c, Automaton auto, String pdaAcceptanceMode) {
        if (c == null) return false;
        if (c instanceof PDAConfiguration) {
            PDAConfiguration pc = (PDAConfiguration) c;
            if (pc.getUnprocessedInput().length() != 0) {
                return false;
            }
            if ("empty_stack".equalsIgnoreCase(pdaAcceptanceMode)) {
                return pc.getStack() != null && pc.getStack().height() == 0;
            } else {
                // "final_state" (default)
                return auto != null && auto.isFinalState(pc.getCurrentState());
            }
        }
        return c.isAccept();
    }

    public static class HeadlessPDASimulator extends AutomatonSimulator {
        private int myAcceptance = 1; // 1: Final State, 0: Empty Stack

        public HeadlessPDASimulator(Automaton automaton) {
            super(automaton);
        }

        public HeadlessPDASimulator(Automaton automaton, int acceptance) {
            super(automaton);
            this.myAcceptance = acceptance;
        }

        public Configuration[] getInitialConfigurations(String input) {
            Configuration[] configs = new Configuration[1];
            CharacterStack stack = new CharacterStack();
            stack.push("Z");
            configs[0] = new PDAConfiguration(myAutomaton.getInitialState(), null,
                    input, input, stack, myAcceptance);
            return configs;
        }

        public ArrayList<Configuration> stepConfiguration(Configuration config) {
            ArrayList<Configuration> list = new ArrayList<Configuration>();
            PDAConfiguration configuration = (PDAConfiguration) config;
            String unprocessedInput = configuration.getUnprocessedInput();
            String totalInput = configuration.getInput();
            State currentState = configuration.getCurrentState();
            Transition[] transitions = myAutomaton.getTransitionsFromState(currentState);
            for (int k = 0; k < transitions.length; k++) {
                PDATransition transition = (PDATransition) transitions[k];
                String inputToRead = transition.getInputToRead();
                String stringToPop = transition.getStringToPop();
                CharacterStack tempStack = configuration.getStack();
                CharacterStack stack = new CharacterStack(tempStack);
                String stackContents = stack.pop(stringToPop.length());
                if (unprocessedInput.startsWith(inputToRead)
                        && stringToPop.equals(stackContents)) {
                    String input = "";
                    if (inputToRead.length() < unprocessedInput.length()) {
                        input = unprocessedInput.substring(inputToRead.length());
                    }
                    State toState = transition.getToState();
                    stack.push(transition.getStringToPush());
                    PDAConfiguration configurationToAdd = new PDAConfiguration(
                            toState, configuration, totalInput, input, stack, myAcceptance);
                    list.add(configurationToAdd);
                }
            }
            return list;
        }

        public boolean isAccepted() {
            Iterator<Configuration> it = myConfigurations.iterator();
            while (it.hasNext()) {
                PDAConfiguration configuration = (PDAConfiguration) it.next();
                if (configuration.isAccept()) return true;
            }
            return false;
        }

        public boolean simulateInput(String input) {
            return false;
        }
    }

    private static boolean runSimulation(AutomatonSimulator sim, Automaton auto, String input, int maxSteps, String pdaAcceptanceMode) {
        Configuration[] initial = sim.getInitialConfigurations(input);
        if (initial == null || initial.length == 0) return false;

        Set<String> visited = new HashSet<>();
        Queue<Configuration> queue = new LinkedList<>();
        for (Configuration c : initial) {
            if (isAcceptingConfig(c, auto, pdaAcceptanceMode)) return true;
            queue.add(c);
            visited.add(c.toString());
        }

        int steps = 0;
        while (!queue.isEmpty() && steps < maxSteps) {
            Configuration curr = queue.poll();
            steps++;
            if (isAcceptingConfig(curr, auto, pdaAcceptanceMode)) return true;

            // Stack depth protection for PDA
            if (curr instanceof PDAConfiguration) {
                PDAConfiguration pc = (PDAConfiguration) curr;
                if (pc.getStack() != null && pc.getStack().height() > 50) {
                    continue;
                }
            }

            ArrayList<Configuration> nextList = sim.stepConfiguration(curr);
            if (nextList != null) {
                for (Configuration next : nextList) {
                    if (isAcceptingConfig(next, auto, pdaAcceptanceMode)) return true;
                    String repr = next.toString();
                    if (!visited.contains(repr)) {
                        visited.add(repr);
                        queue.add(next);
                    }
                }
            }
        }
        return false;
    }

    public static Serializable decodeFile(File file) throws Exception {
        if (!file.exists()) {
            throw new BridgeException("file_not_found", "File not found: " + file.getAbsolutePath());
        }
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(file);
        } catch (Exception e) {
            throw new BridgeException("malformed_model", "Failed to parse XML in model file '" + file.getName() + "': " + e.getMessage());
        }

        try {
            Element elem = doc.getDocumentElement();
            NodeList typeNodes = elem.getElementsByTagName("type");
            if (typeNodes.getLength() > 0) {
                String type = typeNodes.item(0).getTextContent().trim();
                if ("turing".equals(type)) {
                    NodeList bbNodes = elem.getElementsByTagName("block");
                    if (bbNodes.getLength() > 0) {
                        return new TMBBTransducer().fromDOM(doc);
                    } else {
                        return new TMTransducer().fromDOM(doc);
                    }
                }
            }
            Transducer transducer = TransducerFactory.getTransducer(doc);
            if (transducer == null) {
                throw new BridgeException("malformed_model", "Unknown or unsupported model structure in '" + file.getName() + "'");
            }
            return transducer.fromDOM(doc);
        } catch (BridgeException be) {
            throw be;
        } catch (Exception e) {
            throw new BridgeException("malformed_model", "Malformed automaton/grammar definition: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Helper: JSON escaping & error output
    // -------------------------------------------------------------
    
    // -------------------------------------------------------------
    // FSA to Regular Expression
    // -------------------------------------------------------------
    private static void fsaToRe(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Usage: fsa_to_re <input.jff> [output.jff]");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof FiniteStateAutomaton)) {
            printError("Input file is not an FSA.");
            System.exit(1);
        }
        FiniteStateAutomaton fsa = (FiniteStateAutomaton) ((FiniteStateAutomaton) obj).clone();
        FSAToRegularExpressionConverter.convertToSimpleAutomaton(fsa);
        String re = FSAToRegularExpressionConverter.convertToRegularExpression(fsa);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"regularExpression\":\"").append(escapeJson(re)).append("\",");
        sb.append("\"inputFile\":\"").append(escapeJson(inFile.getAbsolutePath())).append("\"");

        if (args.length > 2) {
            File outFile = new File(args[2]);
            RegularExpression regExpr = new RegularExpression(re);
            CODEC.encode(regExpr, outFile, null);
            sb.append(",\"outputFile\":\"").append(escapeJson(outFile.getAbsolutePath())).append("\"");
        }
        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // FIRST & FOLLOW Sets + LL(1) Check
    // -------------------------------------------------------------
    private static void firstFollow(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Usage: first_follow <grammar.jff>");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof Grammar)) {
            printError("Input file is not a grammar.");
            System.exit(1);
        }
        Grammar g = (Grammar) obj;
        Map<String, Set<String>> first = Operations.first(g);
        Map<String, Set<String>> follow = Operations.follow(g);
        boolean isLL1 = false;
        try {
            isLL1 = Operations.isLL1(g);
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"isLL1\":").append(isLL1).append(",");
        sb.append("\"first\":{");
        int count = 0;
        for (Map.Entry<String, Set<String>> e : first.entrySet()) {
            if (count++ > 0) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":[");
            int sc = 0;
            for (String s : e.getValue()) {
                if (sc++ > 0) sb.append(",");
                sb.append("\"").append(escapeJson(s)).append("\"");
            }
            sb.append("]");
        }
        sb.append("},");

        sb.append("\"follow\":{");
        count = 0;
        for (Map.Entry<String, Set<String>> e : follow.entrySet()) {
            if (count++ > 0) sb.append(",");
            sb.append("\"").append(escapeJson(e.getKey())).append("\":[");
            int sc = 0;
            for (String s : e.getValue()) {
                if (sc++ > 0) sb.append(",");
                sb.append("\"").append(escapeJson(s)).append("\"");
            }
            sb.append("]");
        }
        sb.append("}}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // L-System Expand
    // -------------------------------------------------------------
    private static void lsystemExpand(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Usage: lsystem_expand <lsystem.jff> [level]");
            System.exit(1);
        }
        File inFile = new File(args[1]);
        int level = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        Serializable obj = decodeFile(inFile);
        if (!(obj instanceof LSystem)) {
            printError("Input file is not an L-System.");
            System.exit(1);
        }
        LSystem ls = (LSystem) obj;
        Expander exp = new Expander(ls);
        List<String> tokens = exp.expansionForLevel(level);
        String expanded = String.join("", tokens);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"level\":").append(level).append(",");
        sb.append("\"length\":").append(tokens.size()).append(",");
        sb.append("\"expandedString\":\"").append(escapeJson(expanded)).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    // -------------------------------------------------------------
    // Combine Two Automata
    // -------------------------------------------------------------
    private static void combineAutomata(String[] args) throws Exception {
        if (args.length < 4) {
            printError("Usage: combine <input1.jff> <input2.jff> <output.jff>");
            System.exit(1);
        }
        File f1 = new File(args[1]);
        File f2 = new File(args[2]);
        File out = new File(args[3]);

        Serializable o1 = decodeFile(f1);
        Serializable o2 = decodeFile(f2);
        if (!(o1 instanceof Automaton) || !(o2 instanceof Automaton)) {
            printError("Both files must be automata.");
            System.exit(1);
        }

        Automaton a1 = (Automaton) o1;
        Automaton a2 = (Automaton) o2;
        Automaton newOne = (Automaton) a1.clone();
        int d = 250;
        Map<State, State> otherToNew = new HashMap<>();
        for (State s : a2.getStates()) {
            Point p = new Point(s.getPoint() != null ? s.getPoint().x : 0, (s.getPoint() != null ? s.getPoint().y : 0) + d);
            State s2 = newOne.createState(p);
            if (a2.isFinalState(s)) newOne.addFinalState(s2);
            s2.setName(s.getName() != null ? s.getName() + "_2" : "q" + s2.getID());
            otherToNew.put(s, s2);
        }
        for (Transition t : a2.getTransitions()) {
            newOne.addTransition(t.copy(otherToNew.get(t.getFromState()), otherToNew.get(t.getToState())));
        }

        CODEC.encode(newOne, out, null);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"totalStates\":").append(newOne.getStates().length).append(",");
        sb.append("\"totalTransitions\":").append(newOne.getTransitions().length).append(",");
        sb.append("\"outputFile\":\"").append(escapeJson(out.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
    }

    public static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
