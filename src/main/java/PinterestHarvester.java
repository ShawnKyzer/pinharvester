import com.jaunt.*;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;


/**
 * Created by shawnkyzer on 12/15/15.
 */
public class PinterestHarvester {

    private static final String TOKEN = "";
    private static final String OPTIONAL_FIELDS = "fields=id%2Clink%2Cnote%2Curl%2Cattribution%2Cmedia%2Cmetadata%2Cboard%2Ccolor%2Ccounts%2Ccreated_at%2Ccreator%2Cimage%2Coriginal_link";


    public static void main(String[] args){
        // 1. Create a connection and get a response to pinterest
        pinterestSearch();

    }

    private static void getAllPinsFromContent(String pageSource) {
        try{
            UserAgent userAgent = new UserAgent();

            // Each  time the visit URL will have to be modified
             userAgent.openContent(pageSource);

            Elements elements = userAgent.doc.findEvery("<a class=\"socialItem\">");              //find all divs in the document

            System.out.println("Every div: " + elements.size() + " results");  //report number of search results.

            for(Element div : elements){
                //iterate through Results
                String hrefURL = div.getAt("href");
                System.out.println( hrefURL + "\n----\n");      //print each element and its contents
                String pinId = hrefURL.split("/")[2];
                System.out.println( pinId + "\n----\n");      //print each element and its contents
                // Now with the PIN id we will make a rest call to the API to get the pin data
                getPinData(pinId);
            }

        }
        catch(JauntException e){
            System.err.println(e);
        }
    }
    private static void getAllBoardsFromContent(String pageSource) {
        try{
            UserAgent userAgent = new UserAgent();
            // Each  time the visit URL will have to be modified

            userAgent.openContent(pageSource);

            Elements elements = userAgent.doc.findEvery("<a class=\"boardLinkWrapper\">");              //find all divs in the document

            System.out.println("Every div: " + elements.size() + " results");  //report number of search results.

            for(Element div : elements){
                //iterate through Results
                String hrefURL = div.getAt("href");
                System.out.println( hrefURL + "\n----\n");      //print each element and its contents
                String pinId = hrefURL.substring(1, hrefURL.length()-1);
                System.out.println( pinId + "\n----\n");      //print each element and its contents
                // Now with the PIN id we will make a rest call to the API to get the pin data

                JNode currentNodeElement = getBoardData(pinId, "");

                while (currentNodeElement!=null){
                    writeJsonToFile(currentNodeElement.toString());
                    currentNodeElement = getBoardData(null, currentNodeElement.findFirst("next").toString().replaceAll("\\\\",""));
                }

            }

        }
        catch(JauntException e){
            System.err.println(e);
        }
    }

