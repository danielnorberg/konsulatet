package konsulatet;

import static java.time.temporal.ChronoUnit.HOURS;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.sinch.xms.*;
import com.sinch.xms.api.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
class SmsSender {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Path SMS_SENT_DIR = Paths.get("sms-sent");

  private static final String ACCOUNT_ID = System.getenv("SINCH_PLAN_ID");
  private static final String AUTH_TOKEN = System.getenv("SINCH_TOKEN");

  private static final List<String> RECIPIENTS_NUMBERS =
      Splitter.on(',')
          .trimResults()
          .omitEmptyStrings()
          .splitToList(System.getenv().getOrDefault("RECIPIENTS_NUMBERS", ""));
  private static final String SENDER_NUMBER = System.getenv("SENDER_NUMBER");

  private final ApiConnection sinchConnection;

  static boolean isConfigured() {
    return Stream.of(ACCOUNT_ID, AUTH_TOKEN, SENDER_NUMBER).noneMatch(Strings::isNullOrEmpty)
        && !RECIPIENTS_NUMBERS.isEmpty();
  }

  SmsSender() {
    Preconditions.checkState(isConfigured(), "not configured, please set env vars");
    this.sinchConnection = ApiConnection
                .builder()
                .servicePlanId(ACCOUNT_ID)
                .token(AUTH_TOKEN)
                .start();
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
    for (String notificationNumber : RECIPIENTS_NUMBERS) {
      MtBatchTextSmsCreate message =
          SinchSMSApi
              .batchTextSms()
              .sender(SENDER_NUMBER)
              .addRecipient(notificationNumber)
              .body(messageBody)
              .build();

      try {
        MtBatchTextSmsResult batch = sinchConnection.createBatch(message);
        log.info("Sent message to {}: {}", notificationNumber, message);
      } catch (Exception e) {
        log.error("Failed to send message to {}: {}\nDue to error: {}", notificationNumber, message, e.getMessage());
      }
    }
  }

  void sendHello() {
    sendSMS0("Hello World");
  }
}
