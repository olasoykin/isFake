package com.isfake.app.data.repository;

import com.isfake.app.data.datastore.PreferencesDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class LocationRepositoryImpl_Factory implements Factory<LocationRepositoryImpl> {
  private final Provider<PreferencesDataStore> dataStoreProvider;

  public LocationRepositoryImpl_Factory(Provider<PreferencesDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public LocationRepositoryImpl get() {
    return newInstance(dataStoreProvider.get());
  }

  public static LocationRepositoryImpl_Factory create(
      Provider<PreferencesDataStore> dataStoreProvider) {
    return new LocationRepositoryImpl_Factory(dataStoreProvider);
  }

  public static LocationRepositoryImpl newInstance(PreferencesDataStore dataStore) {
    return new LocationRepositoryImpl(dataStore);
  }
}
