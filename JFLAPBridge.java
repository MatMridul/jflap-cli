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

    public static void main(String[] args) {
        if (args.length < 1) {
            printError("No command specified. Usage: java JFLAPBridge <command> [args...]");
            System.exit(1);
        }

        String command = args[0].toLowerCase();
        try {
            switch (command) {
                case "inspect":
                    inspect(args);
                    break;
                case "simulate":
                    simulate(args);
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
                    fsaEquals(args);
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
                default:
                    printError("Unknown command: " + command);
                    System.exit(1);
            }
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            printError("Execution failed: " + t.getMessage() + "\n" + sw.toString());
            System.exit(1);
        }
    }

    // -------------------------------------------------------------
    // Inspect
    // -------------------------------------------------------------
    private static void inspect(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Usage: inspect <file.jff>");
            System.exit(1);
        }
        File file = new File(args[1]);
        if (!file.exists()) {
            printError("File not found: " + file.getAbsolutePath());
            System.exit(1);
        }

        Serializable obj = decodeFile(file);
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
            printError("Usage: simulate <file.jff> [input1 input2 ...]");
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
        AutomatonSimulator sim = getAutomatonSimulator(auto);

        List<String> inputs = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            inputs.add(args[i]);
        }
        if (inputs.isEmpty()) {
            inputs.add(""); // Empty string simulation
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"file\":\"").append(escapeJson(file.getAbsolutePath())).append("\",");
        sb.append("\"results\":[");

        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            if (i > 0) sb.append(",");
            boolean accepted = false;
            try {
                accepted = runSimulation(sim, input, 10000);
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
            printError("Usage: trace <file.jff> [input]");
            System.exit(1);
        }
        File file = new File(args[1]);
        String input = args.length > 2 ? args[2] : "";

        Serializable obj = decodeFile(file);
        if (!(obj instanceof Automaton)) {
            printError("File is not an automaton: " + obj.getClass().getName());
            System.exit(1);
        }

        Automaton auto = (Automaton) obj;
        AutomatonSimulator sim = getAutomatonSimulator(auto);

        Configuration[] initial = sim.getInitialConfigurations(input);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"file\":\"").append(escapeJson(file.getAbsolutePath())).append("\",");
        sb.append("\"input\":\"").append(escapeJson(input)).append("\",");

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

            if (current.isAccept()) {
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
            sb.append("\"isAccept\":").append(c.isAccept());

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
    // FSA Equivalence Check
    // -------------------------------------------------------------
    private static void fsaEquals(String[] args) throws Exception {
        if (args.length < 3) {
            printError("Usage: fsa_equals <file1.jff> <file2.jff>");
            System.exit(1);
        }
        File f1 = new File(args[1]);
        File f2 = new File(args[2]);

        Serializable o1 = decodeFile(f1);
        Serializable o2 = decodeFile(f2);

        if (!(o1 instanceof FiniteStateAutomaton) || !(o2 instanceof FiniteStateAutomaton)) {
            printError("Both files must be Finite State Automata.");
            System.exit(1);
        }

        FiniteStateAutomaton fsa1 = (FiniteStateAutomaton) o1;
        FiniteStateAutomaton fsa2 = (FiniteStateAutomaton) o2;

        fsa1 = (FiniteStateAutomaton) UselessStatesDetector.cleanAutomaton(fsa1);
        fsa2 = (FiniteStateAutomaton) UselessStatesDetector.cleanAutomaton(fsa2);

        FSAEqualityChecker checker = new FSAEqualityChecker();
        boolean equals = checker.equals(fsa1, fsa2);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"equivalent\":").append(equals).append(",");
        sb.append("\"file1\":\"").append(escapeJson(f1.getAbsolutePath())).append("\",");
        sb.append("\"file2\":\"").append(escapeJson(f2.getAbsolutePath())).append("\"");
        sb.append("}");
        System.out.println(sb.toString());
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

    private static AutomatonSimulator getAutomatonSimulator(Automaton auto) {
        if (auto instanceof PushdownAutomaton) {
            return new HeadlessPDASimulator(auto);
        }
        return SimulatorFactory.getSimulator(auto);
    }

    public static class HeadlessPDASimulator extends AutomatonSimulator {
        private int myAcceptance = 0; // 0: Final State, 1: Empty Stack

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

    private static boolean runSimulation(AutomatonSimulator sim, String input, int maxSteps) {
        Configuration[] initial = sim.getInitialConfigurations(input);
        if (initial == null || initial.length == 0) return false;

        Set<String> visited = new HashSet<>();
        Queue<Configuration> queue = new LinkedList<>();
        for (Configuration c : initial) {
            if (c.isAccept()) return true;
            queue.add(c);
            visited.add(c.toString());
        }

        int steps = 0;
        while (!queue.isEmpty() && steps < maxSteps) {
            Configuration curr = queue.poll();
            steps++;
            if (curr.isAccept()) return true;

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
                    if (next.isAccept()) return true;
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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
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
        return transducer.fromDOM(doc);
    }

    // -------------------------------------------------------------
    // Helper: JSON escaping & error output
    // -------------------------------------------------------------
    private static void printError(String msg) {
        System.err.println(msg);
        System.out.println("{\"error\":\"" + escapeJson(msg) + "\"}");
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
