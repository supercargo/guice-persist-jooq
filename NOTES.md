# Release Notes

## Version 2.0.0
Thanks to @apptio-msobala for contributing the major changes and testing of this release
- (fix) Reuse acquired connection in transaction configuration
- (enhancement) Use Guice _Optional_ binder for `Configuration` and `Settings`
- (enhancement) Reduce repeated warnings about ignored Settings (logged on Service construction only)
- (breaking) Drop Java 7 support.  Requires Java 8 or above.
- (dependency) Bumped jOOQ minor version to 3.14 (last OSS version supporting Java 8)
- (dependency) Bumped Guice to 5.1.0


## Version 1.1.0
- (enhancement) `DataSource` is now injected to the (normally singleton) service using a Guice `Provider` to allow the 
  transaction context's scope control over data source creation.  Thanks @mrohan01


## Version 1.0.0
- (breaking) Injector provided `DSLContext` no longer starts `UnitOfWork` automatically.  Clients must explicitly 
control transaction boundaries or use `@Transactional` annotations; fixes #9
- (dependency) Removed dependency on Guava
- (dependency) jOOQ is now `provided` scope
- (dependency) `slf4j-api` upgraded from `1.7.5` to `1.7.25`

## Version 0.2.0
- Added several tests for transaction interceptor
- Fix issue where auto-commit wouldn't be re-enabled in certain execption cases

## Version 0.1.5
- `Configuration` `@Inject`ions now properly optional; fixes #4

## Version 0.1.4
- Added jOOQ `Configuration` as injectable property

## Version 0.1.2
- Bumped jOOQ major version (3.5.0)

## Version 0.1.1
- Initial public release
