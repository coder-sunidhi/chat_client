import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggerUtil {

    private static final Logger LOGGER =
            Logger.getLogger("ChatApplication");

    static {

        LOGGER.setUseParentHandlers(false);

        ConsoleHandler handler =
                new ConsoleHandler();

        handler.setLevel(Level.ALL);

        LOGGER.addHandler(handler);

        LOGGER.setLevel(Level.ALL);
    }

    private LoggerUtil() {

    }

    public static void info(String message) {

        LOGGER.info(message);
    }

    public static void warning(String message) {

        LOGGER.warning(message);
    }

    public static void error(String message,
                             Exception exception) {

        LOGGER.log(
                Level.SEVERE,
                message,
                exception);
    }
}
