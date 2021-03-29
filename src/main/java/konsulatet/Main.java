package konsulatet;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"InfiniteLoopStatement", "UnstableApiUsage"})
public class Main {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final SmsSender smsSender;
  private final AppointmentChecker appointmentChecker;

  public Main() {
    this(new SmsSender(), new AppointmentChecker());
  }

  public Main(SmsSender smsSender, AppointmentChecker appointmentChecker) {
    this.smsSender = Objects.requireNonNull(smsSender, "smsSender");
    this.appointmentChecker = Objects.requireNonNull(appointmentChecker, "appointmentChecker");
  }

  public static void main(String[] args) {
    var checker = new Main();
    checker.hello();
    checker.check();
  }

  private void hello() {
    smsSender.sendHello();
  }

  public void check() {
    try {
      check0();
    } catch (Throwable t) {
      smsSender.sendCrash("Appt checker crash: " + t);
      Throwables.throwIfUnchecked(t);
      throw new RuntimeException(t);
    }
  }

  private void check0() throws IOException {
    while (true) {
      var maybeAvailable = appointmentChecker.checkappointments();
      if (maybeAvailable) {
        log.info("Appointments might be available, notifying!");
        smsSender.sendSMS("Swedish consulate appts might be available!");
      } else {
        log.info("No appointments available");
      }
      Uninterruptibles.sleepUninterruptibly(30, TimeUnit.SECONDS);
    }
  }
}
