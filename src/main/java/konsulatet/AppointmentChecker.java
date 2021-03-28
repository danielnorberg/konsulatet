package konsulatet;

import com.google.common.util.concurrent.Uninterruptibles;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AppointmentChecker implements AutoCloseable {

  public static final By VISERINGSTYP = By.id("viseringstyp");
  public static final By ANTALPERSONER = By.id("antalpersoner");
  public static final By FORTSATT = By.name("fortsatt");
  public static final By TIDSBOKNING_PANEL = By.className("tidsbokningPanel");

  private final WebDriver driver;

  public AppointmentChecker() {
    WebDriverManager.chromedriver().setup();
    driver = new ChromeDriver();
  }

  @Override
  public void close() {
    driver.close();
  }

  public boolean checkappointments() {
    WebDriverWait wait = new WebDriverWait(driver, 300, 10);
    driver.get(
        "https://www.migrationsverket.se/ansokanbokning/valjtyp?51&enhet=U0766&sprak=sv&callback=https:/swedenabroad.se/en/about-abroad-for-swedish-citizens/usa/service-for-swedish-citizens/passports/passports-for-adults/");

    var viseringsTyp = wait.until(ExpectedConditions.visibilityOfElementLocated(VISERINGSTYP));
    var selectViseringsTyp = new Select(viseringsTyp);
    selectViseringsTyp.selectByVisibleText("ansöka om svenskt pass/id-handlingar");

    var antalPersoner = wait.until(ExpectedConditions.visibilityOfElementLocated(ANTALPERSONER));
    var selectAntalPersoner = new Select(antalPersoner);
    selectAntalPersoner.selectByVisibleText("1");

    for (int i = 0; i < 100; i++) {
      try {
        var fortsatt = wait.until(ExpectedConditions.visibilityOfElementLocated(FORTSATT));
        fortsatt.click();
        break;
      } catch (StaleElementReferenceException e) {
        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
      }
    }

    wait.until(ExpectedConditions.visibilityOfElementLocated(TIDSBOKNING_PANEL));
    var errors = driver.findElements(By.cssSelector(".feedbackPanelERROR > .feedbackPanelERROR"));
    if (errors.isEmpty()) {
      return true;
    }

    var foundAppointmentError =
        errors.stream().anyMatch(e -> e.getText().contains("inga mer lediga tider"));

    if (!foundAppointmentError) {
      return true;
    }

    return false;
  }
}
