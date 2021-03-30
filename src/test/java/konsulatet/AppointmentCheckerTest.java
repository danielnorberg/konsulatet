package konsulatet;

import java.lang.invoke.MethodHandles;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppointmentCheckerTest {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @BeforeClass
  public static void beforeClass() {
    Assume.assumeTrue(seleniumConfigured());
  }

  private static boolean seleniumConfigured() {
    try {
      var driver = AppointmentChecker.createDriver();
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
      var available = c.checkappointments(office);
      log.info("office appts available: {}", available);
    }
  }
}
