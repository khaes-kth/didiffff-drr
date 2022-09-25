package se.kth.assertteam.didiffff.analyzer.cli;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;
import spoon.Launcher;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.CtScanner;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "match-line-finder", mixinStandardHelpOptions = true, version = "1.0",
        description = "Finds matched lines between left and right source files.")
public class MatchedLineFinderCommand implements Callable<Integer> {
    private static final String DIFF_SCRIPT_PATH =
            "/home/khaes/phd/projects/explanation/code/tmp/didiffff/analyzer/scripts/diffn.sh";

    @CommandLine.Option(names = {"-ls", "--left-src-path"}, description = "Path to the left source file.")
    private String leftSrcPath;

    @CommandLine.Option(names = {"-rs", "--right-src-path"}, description = "Path to the right source file.")
    private String rightSrcPath;

    @CommandLine.Option(names = {"-lo", "--left-output-path"}, description = "Path to the left output file.")
    private String leftOutputPath;

    @CommandLine.Option(names = {"-ro", "--right-output-path"}, description = "Path to the right output file.")
    private String rightOutputPath;

    @Override
    public Integer call() throws Exception {
        Pair<Set<Integer>, Set<Integer>> output = invoke(new File(leftSrcPath), new File(rightSrcPath));

        createInputFile(StringUtils.join(output.getLeft(), ","), leftOutputPath);
        createInputFile(StringUtils.join(output.getRight(), ","), rightOutputPath);
        return 0;
    }

    /**
     * Find the matched line between two Java source files.
     *
     * @param left previous version of source file
     * @param right revision of source file
     * @return matched line for left and matched line for right
     * @throws Exception raised from gumtree-spoon
     */
    public static Pair<Set<Integer>, Set<Integer>> invoke(File left, File right) throws IOException {
        Pair<Set<Integer>, Set<Integer>> diffLines = getDiffLines(left, right);

        Pair<CtTypeMember, CtTypeMember> methods = findMethods(left, right, diffLines);

        Set<Integer> matchedLinesLeft = getMatchedLines(diffLines.getLeft(), methods.getLeft());
        Set<Integer> matchedLinesRight = getMatchedLines(diffLines.getRight(), methods.getRight());

        return Pair.of(matchedLinesLeft, matchedLinesRight);
    }

    private static Pair<Set<Integer>, Set<Integer>> getDiffLines(File left, File right)
            throws IOException {
        Set<Integer> src = new HashSet<>();
        Set<Integer> dst = new HashSet<>();
        ProcessBuilder pb =
                new ProcessBuilder(
                        DIFF_SCRIPT_PATH,
                        "--no-index",
                        left.getAbsolutePath(),
                        right.getAbsolutePath());
        Process p = pb.start();
        InputStreamReader isr = new InputStreamReader(p.getInputStream());
        BufferedReader rdr = new BufferedReader(isr);
        String line;
        while ((line = rdr.readLine()) != null) {
            String[] lineContents = line.split(":");
            if (lineContents[0].equals("Left")) {
                src.add(Integer.parseInt(lineContents[1]));
            }
            if (lineContents[0].equals("Right")) {
                dst.add(Integer.parseInt(lineContents[1]));
            }
        }
        if (src.size() == 0 && dst.size() == 0) {
            throw new NoDiffException("There is no diff between original and patched version");
        }
        return Pair.of(src, dst);
    }

    static class BlockFinder extends CtScanner {
        private final Set<Integer> diffLines;
        private final Set<Integer> lines = new HashSet<>();

        private BlockFinder(Set<Integer> diffLines) {
            this.diffLines = diffLines;
        }

        /**
         * Get line numbers of statements, excluding diff lines, within a block.
         *
         * @param block element to be traversed
         * @param <R> return type of block, if any
         */
        @Override
        public <R> void visitCtBlock(CtBlock<R> block) {
            List<CtStatement> statements = block.getStatements();
            statements.forEach(
                    statement -> {
                        if (!shouldBeIgnored(statement)
                                && !diffLines.contains(statement.getPosition().getLine())) {
                            lines.add(statement.getPosition().getLine());
                        }
                    });
            super.visitCtBlock(block);
        }

        @Override
        public <S> void visitCtCase(CtCase<S> caseStatement) {
            List<CtStatement> caseBlock = caseStatement.getStatements();
            caseBlock.forEach(
                    statement -> {
                        if (!diffLines.contains(statement.getPosition().getLine())
                                && !shouldBeIgnored(statement)) {
                            lines.add(statement.getPosition().getLine());
                        }
                    });
            super.visitCtCase(caseStatement);
        }

        private static boolean shouldBeIgnored(CtElement element) {
            return element instanceof CtComment || element.isImplicit();
        }

