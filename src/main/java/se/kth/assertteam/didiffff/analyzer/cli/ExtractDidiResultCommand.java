package se.kth.assertteam.didiffff.analyzer.cli;

import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import picocli.CommandLine;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "extract-didi-results", mixinStandardHelpOptions = true, version = "1.0",
        description = "Extracts didi results from its report.")
public class ExtractDidiResultCommand implements Callable<Integer> {
    private static final long SELENIUM_LOAD_WAIT_SEC = 1000L;
    private static final long SELENIUM_LONG_LOAD_WAIT_SEC = 10000L;

    @CommandLine.Option(names = {"-u", "--url"}, description = "URL to didiffff report.")
    private String url;

    @CommandLine.Option(names = {"-p", "--path-to-changed-file"}, description = "Path to the changed source file.")
    private String pathToChangedFile;

    @CommandLine.Option(names = {"-ls", "--left-src-path"}, description = "Path to the left source file.")
    private File leftSrc;

    @CommandLine.Option(names = {"-rs", "--right-src-path"}, description = "Path to the right source file.")
    private File rightSrc;

    @Override
    public Integer call() throws Exception {
        Pair<Set<Integer>, Set<Integer>> breakpoints = MatchedLineFinderCommand.invoke(leftSrc, rightSrc);
        analyzeDidiffffResult(url, pathToChangedFile, breakpoints.getLeft(), breakpoints.getRight());
        return 0;
    }

    private void analyzeDidiffffResult
            (
                    String url,
                    String pathToChangedFile,
                    Set<Integer> leftBreakpoints,
                    Set<Integer> rightBreakpoints
            )
            throws InterruptedException {
        String[] pathParts = pathToChangedFile.split("/"); // pathToChangedFile = "Math-1/src/java/main/x/y/z.java

        boolean noDiff = false, sameLength = false, contentDiff = false, lengthDiff = false;

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("headless");
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(url);
            Thread.sleep(SELENIUM_LONG_LOAD_WAIT_SEC);

            WebElement curElem = driver.findElement(By.xpath("//span[contains(text(),'" + pathParts[0] + "')]"));
            curElem.click();
            Thread.sleep(SELENIUM_LOAD_WAIT_SEC);

            for(int i = 1; i < pathParts.length; i++) {
                curElem = curElem.findElement(By.xpath("./../../.."));
                List<WebElement> containingElems =
                        curElem.findElements(By.xpath("//span[contains(text(),'" + pathParts[i] + "')]"));
                for(WebElement elem : containingElems){
                    if(elem.getText().equals(pathParts[i])) {
                        curElem = elem;
                        break;
                    }
                }
                curElem.click();
                Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
            }

            if(!(leftBreakpoints == null || leftBreakpoints.size() == 0 || rightBreakpoints == null || rightBreakpoints.size() == 0)) {
                int startLine = Math.min(Collections.min(leftBreakpoints), Collections.min(rightBreakpoints)),
                        endLine = Math.max(Collections.max(leftBreakpoints), Collections.max(rightBreakpoints));

                for (int i = startLine; i <= endLine; i++) {
                    List<WebElement> elems =
                            driver.findElements(By.xpath("//span[@class='css-1p60ryf' and contains(text(),'" + i + "')]"));
                    for(WebElement elem : elems){
                        if(elem.getText().equals(i + "")){
                            curElem = elem;
                            break;
                        }
                    }
                    curElem = curElem.findElement(By.xpath("./.."));
                    String elemHTML = curElem.getAttribute("outerHTML");
                    noDiff = noDiff || elemHTML.contains("<span class=\"css-1qmrb3c\">");
                    sameLength = sameLength || elemHTML.contains("<span class=\"css-9bkuni\">");
                    contentDiff = contentDiff || elemHTML.contains("<span class=\"css-16ay0pa\">");
                    lengthDiff = lengthDiff || elemHTML.contains("<span class=\"css-8iwzw6\">");
                }
            }

        } finally {
        }

        System.out.println(String.format("no-diff: %s, same-length: %s, content-diff: %s, length-diff: %s", noDiff, sameLength,
                contentDiff, lengthDiff));
    }
}
