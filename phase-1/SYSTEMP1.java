
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Name : Charmal Mahapatabendige
// CS 5323 : Design and Implementation of Operating System - 2
// Assignment Title : OSII Project Phase -I
// Date : 03-21-2017
// The system consists of Memory- The array contains all the memory values.
// Loader - Reads all the instructions fromt the input file and saved it in the memory.
// The CPU - reads the instructions and act releated to those instcutions.
// ErrorHandler - Handles all the errors in the system.
public class SYSTEMP1 {

    /**
     * The Memory integer list.
     */
    public static List<Integer> MEMORY = new ArrayList<>(4096);

    /**
     * The boolean represent the trace switch on or off
     */
    public static boolean isTraceSwitchOn;

    /**
     * The integer contains the system CLOCK
     */
    public static int CLOCK = 0;

    /**
     * The integer contains the time needs to input
     */
    public static int inputTime = 0;

    /**
     * The integer contains the time needs to output
     */
    public static int outputTime = 0;

    /**
     * The register
     */
    public static Integer[] register = new Integer[10];

    /**
     * The initial instruction index
     */
    public static int initialInstructionIndex = 0;

    /**
     * The initial instruction index
     */
    public static final int formatIntDex = 0b111111111111;

    /**
     * The name of the file that contains the input.
     */
    public static String inputFileName = "input_Error_Wrong_RD_WR_HLT.txt";

    /**
     * The name of the file that contains the output.
     */
    public static final String outputFileName = "output.txt";

    /**
     * The name of the file that contains the trace.
     */
    public static final String traceFileName = "trace_file.txt";

    /**
     * The name of the file that contains the dumpout file.
     */
    public static final String dumpOutPutFileName = "dumpOutput.txt";
    
    /**
     * The output file contents.
     */
    public static List<String> OUTPUT = new ArrayList<>();

    // The hex name
    public static final String hexname = "(HEX)";

    //The decimal name
    public static final String decimalName = "(DECIMAL)";

    /**
     * The USER job list
     */
    public static List<Integer> USERJOB = new ArrayList<>();

    /**
     * Defines whether the trace file heading
     */
    public static boolean isFirstTimeTraceFile = true;

    /**
     * The main system.
     *
     * @param args
     * @throws IOException
     * @throws Exception
     */
    public static void main(String[] args) throws IOException, Exception {

        if (args.length > 0) {
            String input = args[0];
            if (input != null && !input.isEmpty()) {
                inputFileName = input;
            }
        }

        new File(outputFileName).delete();

        new File(traceFileName).delete();

        LOADER(0, 0);

        CPU(initialInstructionIndex, 0);

        for (int k = 0; k < USERJOB.size(); k++) {
            OUTPUT.add("Current User job : " + USERJOB.get(k).toString());
        }

        printOutputFile();
    }

