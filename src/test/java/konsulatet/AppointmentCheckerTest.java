package konsulatet;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class AppointmentCheckerTest {

  @BeforeClass
  public static void beforeClass() {
    Assume.assumeTrue(seleniumConfigured());
  }

  private static boolean seleniumConfigured() {
    try {
      WebDriverManager.chromedriver().setup();
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--headless");
      var driver = new ChromeDriver(options);
      try {
        driver.get("https://www.google.com/");
        driver.close();
      } finally {
        driver.quit();
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Test
  public void checkappointments() {
    var c = new AppointmentChecker();
    for (String office : c.offices().keySet()) {
      c.checkappointments(office);
    }
  }
}
