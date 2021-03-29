package konsulatet;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"UnstableApiUsage"})
public class Main implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final SmsSender smsSender;
  private final AppointmentChecker appointmentChecker;
  private volatile boolean closed;

  public Main() {
    this(new SmsSender(), new AppointmentChecker());
  }

  public Main(SmsSender smsSender, AppointmentChecker appointmentChecker) {
    this.smsSender = Objects.requireNonNull(smsSender, "smsSender");
    this.appointmentChecker = Objects.requireNonNull(appointmentChecker, "appointmentChecker");
  }

  public static void main(String[] args) {
    var checker = new Main();
    checker.check();
  }

  @Override
  public void close() {
    closed = true;
  }

  public void check() {
    smsSender.sendHello();
    try {
      check0();
    } catch (Throwable t) {
      smsSender.sendCrash("Appt checker crash: " + t);
      Throwables.throwIfUnchecked(t);
      throw new RuntimeException(t);
    }
  }

  private void check0() throws IOException {
    while (!closed) {
      for (String office : appointmentChecker.offices()) {
        log.info("Checking for appointments at {}", office);
        var maybeAvailable = appointmentChecker.checkappointments(office);
        if (maybeAvailable) {
          log.info("Appointments might be available at {}, notifying!", office);
          smsSender.sendSMS(office, "Appointments might be available at " + office);
        } else {
          log.info("No appointments available at {}", office);
        }
      }
      Uninterruptibles.sleepUninterruptibly(30, TimeUnit.SECONDS);
    }
  }
}
