package logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.Date;

import ui.ControlPanel;

public class Logger {
    public static boolean sysOut = false;
    static String path = "";
    static String line = "";
    static boolean corrupted = false;

    static {
        init();
    }

    public static void init() {
        Date date = new Date(System.currentTimeMillis());
        date.getTime();

        println();
        println();
        println("-*#*--*#*--*#*--*#*--*#*--*#*--*#*--*#*--*#*--*#*--*#*--*#*-");
        println("");
        println("____ New Session: " + date.toString() + " " + date.getTime() + " ___");
        println();
    }

    public static void printlnToFile(String s) {
        if (corrupted) {
            System.out.println("Logger Corrupted! " + path);
            return;
        }
        try {
            /*
             * File directory = new File(path);
             * if (!directory.exists()){
             * }
             */

            Writer output;
            output = new BufferedWriter(new FileWriter(path, true));
            output.append(line + s + "\n");
            output.close();
            line = "";
            if (sysOut)
                Logger.println(s);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("NO Log file found. " + path);
            corrupted = true;
            System.exit(-1);
        }

    }

    public static void println(String s) {
        if (corrupted) {
            System.out.println("Logger Corrupted! " + path);
            return;
        }
        if (sysOut)
            System.out.println(s);
        ControlPanel.log(s);
    }

    public static void notify(String s) {

        println(s);
    }

    public static void println() {
        println("");
    }

    public static void print(String s) {
        // line += s;
        ControlPanel.log(s);
    }

    public static void printSys(boolean print) {
        sysOut = print;
    }

    public static void printStackTrace(Exception e) {
        println("::: EXCEPTION! :::");
        println(e.getClass().toGenericString());
        StackTraceElement[] stack = e.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            println(stack[i].toString());
        }
        println();
    }

    public static void printStackTrace(Throwable e) {
        println("::: EXCEPTION! :::");
        println(e.getClass().toGenericString());
        StackTraceElement[] stack = e.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            println(stack[i].toString());
        }
        println();
    }

}
// Logger.printStackTrace(e);