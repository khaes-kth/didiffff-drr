package sahabdiff;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class Tmp {
    private static final long SELENIUM_LOAD_WAIT_SEC = 1000L;
    private static final long SELENIUM_LONG_LOAD_WAIT_SEC = 10000L;

    public static void main(String[] args) throws InterruptedException {
        analyzeDidiffffResult(args);
    }

    private static void simpleMethod() {
        int i = 0;
        for(; i < 10; i++)
            System.out.println(i);
    }

    private static void analyzeDidiffffResult(String[] args) throws InterruptedException {
        String url = args[0];
        String[] pathParts = args[1].split("/"); // args[0] = "Math-1/src/java/main/x/y/z.java
        int startLine = Integer.parseInt(args[2]), endLine = Integer.parseInt(args[3]);

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
                curElem = curElem.findElement(By.xpath("//span[contains(text(),'" + pathParts[i] + "')]"));
                curElem.click();
                Thread.sleep(SELENIUM_LOAD_WAIT_SEC);
            }

            for(int i = startLine; i <= endLine; i++){
                curElem = driver.findElement(By.xpath("//span[@class='css-1p60ryf' and contains(text(),'" + i + "')]"));
                curElem = curElem.findElement(By.xpath("./.."));
                String elemHTML = curElem.getAttribute("outerHTML");
                noDiff = noDiff || elemHTML.contains("<span class=\"css-1qmrb3c\">");
                sameLength = sameLength || elemHTML.contains("<span class=\"css-9bkuni\">");
                contentDiff = contentDiff || elemHTML.contains("<span class=\"css-16ay0pa\">");
                lengthDiff = lengthDiff || elemHTML.contains("<span class=\"css-8iwzw6\">");
            }

        } finally {
        }

        System.out.println(String.format("no-diff: %s, same-length: %s, content-diff: %s, length-diff: %s", noDiff, sameLength,
                contentDiff, lengthDiff));
    }
}
