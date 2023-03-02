package artmann.microjava;

import java.io.*;

/**
 * Uses JDK 1.8.
 * Compiles Code from MicroJava into MicroJava-Bytecode in clear text.
 * Note that normally Bytecode would be written to a .obj file containing individual bytes for each instruction. For the purpose of understanding the output this compiler generates a clear text format of MJ-Bytecode, in a file type .cmj for "compiled microjava"
 *
 * This Compiler partly contains code that was given with prompts for the exercise "Übersetzerbau" at the Johannes Kepler University.
 */
public class Compiler {

    public static void main(String[] args) {

        String file = "Test.mj"; //input file to analyse
        String outputName = objectName(file);

        try {
            Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));

            System.out.println("-------------------------------");
            System.out.println("Compiling " + file);

            Parser parser = new Parser(scanner);
            parser.parse();
            if (scanner.errors.errorCount() == 0) {
                parser.code.write(new BufferedWriter(new FileWriter(outputName)));
            }

            if (scanner.errors.errorCount() > 0) {
                System.out.println(scanner.errors);
                System.out.println(scanner.errors.errorCount() + " errors.");
            } else {
                System.out.println("No errors.");
            }
        } catch (IOException ex) {
            System.out.println("I/O Error: " + ex.getMessage());
        }
    }

    private static String objectName(String s) {
        int i = s.lastIndexOf('.');
        if (i > 0) {
            return s.substring(0, i) + ".cmj";
        }
        return s + ".cmj";
    }
}
