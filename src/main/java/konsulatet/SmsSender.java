package konsulatet;

import static java.time.temporal.ChronoUnit.HOURS;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
class SmsSender {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Path SMS_SENT_DIR = Paths.get("sms-sent");

  private static final String ACCOUNT_SID = System.getenv("TWILIO_SID");
  private static final String AUTH_TOKEN = System.getenv("TWILIO_TOKEN");

  private static final List<String> NOTIFICATION_NUMBERS =
      Splitter.on(',')
          .trimResults()
          .omitEmptyStrings()
          .splitToList(System.getenv().getOrDefault("NOTIFICATION_NUMBERS", ""));
  private static final String TWILIO_NUMBER = System.getenv("TWILIO_NUMBER");

  static boolean isConfigured() {
    return Stream.of(ACCOUNT_SID, AUTH_TOKEN, TWILIO_NUMBER).noneMatch(Strings::isNullOrEmpty)
        && !NOTIFICATION_NUMBERS.isEmpty();
  }

  SmsSender() {
    Preconditions.checkState(isConfigured(), "not configured, please set env vars");
    Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
  }

  void sendSMS(String key, String message) {
    try {
      var now = Instant.now();
      var sentFile = SMS_SENT_DIR.resolve(key);
      if (Files.exists(sentFile)) {
        var lastSent = Instant.parse(Files.readString(sentFile).trim());
        if (lastSent.isAfter(now.minus(1, HOURS))) {
          log.info("Sent notification SMS {}, throttling. Message: {}", lastSent, message);
          return;
        }
      }
      sendSMS0(message);
      Files.createDirectories(SMS_SENT_DIR);
      Files.writeString(sentFile, now.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void sendCrash(String messageBody) {
    sendSMS0(messageBody);
  }

  private void sendSMS0(String messageBody) {
    for (String notificationNumber : NOTIFICATION_NUMBERS) {
      Message message =
          Message.creator(
                  new PhoneNumber(notificationNumber), // to
                  new PhoneNumber(TWILIO_NUMBER), // from
                  messageBody)
              .create();
      log.info("Sent message to {}: {}", notificationNumber, message);
    }
  }

  void sendHello() {
    sendSMS0("Hello World");
  }
}
