package se.kth.assertteam.didiffff.analyzer.cli;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "DidiffffHelper",
        mixinStandardHelpOptions = true,
        subcommands = {MatchedLineFinderCommand.class, ExtractDidiResultCommand.class},
        description =
                "The DidiffffHelper command line, for producing and analyzing didiffff results.",
        synopsisSubcommandLabel = "<COMMAND>")
public class DidiffffHelperCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return -1;
    }
}
