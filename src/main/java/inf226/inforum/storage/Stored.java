package inf226.inforum.storage;

import java.util.UUID;

public class Stored<T> {
  public final T value;
  public final UUID identity;
  public final UUID version;

  public Stored(T value) {
    this.value = value;
    this.identity = UUID.randomUUID();
    this.version = UUID.randomUUID();
  }

  public Stored(T value, UUID identity, UUID version) {
    this.value = value;
    this.identity = identity;
    this.version = version;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final Stored<T> stored_other = (Stored<T>) other;
    return this.identity.equals(stored_other.identity)
        && this.version.equals(stored_other.version)
        && this.value.equals(stored_other.value);

  }

  Stored<T> newVersion(T newValue) {
     return new Stored<T>(newValue , identity, UUID.randomUUID());
  }
}

