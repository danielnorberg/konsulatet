package konsulatet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MainTest {

  private static final Map<String, String> OFFICES =
      Map.of("foo", "http://foo.example.com/", "bar", "http://bar.example.com/");

  @Mock SmsSender smsSender;
  @Mock AppointmentChecker appointmentChecker;

  private Main main;

  private CompletableFuture<Void> f;

  @Before
  public void setUp() {
    when(appointmentChecker.offices()).thenReturn(OFFICES);
    main = new Main(smsSender, appointmentChecker);
  }

  @After
  public void tearDown() throws Exception {
    main.close();
    if (f != null) {
      f.cancel(true);
      try {
        f.get(30, TimeUnit.SECONDS);
      } catch (CancellationException | InterruptedException | ExecutionException e) {
        // ignore
      }
    }
  }

  @Test
  public void shouldSendSmsWhenApptFound() {
    when(appointmentChecker.checkappointments(anyString())).thenReturn(true);
    f = CompletableFuture.runAsync(() -> main.check());
    OFFICES.forEach(
        (office, url) ->
            verify(smsSender, timeout(30_000).atLeastOnce())
                .sendSMS(
                    office, "Appointments might be available at " + office + ", check at " + url));
  }

  @Test
  public void shouldSendSmsWhenCrashing() {
    when(appointmentChecker.checkappointments(anyString()))
        .thenThrow(new RuntimeException("Failed!"));
    f = CompletableFuture.runAsync(() -> main.check());
    verify(smsSender, timeout(30_000).atLeastOnce()).sendCrash(anyString());
  }

  @Test
  public void shouldSendSmsWhenStarting() {
    when(appointmentChecker.checkappointments(anyString())).thenReturn(false);
    f = CompletableFuture.runAsync(() -> main.check());
    verify(smsSender, timeout(30_000).atLeastOnce()).sendHello();
  }
}
