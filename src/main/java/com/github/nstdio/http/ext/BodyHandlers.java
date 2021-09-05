package com.github.nstdio.http.ext;

import java.io.InputStream;
import java.net.http.HttpResponse.BodyHandler;

/**
 * Implementations of {@code BodyHandler}'s.
 */
public final class BodyHandlers {
  private BodyHandlers() {
  }

  /**
   * @return The decompressing body handler.
   */
  public static BodyHandler<InputStream> ofDecompressing() {
    return new DecompressingBodyHandler();
  }

}
