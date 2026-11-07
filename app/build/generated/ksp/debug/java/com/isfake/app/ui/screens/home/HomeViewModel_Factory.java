package com.isfake.app.ui.screens.home;

import android.content.Context;
import com.isfake.app.data.repository.LocationRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<LocationRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public HomeViewModel_Factory(Provider<LocationRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<LocationRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new HomeViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static HomeViewModel newInstance(LocationRepository repository, Context context) {
    return new HomeViewModel(repository, context);
  }
}