    private static JNode getBoardData(String boardId, String alternateCursorURL) {
            String optionalFields = "fields=id%2Clink%2Cnote%2Curl%2Cattribution%2Cmedia%2Cmetadata%2Cboard%2Ccolor%2Ccounts%2Ccreated_at%2Ccreator%2Cimage%2Coriginal_link";
            // Make a separate call here to get all the JSON data
            try{
                UserAgent userAgent = new UserAgent();         //create new userAgent (headless browser).

                if(alternateCursorURL.isEmpty()) {
                    userAgent.sendGET("https://api.pinterest.com/v1/boards/" + boardId + "/pins/?access_token=" + TOKEN + "&" + optionalFields);   //send request
                } else {
                    userAgent.sendGET(alternateCursorURL);   //send request
                }
                System.out.println("Other response data: " + userAgent.response); //response metadata, including headers.

                return userAgent.json;
            }
            catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
                System.err.println(e);
            }
            return  null;
    }

    public static Boolean pinterestPinSearch(WebDriver driver, String searchTerms ,int timeOutInMins) {
        int totalTime = timeOutInMins * 60000; // in millseconds
        long startTime = System.currentTimeMillis();
        boolean timeEnds = false;
        driver.get("https://www.pinterest.com/search/pins/?q="+searchTerms);
        waitForPageLoaded(driver);
        while (!timeEnds) {
            scrollToBottom(driver);
            waitForPageLoaded(driver);
            timeEnds = (System.currentTimeMillis() - startTime >= totalTime);
        }
        System.out.println("Not Found");
        return false;
    }

    public static Boolean pinterestBoardSearch(WebDriver driver, String searchTerms , int timeOutInMins) {
        int totalTime = timeOutInMins * 60000; // in millseconds
        long startTime = System.currentTimeMillis();
        boolean timeEnds = false;
        driver.get("https://www.pinterest.com/search/boards/?q="+searchTerms);
        waitForPageLoaded(driver);
        while (!timeEnds) {
            scrollToBottom(driver);
            waitForPageLoaded(driver);
            timeEnds = (System.currentTimeMillis() - startTime >= totalTime);
        }
        System.out.println("Not Found");
        return false;
    }

    public static void pinterestSearch() {
        String searchTerms = "before and after weightloss";
        DesiredCapabilities DesireCaps = new DesiredCapabilities();
        DesireCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "/usr/local/Cellar/phantomjs/2.0.0/bin/phantomjs");
        WebDriver driver=new PhantomJSDriver(DesireCaps);
        // loginToPinterest(searchTerms, driver);
        pinterestBoardSearch(driver, searchTerms, 1);
      //  getAllPinsFromContent(driver.getPageSource());
        getAllBoardsFromContent(driver.getPageSource());
    }

    private static void loginToPinterest(String searchTerms, WebDriver driver) {
        driver.get("https://www.pinterest.com/logout");
        driver.manage().window().setSize(new Dimension(5000,5000));
        driver.get("https://www.pinterest.com/login");
        waitForPageLoaded(driver);
        driver.findElement(By.xpath("/html/body/div/div[1]/div/div[1]/div/button[2]")).click();
        waitForPageLoaded(driver);
        driver.findElement(By.xpath("//*[@id=\"userEmail\"]")).sendKeys("shawnkyzer@yahoo.com");
        driver.findElement(By.xpath("//*[@id=\"userPassword\"]")).sendKeys("789Grand");
        driver.findElement(By.xpath("/html/body/div/div[6]/div/div[2]/div/div/div/div/div[2]/div[2]/div[1]/div/div[2]/form/div/ul/div[1]/div[2]/li[2]/div/button")).click();
        waitForPageLoaded(driver);
        pinterestPinSearch(driver, searchTerms, 1);//timeOut in Mins
    }

    private static void scrollToBottom(WebDriver driver) {
        long longScrollHeight = (Long) ((JavascriptExecutor) driver).executeScript("return Math.max("
                + "document.body.scrollHeight, document.documentElement.scrollHeight,"
                + "document.body.offsetHeight, document.documentElement.offsetHeight,"
                + "document.body.clientHeight, document.documentElement.clientHeight);"
        );
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, " + longScrollHeight + ");");
    }

    public static void waitForPageLoaded(WebDriver driver) {
        ExpectedCondition<Boolean> expectation = new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                return ((JavascriptExecutor) driver).executeScript(
                        "return document.readyState").equals("complete");
            }
        };
        WebDriverWait wait = new WebDriverWait(driver, 20);
        wait.until(expectation);
    }

    private static JNode getPinData(String pinId){
        // Make a separate call here to get all the JSON data

        try{
            UserAgent userAgent = new UserAgent();         //create new userAgent (headless browser).
            userAgent.sendGET("https://api.pinterest.com/v1/pins/"+pinId+"/?access_token="+ TOKEN +"&"+ OPTIONAL_FIELDS);   //send request
            System.out.println(userAgent.json);            //print the retrieved JSON object
            System.out.println("Other response data: " + userAgent.response); //response metadata, including headers.
        return  userAgent.json;

        }
        catch(JauntException e){         //if an HTTP/connection error occurs, handle JauntException.
            System.err.println(e);
        }
        return  null;
    }

    private static void writeJsonToFile(String jsonContent){
        try {

            File file = new File("pinterest/"+System.currentTimeMillis()+".JSON");

            FileUtils.writeStringToFile(file, jsonContent);

            System.out.println("Done");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
