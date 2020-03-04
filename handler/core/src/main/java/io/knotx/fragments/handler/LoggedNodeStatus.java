package io.knotx.fragments.handler;

import io.knotx.fragments.engine.EventLogEntry;

public enum LoggedNodeStatus {
  SUCCESS,
  ERROR,
  OTHER,
  MISSING,
  UNPROCESSED;

  public static LoggedNodeStatus from(EventLogEntry logEntry) {
    switch (logEntry.getStatus()) {
      case ERROR:
      case TIMEOUT:
        return ERROR;
      case UNPROCESSED:
        return UNPROCESSED;
      case UNSUPPORTED_TRANSITION:
        return MISSING;
      case SUCCESS:
        return "_success".equals(logEntry.getTransition()) ? SUCCESS : OTHER;
      default:
        throw new IllegalArgumentException();
    }
  }
}
