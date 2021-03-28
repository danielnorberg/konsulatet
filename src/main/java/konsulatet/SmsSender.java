package konsulatet;

import static java.time.temporal.ChronoUnit.HOURS;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SmsSender {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Path SMS_SENT_FILE = Paths.get("sms-sent");

  private static final String ACCOUNT_SID = System.getenv("TWILIO_SID");
  private static final String AUTH_TOKEN = System.getenv("TWILIO_TOKEN");

  private static final String NOTIFICATION_NUMBER = System.getenv("NOTIFICATION_NUMBER");
  private static final String TWILIO_NUMBER = System.getenv("TWILIO_NUMBER");

  public static void main(String[] args) throws IOException {
    var sender = new SmsSender();
    sender.sendSMS("hello world");
    Files.deleteIfExists(SMS_SENT_FILE);
  }

  public SmsSender() {
    Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
  }

  void sendSMS(String message) throws IOException {
    var now = Instant.now();
    if (Files.exists(SMS_SENT_FILE)) {
      var lastSent = Instant.parse(Files.readString(SMS_SENT_FILE).trim());
      if (lastSent.isAfter(now.minus(1, HOURS))) {
        System.err.printf("Sent notification SMS %s, throttling...%n", lastSent);
        return;
      }
    }
    sendSMS0(message);
    Files.writeString(SMS_SENT_FILE, now.toString());
  }

  private void sendSMS0(String messageBody) {
    Message message =
        Message.creator(
                new PhoneNumber(NOTIFICATION_NUMBER), // to
                new PhoneNumber(TWILIO_NUMBER), // from
                messageBody)
            .create();
    log.info("Sent message: {} with body {}", message, messageBody);
  }

  public void sendHello() {
    sendSMS0("Hello World");
  }
}