    // The LOADER Subsystem use for load the instructions in the input file
    // which is required in CPU subsystem for execution. 
    // Futhermore it help to loand the instruction from memory.
    /**
     *
     * @param X
     * @param Y
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void LOADER(int X, int Y) throws
            FileNotFoundException, IOException, Exception {

        USERJOB.add(1);

        File file = new File(inputFileName);

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String line;

        int actualInsCount = 0;

        int insCount = 0;

        try {

            while ((line = bufferedReader.readLine()) != null) {

                String[] splited = (line.split("(?<=\\G...)"));

                for (String splited1 : splited) {

                    int ins = convertHexToBin(splited1.trim());

                    if (actualInsCount == 0) {
                        insCount = ins;
                    } else {
                        MEMORY.add(ins);
                    }

                    actualInsCount++;
                }
            }

            if ((actualInsCount - 3) != insCount) {
                ERROR_HANDLER(ErrorCode.Error_Program_Size_Large.ordinal());
                terminate();
            }

        } catch (Exception ex) {
            ERROR_HANDLER(ErrorCode.Error_Illegal_Input.ordinal());
            terminate();
        }

        getInitialCommand();

        checkTraceInfo();

    }

    /**
     * The CPU subsystem is called from SYSTEM to execute the user job.
     * parameter X denotes the initial value of program counter. parameter Y
     * denotes the trace switch.
     *
     *
     * @param X initial value
     * @param Y
     * @throws IOException
     * @throws Exception
     */
    public static void CPU(int X, int Y) throws IOException, Exception {

        intiaizeRegister(X);

        int EA = 0;
        int instruction;
        boolean isIterate = true;

        while (isIterate) {

            int mem = memoryGet(register[2]);

            instruction = (mem);

            int opcode = ((instruction & 0b111111111111) >>> 8) & (0b0111);
            int R = ((instruction & 0b111111111111) >> 7) & (0b1);

            if (isTraceSwitchOn) {
                traceExecute(R, opcode, EA, true);
            }

            int typeSpecifier = (((instruction & formatIntDex) >> 11) & (0b1));

            checkopCodeError(opcode);

            if ((opcode == 0b110)) {//type II
                adjustClock(11);
                int r = ((instruction & formatIntDex) >> 6) & (0b1);
                int w = ((instruction & formatIntDex) >> 5) & (0b1);
                int h = ((instruction & formatIntDex) >> 4) & (0b1);

                detectWrongRDWRHLTError(r, w, h);

                if (r == 0b1) {//READ 
                    Scanner input = new Scanner(System.in);
                    String hexNum = input.nextLine();
                    int temp = convertHexToBin(hexNum);
                    if (R == 0b0) {
                        register[5] = temp;
                    } else {
                        register[4] = temp;
                    }
                    register[2] += 1;
                    inputTime = inputTime + 10;

                } else if (w == 0b1) {//WR
                    if (R == 0b0) {
                        String num = String.valueOf(register[5]);
                        int i = Integer.parseInt(num, 10);

                        String output = Integer.toHexString(i);
                        System.out.println(output);
                        println(output + hexname);
                    } else {
                        String num = String.valueOf(register[4]);
                        int i = Integer.parseInt(num, 10);

                        String output = Integer.toHexString(i);
                        System.out.println(output);
                        println(output + hexname);
                    }
                    register[2] += 1;
                    outputTime = outputTime + 10;

                } else if (h == 0b1) {//HLT                 
                    break;
                }

            } else if ((opcode == 0b0111) && (typeSpecifier == 0b0)) {//type III
                adjustClock(1);
                int a = ((instruction & formatIntDex) >> 6) & (0b1);
                int b = ((instruction & formatIntDex) >> 5) & (0b1);
                int c = ((instruction & formatIntDex) >> 4) & (0b1);
                int d = ((instruction & formatIntDex) >> 3) & (0b1);
                int e = ((instruction & formatIntDex) >> 2) & (0b1);
                int f = ((instruction & formatIntDex) >> 1) & (0b1);
                int g = ((instruction & formatIntDex)) & (0b1);

                boolean c1 = ((a + b + c + d + e + f) > 0b1);
                boolean c2 = (a + b + c + d + g > 0b1);
                boolean c3 = (a + b + c + d + e + f == 0b0);
                if (c1 || c2 || c3) {
                    ERROR_HANDLER(ErrorCode.Error_Shift_Combination
                            .ordinal());
                    terminate();
                }

                if (a == 0b1) {//CLR
                    if (R == 0b0) {
                        register[5] = 0;
                    } else {
                        register[4] = 0;
                    }
                } else if (b == 0b1) {//INC
                    if (R == 0b0) {
                        register[5] = register[5] + 1;
                    } else {
                        register[4] = register[4] + 1;
                    }
                } else if (c == 0b1) {//COM
                    if (R == 0b0) {
                        register[5] = ~(register[5]);
                    } else {
                        register[4] = ~(register[4]);
                    }
                } else if (d == 0b1) {//BSW 
                    if (R == 0b0) {
                        register[5] = Integer.reverse(register[5]);
                    } else {
                        register[4] = Integer.reverse(register[4]);
                    }
                } else if (e == 0b1) {//RTL
                    if (R == 0b0) {
                        if (g == 0b0) {
                            register[5] = ((register[5]) << 1);
                        } else {
                            register[5] = ((register[5]) << 2);
                        }
                    } else if (g == 0b0) {
                        register[4] = ((register[4]) << 1);
                    } else {
                        register[4] = ((register[4]) << 2);
                    }
                } else if (f == 0b1) {
                    if (R == 0b0) {
                        if (g == 0b0) {
                            register[5] = ((register[5]) >> 1);
                        } else {
                            register[5] = ((register[5]) >> 2);
                        }
                    } else if (g == 0b0) {
                        register[4] = ((register[4]) >> 1);
                    } else {
                        register[4] = ((register[4]) >> 2);
                    }
                }

                register[2] += 1;

            } else if ((opcode == 0b0111) && (typeSpecifier == 0b1)) { //TYPE 4
                adjustClock(1);

                int equal = ((instruction & formatIntDex) >> 4) & (0b111);
                int temp;

                switch (equal) {
                    case 0b000://NSK 
                        register[2] = register[2] + 1;
                        break;
                    case 0b001: {//GTR                        
                        if (R == 0b0) {
                            temp = register[5];
                        } else {
                            temp = register[4];
                        }
                        if (temp > 0) {
                            register[2] = register[2] + 2;
                        } else {
                            register[2] += 1;
                        }
                        break;
                    }
                    case 0b010: { //LSS

                        if (R == 0b0) {
                            temp = register[5];
                        } else {
                            temp = register[4];
                        }
                        if (temp < 0) {
                            register[2] = register[2] + 2;
                        } else {
                            register[2] += 1;
                        }
                        break;
                    }
                    case 0b011: { //NEQ

                        if (R == 0b0) {
                            temp = register[5];
                        } else {
                            temp = register[4];
                        }
                        if (temp != 0) {
                            register[2] = register[2] + 2;
                        } else {
                            register[2] += 1;
                        }
                        break;
                    }
                    case 0b100: {  //EQL                      
                        if (R == 0b0) {
                            temp = register[5];
                        } else {
                            temp = register[4];
                        }
                        if (temp == 0) {
                            register[2] = register[2] + 2;
                        } else {
                            register[2] += 1;
                        }
                        break;
                    }
                    case 0b101: { //GRE                       
                        if (R == 0b0) {
                            temp = register[5];
                        } else {
                            temp = register[4];
                        }

                        if (temp >= 0) {
                            register[2] = register[2] + 2;
                        } else {
                            register[2] += 1;
                        }
                        break;
                    }
                    case 0b110: {//LSE                        
                        if (R == 0b0) {
                            temp = register[5];
                        } else {
                            temp = register[4];
                        }
                        if (temp <= 0) {
                            register[2] = register[2] + 2;
                        } else {
                            register[2] += 1;
                        }
                        break;
                    }
                    case 0b111: {//USK                        
                        register[2] = register[2] + 2;
                        break;
                    }
                    default:
                        break;
                }

            }//type I
            else if (opcode == 0b000 || opcode == 0b001
                    || opcode == 0b010 || opcode == 0b011
                    || opcode == 0b100 || opcode == 0b101) {

                adjustClock(1);
                int I, x, ADDR;
                I = ((instruction & formatIntDex) >> 11) & (0b1);
                x = ((instruction & formatIntDex) >> 6) & (0b1);
                ADDR = ((instruction & formatIntDex)) & (0b111111);
                EA = addressMode(I, x, register[2], register[4], ADDR);

                switch (opcode) {
                    case 0b000:  // Logical AND 
                        if (R == 0b0) {
                            register[5] = register[5] & memoryGet(EA);
                        } else {
                            register[4] = register[4] & memoryGet(EA);
                        }
                        register[2] += 1;
                        break;

                    case 0b001:// ADDITION
                        int sign = ((memoryGet(EA) >> 11) & 0b1);
                        int unsignInt;
                        if (sign == 1) {
                            unsignInt = ~memoryGet(EA) + 1;
                            unsignInt = (unsignInt & (formatIntDex));
                            unsignInt = -unsignInt;
                        } else {
                            unsignInt = memoryGet(EA);
                        }
                        if (R == 0b0) {
                            register[5] = register[5] + unsignInt;
                            detectOverFlowError(register[5]);
                        } else {
                            register[4] = register[4] + unsignInt;
                            detectOverFlowError(register[4]);
                        }
                        register[2] += 1;
                        break;

                    case 0b010:  // STORE
                        if (R == 0b0) {
                            memorySet(EA, register[5]);
                        } else {
                            memorySet(EA, register[4]);
                        }
                        register[2] += 1;
                        break;

                    case 0b011:   //LOAD
                        if (R == 0b0) {
                            register[5] = memoryGet(EA);
                        } else {
                            register[4] = memoryGet(EA);
                        }
                        register[2] += 1;
                        break;

                    case 0b100: //JUMP
                        register[2] = EA;
                        break;

                    case 0b101://JUMP AND LINK 
                        if (R == 0b0) {
                            register[5] = register[2] + 1;
                            register[2] = EA;
                        } else {
                            register[4] = register[2] + 1;
                            register[2] = EA;
                        }
                        break;
                    default:
                        break;
                }
            } else {
                ERROR_HANDLER(ErrorCode.Error_Invalid_Opcode.ordinal());
                terminate();
            }

            if (isTraceSwitchOn) {
                traceExecute(R, opcode, EA, false);
            }

            if (CLOCK > 2500) {
                ERROR_HANDLER(ErrorCode.Error_InfiniteLoop.ordinal());
                terminate();
            }
        }
    }

