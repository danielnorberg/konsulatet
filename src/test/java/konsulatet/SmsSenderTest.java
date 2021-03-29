package konsulatet;

import java.io.IOException;
import org.junit.Assume;
import org.junit.Test;

public class SmsSenderTest {

  @Test
  public void testSend() throws IOException {
    Assume.assumeTrue(SmsSender.isConfigured());
    var sender = new SmsSender();
    sender.sendSMS("foo", "bar");
  }
}