        public Set<Integer> getLines() {
            return lines;
        }
    }

    private static Set<Integer> getMatchedLines(Set<Integer> diffLines, CtTypeMember method) {
        BlockFinder blockTraversal = new BlockFinder(diffLines);
        blockTraversal.scan(method);
        return blockTraversal.getLines();
    }

    private static Pair<CtTypeMember, CtTypeMember> findMethods(
            File left, File right, Pair<Set<Integer>, Set<Integer>> diffLines) throws IOException {
        // In an ideal case, srcNode of first root operation will give the method because APR
        // patches usually have
        // only one operation.
        // We also return the first method we find because we assume there will a patch inside only
        // one method.
        CtType<?> leftType = getType(left);
        CtType<?> rightType = getType(right);

        List<CtTypeMember> leftTypeMembers = getTypeMembers(leftType);
        List<CtTypeMember> rightTypeMembers = getTypeMembers(rightType);

        CtTypeMember leftDiffedTypeMember =
                findDiffedTypeMember(leftTypeMembers, diffLines.getLeft());
        CtTypeMember rightDiffedTypeMember =
                findDiffedTypeMember(rightTypeMembers, diffLines.getRight());

        if (leftDiffedTypeMember == null && rightDiffedTypeMember == null) {
            throw new RuntimeException("Neither left nor right diffed type member could be found.");
        }

        if (leftDiffedTypeMember == null) {
            leftDiffedTypeMember = findMapping(rightDiffedTypeMember, leftTypeMembers);
        }

        if (rightDiffedTypeMember == null) {
            rightDiffedTypeMember = findMapping(leftDiffedTypeMember, rightTypeMembers);
        }

        return Pair.of(leftDiffedTypeMember, rightDiffedTypeMember);
    }

    private static CtType<?> getType(File file) throws IOException {
        return Launcher.parseClass(Files.readString(file.toPath()));
    }

    private static List<CtTypeMember> getTypeMembers(CtType<?> type) {
        List<CtTypeMember> typeMembers = new ArrayList<>();
        for (CtTypeMember candidateTypeMember : type.getTypeMembers()) {
            if (candidateTypeMember instanceof CtMethod<?>
                    || candidateTypeMember instanceof CtConstructor<?>) {
                typeMembers.add(candidateTypeMember);
            }
            if (candidateTypeMember instanceof CtClass<?>) {
                typeMembers.addAll(getTypeMembers((CtType<?>) candidateTypeMember));
            }
        }
        return typeMembers;
    }

    private static CtTypeMember findDiffedTypeMember(
            List<CtTypeMember> typeMembers, Set<Integer> diffLines) {
        Set<CtTypeMember> candidates = new HashSet<>();
        for (CtTypeMember typeMember : typeMembers) {
            if (typeMember.isImplicit()) {
                continue;
            }
            for (Integer position : diffLines) {
                if (typeMember.getPosition().getLine() < position
                        && position < typeMember.getPosition().getEndLine()) {
                    candidates.add(typeMember);
                }
            }
        }
        if (candidates.size() > 1) {
            throw new RuntimeException("More than 1 diffedTypeMember found");
        }
        if (candidates.size() == 0) {
            return null;
        }
        return candidates.stream().findFirst().get();
    }

    private static CtTypeMember findMapping(
            CtTypeMember whoseMapping, List<CtTypeMember> candidateMappings) {
        int expectedStartLine = whoseMapping.getPosition().getLine();
        return candidateMappings.stream()
                .filter(typeMember -> !typeMember.isImplicit())
                .filter(typeMember -> typeMember.getPosition().getLine() == expectedStartLine)
                .findFirst()
                .get();
    }

    public static class NoDiffException extends RuntimeException {
        public NoDiffException(String message) {
            super(message);
        }
    }

    private static int checkout(File cwd, String commit) throws IOException, InterruptedException {
        ProcessBuilder checkoutBuilder = new ProcessBuilder("git", "checkout", commit);
        checkoutBuilder.directory(cwd);
        Process p = checkoutBuilder.start();
        return p.waitFor();
    }

    private static File copy(File cwd, File diffedFile, String revision)
            throws IOException, InterruptedException {
        final File revisionDirectory = new File(cwd.toURI().resolve(revision));
        revisionDirectory.mkdir();

        ProcessBuilder cpBuilder =
                new ProcessBuilder(
                        "cp",
                        diffedFile.getAbsolutePath(),
                        revisionDirectory.toURI().resolve(diffedFile.getName()).getPath());
        cpBuilder.directory(cwd);
        Process p = cpBuilder.start();
        p.waitFor();

        return new File(revisionDirectory.toURI().resolve(diffedFile.getName()).getPath());
    }

    private static void createInputFile(String content, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }
}