    /**
     * The MEMORY subsystem is called for several operations. 1. read the memory
     * 2. write to the memory and 3.dump the memory.
     *
     * @param X - What type of memory operation.
     * @param Y - Effective memory address
     * @param Z - memory buffer register.
     * @throws Exception
     */
    public static void MEMORY(String X, int Y, int Z) throws Exception {

        if (null != X) {
            switch (X) {
                case "READ":
                    Z = memoryGet(Y);
                    break;
                case "WRIT":
                    memorySet(Y, Z);
                    break;
                case "DUMP":
                    int j = 0;
                    int n = 0;
                    int m = 0;
                    int index = 256;
                    int iterations = index / 8 + 1;
                    File file = new File(dumpOutPutFileName);
                    FileWriter fileWritter = new FileWriter(file.getName(), true);
                    BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                    bufferWritter.newLine();
                    for (int k = 0; k < iterations; k++) {
                        bufferWritter.write((String.format("%04x", n)).toUpperCase());
                        j = 0;
                        n = n + 8;
                        while ((j < 8) && (m <= 256)) {

                            if (m < MEMORY.size()) {
                                bufferWritter.write(" " + String.format("%03x", MEMORY.get(m)).toUpperCase() + " ");
                            } else {
                                bufferWritter.write(" " + String.format("%03x", 0).toUpperCase() + " ");
                            }
                            m++;
                            j++;
                        }
                        bufferWritter.newLine();
                    }
                    bufferWritter.close();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Returns the appropiate error message
     *
     * @param errorid
     * @throws java.lang.Exception
     */
    public static void ERROR_HANDLER(int errorid) throws Exception {
        String errorMessage = "";

        ErrorCode errorCode = ErrorCode.values()[errorid];

        switch (errorCode) {
            case Error_Over_Flow:
                errorMessage = "Termination Status - Abnormal. "
                        + "Message : Overflow error";
                break;
            case Error_InfiniteLoop:
                errorMessage = "Termination Status- Abnormal. "
                        + "Message : Infinite loop";
                break;
            case Error_InvalidLoader:
                errorMessage = "Termination Status- Abnormal. "
                        + "Message : Invalid Loader format character";
                break;
            case Error_Invalid_Opcode:
                errorMessage = "Termination Status- Abnormal. "
                        + "Message : Invalid opcode.";
                break;
            case Error_Addr_Outof_Range:
                errorMessage = "Termination Status - Abnormal. "
                        + "Message :  Address out of range.";
                break;
            case Error_Illegal_Input:
                errorMessage = "Termination Status- Abnormal. "
                        + "Message : Illegal Input.";
                break;
            case Error_Program_Size_Large:
                errorMessage = "Termination Status- Abnormal. "
                        + "Message : Program Size too large.";
                break;
            case Error_Wrong_RD_WR_HLT:
                errorMessage = "Termination Status- Abnormal. "
                        + "Message : More than one "
                        + "of the RD WR HLT bit is set.";
                MEMORY("DUMP", 0, 0);
                break;
            case Error_Shift_Combination:
                errorMessage = "Termination - Abnormal. Message : Wrong"
                        + " shift bits combination";
                MEMORY("DUMP", 0, 0);

                break;
            case Error_Null_Reference:
                errorMessage = "Termination - Abnormal. Message :Null Reference"
                        + "exception occured as input given is null";
                break;

            case Warning_Shift_Invalid_Trace_Info:
                errorMessage = "Warning Message : Invalid or Missing Trace Flag."
                        + " The Trace flag value should be 0 or 1.";
                break;
            default:
                break;
        }

        println(errorMessage);
    }

    // intialize register values.
    public static void intiaizeRegister(int x) {
        register[0] = 0;
        register[1] = 1;
        register[2] = x;// PC
        register[3] = 0;
        register[4] = 0;
        register[5] = 0;
        register[6] = 0;
        register[7] = 0;
        register[8] = 0;
        register[9] = 0;
    }

    // Get the initial command.
    public static void getInitialCommand() {
        String initialCommand = (MEMORY.get(MEMORY.size() - 2)).toString();
        initialInstructionIndex = Integer.parseInt(initialCommand);
    }

    // Check the trace info.
    public static void checkTraceInfo() throws Exception {
        String traceBit = (MEMORY.get(MEMORY.size() - 1)).toString();

        if (null != traceBit) {
            switch (traceBit) {
                case "1":
                    isTraceSwitchOn = true;
                    break;
                case "0":
                    isTraceSwitchOn = false;
                    break;
                default:
                    ERROR_HANDLER(ErrorCode.Warning_Shift_Invalid_Trace_Info.ordinal());
                    isTraceSwitchOn = false;
                    break;
            }
        } else {
            ERROR_HANDLER(ErrorCode.Warning_Shift_Invalid_Trace_Info.ordinal());
            isTraceSwitchOn = false;
        }

    }

    /**
     * Method returns the effective address while calculating the called about
     * the address mode d
     *
     * @param I
     * @param x
     * @param reg2
     * @param reg4
     * @param ADDR
     * @return
     * @throws Exception
     */
    public static int addressMode(int I, int x, int reg2,
            int reg4, int ADDR) throws Exception {
        int effectiveAddr = 0;
        int sign = ((ADDR >> 5) & 0b1);

        int temp;
        temp = ~ADDR + 1;
        temp = temp & 0b111111;
        boolean c1 = ((reg2 + 1 - temp) >= 0);

        boolean signcompliment = ((sign == 1) && (c1));

        // Direct Addressing
        if ((I == 0b0) && (x == 0b0)) {
            if (signcompliment) {
                ADDR = ~ADDR + 1;
                ADDR = ADDR & 0b111111;
                effectiveAddr = reg2 + 1 - ADDR;

            } else {
                effectiveAddr = reg2 + 1 + ADDR;
            }
            // Indexing mode
        } else if (signcompliment) {
            if (sign == 1) {
                ADDR = ~ADDR + 1;
                ADDR = ADDR & 0b111111;
                effectiveAddr = ADDR + memoryGet(reg4);
            } else {
                int A = reg2 + 1 + ADDR;
                detectOverFlowError(A);
                effectiveAddr = A + memoryGet(reg4);
            }
            // Indirection mode
        } else if ((I == 0b1) && (x == 0b0)) {
            if (signcompliment) {
                ADDR = ~ADDR + 1;
                ADDR = ADDR & 0b111111;
                int A = reg2 - (ADDR);
                effectiveAddr = memoryGet(A);
            } else {
                int A = reg2 + 1 + ADDR;
                detectOverFlowError(A);
                effectiveAddr = memoryGet(A);
            }
            // //Indexing & Indirection modes
        } else if ((I == 0b1) && (x == 0b1)) {
            if (signcompliment) {
                ADDR = ~ADDR + 1;
                ADDR = ADDR & 0b111111;
                int A = reg2 + 1 - (ADDR);
                effectiveAddr = memoryGet(A) + memoryGet(reg4);
            } else {
                int A = reg2 + 1 + ADDR;
                detectOverFlowError(A);
                effectiveAddr = memoryGet(A) + memoryGet(reg4);
            }
        }

        detectOverFlowError(effectiveAddr);

        return effectiveAddr;
    }

    // Method returns the supplied string's bin value.
    public static Integer convertHexToBin(String hexVal) throws Exception {
        detectHexToBinError(hexVal);

        int intVal = Integer.parseInt(hexVal, 16);
        int val = Integer.parseInt(Integer.toBinaryString(intVal), 2);
        return val;
    }

    // Method returns the supplied string's hex value.
    public static String convertBinToHex(String bin) {
        int i = Integer.parseInt(bin, 2);
        String hex = Integer.toHexString(i);
        return hex;
    }

    // System terminates
    public static void terminate() throws IOException {
        printOutputFile();
        System.exit(-1);
    }

    // Set the memory value.
    public static void memorySet(int index, int value) {
        MEMORY.set(index, value);
    }

    // get the memory content 
    public static int memoryGet(int index) {
        return MEMORY.get(index);
    }

    /**
     * Detect the memory over flow error.
     *
     * @param memoryAddr
     * @throws Exception
     */
    public static void detectOverFlowError(int memoryAddr) throws Exception {
        String binary = Integer.toBinaryString(memoryAddr);

        if (binary.length() > 12) {
            ERROR_HANDLER(ErrorCode.Error_Over_Flow.ordinal());
            terminate();
        }
    }

    // Detect the Read write and halt error.
    public static void detectWrongRDWRHLTError(int r, int w, int h) throws Exception {
        boolean c1 = (r == 0b1) && ((w == 0b1));
        boolean c2 = (r == 0b1) && ((h == 0b1));
        boolean c3 = (w == 0b1) && ((h == 0b1));

        if (c1 || c2 || c3) {
            ERROR_HANDLER(ErrorCode.Error_Wrong_RD_WR_HLT.ordinal());
            terminate();
        }
    }

    // check the invalid loarder format error
    public static void detectHexToBinError(String s)
            throws Exception, IOException {
        int full = 0;
        for (int i = 0; i < s.length(); i++) {
            try {
                int t = Integer.parseInt(s.charAt(i) + "", 16);
                full = full << 4;
                full = full + t;
            } catch (Exception ex) {
                ERROR_HANDLER(ErrorCode.Error_InvalidLoader.ordinal());
                terminate();
            }

        }
    }

    // Check the OpCode is correct or not    
    public static void checkopCodeError(int opcode) throws Exception {
        if (opcode == 0b110 || opcode == 0b111 || opcode == 0b000
                || opcode == 0b001 || opcode == 0b011
                || opcode == 0b100 || opcode == 0b101 || opcode == 0b010) {
        } else {
            ERROR_HANDLER(ErrorCode.Error_Invalid_Opcode.ordinal());
            terminate();
        }

    }

    // All the errors and warnings 
    public enum ErrorCode {
        Error_Over_Flow,
        Error_InfiniteLoop,
        Error_InvalidLoader,
        Error_FormatChar,
        Error_Invalid_Opcode,
        Error_Addr_Outof_Range,
        Error_Illegal_Input,
        Error_Program_Size_Large,
        Error_Wrong_RD_WR_HLT,
        Error_Shift_Combination,
        Error_Null_Reference,
        Warning_Shift_Invalid_Trace_Info,
    }

    // Adjust the clock value.
    public static void adjustClock(int incremantValue) {

        if (incremantValue > 1) {

        }

        CLOCK = CLOCK + incremantValue;
    }

    // The system print method    
    public static void println(String line) {
        OUTPUT.add(line);
    }

    // The trace file output.
    public static void traceExecute(int R, int opcode,
            int EA, boolean isBefore) throws IOException {

        File file = new File(traceFileName);
        FileWriter fileWritter = new FileWriter(file.getName(), true);

        try (BufferedWriter bufferWritter = new BufferedWriter(fileWritter)) {

            if (isFirstTimeTraceFile) {
                bufferWritter.write("PC" + "\t" + "Ins"
                        + "\t" + "Reg."
                        + "\t" + "Before/"
                        + "\t" + "Content" + "\t"
                        + "EA value" + "  "
                        + "Memory(EA)");

                bufferWritter.newLine();

                bufferWritter.write(hexname + "\t" + hexname
                        + "\t" + ""
                        + "\t" + "After"
                        + "\t" + hexname + "\t"
                        + hexname + "\t   "
                        + hexname);

                bufferWritter.newLine();
                isFirstTimeTraceFile = false;
            }

            String ea = convertBinToHex(
                    Integer.toBinaryString(EA)).toUpperCase();

            String reg5 = convertBinToHex(
                    Integer.toBinaryString(register[5])).toUpperCase();

            String reg4 = convertBinToHex(
                    Integer.toBinaryString(register[4])).toUpperCase();

            String memEA = convertBinToHex(
                    Integer.toBinaryString(memoryGet(EA))).toUpperCase();

            String timeManner = "after";

            if (isBefore) {
                timeManner = "before";
            }

            String registerType = "";
            String reg2 = "";
            String reg2Ins = "";

            if (isBefore) {

                reg2 = convertBinToHex(
                        Integer.toBinaryString(register[2])).toUpperCase();

                reg2Ins = convertBinToHex(
                        Integer.toBinaryString(
                                memoryGet(register[2]))).toUpperCase();

                if (R == 0b0) {
                    registerType = "R5";
                } else {
                    registerType = "R4";
                }
            }

            String content = "";

            if (R == 0b0) {

                content = reg5;

            } else {

                content = reg4;

            }

            bufferWritter.write(reg2.toUpperCase() + "\t" + reg2Ins
                    + "\t" + registerType
                    + "\t" + timeManner
                    + "\t   " + content + "\t   "
                    + ea + "\t   "
                    + memEA);

            bufferWritter.newLine();
        }

    }

    //Print the output
    public static void printOutputFile() throws IOException {

        String clockval = "CLOCK value : "
                + Integer.toHexString(CLOCK).toUpperCase() + hexname;

        OUTPUT.add(clockval);

        String inputTimeval = "Input time : " + inputTime + decimalName;
        OUTPUT.add(inputTimeval);

        String outputTimeval = "Output time : " + outputTime + decimalName;
        OUTPUT.add(outputTimeval);

        int executionTime = CLOCK - (inputTime + outputTime);

        String executionTimeval = "Execution time : " + executionTime + decimalName;
        OUTPUT.add(executionTimeval);

        File file = new File(outputFileName);
        FileWriter fileWritter = new FileWriter(file.getName(), true);
        BufferedWriter bufferWritter
                = new BufferedWriter(fileWritter);
        bufferWritter.newLine();

        for (int k = 0; k < OUTPUT.size(); k++) {

            bufferWritter.write((OUTPUT.get(k)));

            bufferWritter.newLine();
        }

        bufferWritter.close();

    }
}
