# Guice Persist / jOOQ Integration

A simple integration between Guice's [persistence extensions](https://code.google.com/p/google-guice/wiki/GuicePersist) and [jOOQ](http://jooq.org/).  Follows closely in the pattern of of the JPA persistence extension written by Dhanji R. Prasanna (dhanji@gmail.com).

## Maven Coordinates

The project is deployed to Maven Central:

	<dependency>
	   <groupId>com.adamlewis</groupId>
	   <artifactId>guice-persist-jooq</artifactId>
	   <version>0.1.2</version>
	</dependency>

## Basic Usage
See [Guice Persist](https://github.com/google/guice/wiki/GuicePersist) and [Transactions and Units of Work](https://github.com/google/guice/wiki/Transactions) for a reference on the basic semantics of the Guice Persist extension.

In your module, install a new `com.adamlewis.guice.persist.jooq.JooqPersistModule` and then provide bindings for `javax.sql.DataSource` and `org.jooq.SQLDialect`. Then write `@Inject`able DAOs which depend on a `com.google.inject.Provider` for `org.jooq.DSLContext`. **This is very important**: don't inject `DSLContext` directly, always use a provider - the `DSLContext` is bound to the current thread and using a provider ensures that the DAO will access the current's thread `DSLContext`. This also allows the `@Transactional` annotation to properly intercept the method call and modify the current thread's `DSLContext`.

## Example

Here is an example Guice module written to connect guice-persist-jooq up to the [Dropwizard](https://dropwizard.github.io/dropwizard/) connection factory:

	import javax.sql.DataSource;

	import org.jooq.SQLDialect;

	import com.adamlewis.guice.persist.jooq.JooqPersistModule;
	import com.google.inject.AbstractModule;
	import com.google.inject.Provides;
	import com.yammer.dropwizard.db.DatabaseConfiguration;
	import com.yammer.dropwizard.db.ManagedDataSource;
	import com.yammer.dropwizard.db.ManagedDataSourceFactory;

	public class MyPersistenceModule extends AbstractModule {

		private final DatabaseConfiguration configuration;
		
		public MyPersistenceModule(final DatabaseConfiguration configuration) {
			this.configuration = configuration;
		}
		
		@Override
		protected void configure() {
			install(new JooqPersistModule());
			bind(DataSource.class).to(ManagedDataSource.class);
		}
		

		@Provides ManagedDataSource dataSource(final ManagedDataSourceFactory factory) throws ClassNotFoundException {
			return factory.build(configuration);
		}
		
		@Provides
		public SQLDialect dialect() {
			//TODO read from DB configuration
			return SQLDialect.POSTGRES;
		}
	}

And here is an example of what a DAO might look like:

	public class UserDao {
		// Note the usage of Provider,
		// instead of directly injecting DSLContext
		private final Provider<DSLContext> create;
		
		@Inject
		public UserDao(final Provider<DSLContext> dsl) {
			this.create = dsl;
		}
		
		
		public List<String> getUsernames() {
			return create.get().selectDistinct(User.USER.NAME).from(User.USER).fetch(User.USER.NAME);
		}	
	}
