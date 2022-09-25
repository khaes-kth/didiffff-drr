package se.kth.assertteam.didiffff.analyzer;

import picocli.CommandLine;
import se.kth.assertteam.didiffff.analyzer.cli.DidiffffHelperCommand;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new DidiffffHelperCommand()).execute(args);
        System.exit(exitCode);
    }
}
