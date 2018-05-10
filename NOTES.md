# Release Notes

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
