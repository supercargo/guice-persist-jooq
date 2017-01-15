package com.adamlewis.guice.persist.jooq.utils;

import com.google.inject.Provider;

public class Providers {
  private Providers() {
  }

  public static <T> Provider<T> of(T value) {
    return new SimpleProvider<T>(value);
  }

  private static class SimpleProvider<T> implements Provider<T> {
    private T value;

    public SimpleProvider(T value) {
      this.value = value;
    }

    public T get() {
      return value;
    }
  }
}
