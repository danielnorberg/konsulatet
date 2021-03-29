package konsulatet;

import static java.util.stream.Collectors.toList;

import com.google.common.util.concurrent.Uninterruptibles;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppointmentChecker implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final By VISERINGSTYP = By.id("viseringstyp");
  private static final By ANTALPERSONER = By.id("antalpersoner");
  private static final By FORTSATT = By.name("fortsatt");
  private static final By TIDSBOKNING_PANEL = By.className("tidsbokningPanel");

  public static final String KONSULATET_NYC = "konsulatet-nyc";
  public static final String AMBASSADEN_DC = "ambassaden-dc";

  private static final Map<String, String> offices =
      Map.of(
          KONSULATET_NYC,
              "https://www.migrationsverket.se/ansokanbokning/valjtyp?51&enhet=U0766&sprak=sv&callback=https:/swedenabroad.se/en/about-abroad-for-swedish-citizens/usa/service-for-swedish-citizens/passports/passports-for-adults/",
          AMBASSADEN_DC,
              "https://www.migrationsverket.se/ansokanbokning/valjtyp?5&enhet=U1075&sprak=sv&callback=http://www.swedenabroad.com/en-GB/Embassies/Washington/Services-for-Swedes/Passport-Applications-and-Name-Registration/");

  private final WebDriver driver;

  AppointmentChecker() {
    WebDriverManager.chromedriver().setup();
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless");
    driver = new ChromeDriver(options);
  }

  @Override
  public void close() {
    driver.quit();
  }

  Collection<String> offices() {
    return offices.keySet();
  }

  boolean checkappointments(String office) {
    var wait = new WebDriverWait(driver, 300, 10);

    var url = offices.get(office);
    log.debug("opening {} url: {}", office, url);
    driver.get(url);

    log.debug("waiting for visit type selector");
    var viseringsTyp = wait.until(ExpectedConditions.visibilityOfElementLocated(VISERINGSTYP));
    var selectViseringsTyp = new Select(viseringsTyp);
    log.debug("selecting passport visit");
    selectViseringsTyp.selectByVisibleText("ansöka om svenskt pass/id-handlingar");

    log.debug("waiting for nr of persons selector");
    var antalPersoner = wait.until(ExpectedConditions.visibilityOfElementLocated(ANTALPERSONER));
    var selectAntalPersoner = new Select(antalPersoner);
    log.debug("selecting 1 person");
    selectAntalPersoner.selectByVisibleText("1");

    for (int i = 0; i < 100; i++) {
      try {
        log.debug("waiting for continue button");
        var fortsatt = wait.until(ExpectedConditions.visibilityOfElementLocated(FORTSATT));
        log.debug("clicking continue button");
        fortsatt.click();
        break;
      } catch (StaleElementReferenceException e) {
        log.debug("continue button was stale, retrying");
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
      } catch (ElementClickInterceptedException e) {
        log.debug("continue button was not yet clickable, retrying");
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
      }
    }

    log.debug("waiting for booking panel");
    wait.until(ExpectedConditions.visibilityOfElementLocated(TIDSBOKNING_PANEL));
    log.debug("looking for booking errors");
    var errors = driver.findElements(By.cssSelector(".feedbackPanelERROR > .feedbackPanelERROR"));
    if (errors.isEmpty()) {
      log.debug("no booking errors, success!");
      return true;
    }

    var errorMessages = errors.stream().map(WebElement::getText).collect(toList());
    log.debug("found appointment errors: {}", errorMessages);
    var foundNoMoreSlotsError =
        errorMessages.stream().anyMatch(m -> m.contains("inga mer lediga tider"));

    if (!foundNoMoreSlotsError) {
      log.debug("did not find no more slots error, maybe success?");
      return true;
    }

    log.debug("found no more slots error");
    return false;
  }
}
