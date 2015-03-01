##<(◕___◕)>

- [ ] Add configurable permissions, using java security manager, to restrict scripts access to system services such as file, network, etc.
- [ ] Investigate JNDIConfiguration to see if we could overlay it on top of the base configuration just before invoking PokerFace.config (aka support ldap configuration)
- [ ] Add support for selectively caching responses from remote Targets; Maybe use Guava Cache.
- [ ] Add more reverse proxy specific unit tests
- [ ] Add session / authentication support to allow for routing rules based on that information.
