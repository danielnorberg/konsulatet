package konsulatet;

import static java.util.stream.Collectors.toList;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
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
  // There is no semantic queryable name for this checkbox (the id is "id1")
  private static final By KORREKT_AMBASSAD = By.name("control.panel:control.checkbox");

  public static final String KONSULATET_NYC = "konsulatet-nyc";
  public static final String AMBASSADEN_DC = "ambassaden-dc";

  private static final Map<String, String> offices =
      Map.of(
          KONSULATET_NYC,
              "https://www.migrationsverket.se/ansokanbokning/valjtyp?enhet=U0766&sprak=en",
          AMBASSADEN_DC,
              "https://www.migrationsverket.se/ansokanbokning/valjtyp?enhet=U1075&sprak=en");

  private final WebDriver driver;

  AppointmentChecker() {
    this(createDriver());
  }

  static ChromeDriver createDriver() {
    WebDriverManager.chromedriver().setup();
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless");
    return new ChromeDriver(options);
  }

  public AppointmentChecker(WebDriver driver) {
    this.driver = driver;
  }

  @Override
  public void close() {
    driver.quit();
  }

  Map<String, String> offices() {
    return offices;
  }

  boolean checkappointments(String office) {
    var wait =
        new WebDriverWait(driver, 300, 10)
            .ignoring(StaleElementReferenceException.class)
            .ignoring(ElementClickInterceptedException.class)
            .ignoring(NoSuchElementException.class);

    var url = offices.get(office);
    log.debug("opening {} url: {}", office, url);
    driver.get(url);

    log.debug("waiting for visit type selector");
    var viseringsTyp = wait.until(ExpectedConditions.visibilityOfElementLocated(VISERINGSTYP));
    var selectViseringsTyp = new Select(viseringsTyp);
    log.debug("selecting passport visit");
    selectViseringsTyp.selectByVisibleText("apply for Swedish passport or id document");

    log.debug("waiting for nr of persons selector");
    var antalPersoner = wait.until(ExpectedConditions.visibilityOfElementLocated(ANTALPERSONER));
    var selectAntalPersoner = new Select(antalPersoner);
    log.debug("selecting 1 person");
    selectAntalPersoner.selectByVisibleText("1");

    wait.until(
        d -> {
          log.debug("waiting for correct embassy checkbox");
          var korrektAmbassad = driver.findElement(KORREKT_AMBASSAD);
          log.debug("selecting that this is the correct embassy");
          korrektAmbassad.click();
          return true;
        });

    wait.until(
        d -> {
          log.debug("looking for continue button");
          var fortsatt = driver.findElement(FORTSATT);
          log.debug("clicking continue button");
          fortsatt.click();
          return true;
        });

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
        errorMessages.stream().anyMatch(m -> m.contains("no available time slots"));

    if (!foundNoMoreSlotsError) {
      log.debug("did not find no more slots error, maybe success?");
      return true;
    }

    log.debug("found no more slots error");
    return false;
  }
}
