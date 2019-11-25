This package lives in a non-standard location for `jitsi-utils` 
(`org.jitsi.service` as opposed nested beneath `org.jitsi.utils` like 
everything else), because the `ConfigurationService` definition was migrated 
from `libjitsi`, and the `libjitsi` bundle activator relies on implementations 
having the same package names as their service definitions, but with`service` 
swapped for `impl`.  In an effort to minimize the change there, the `service` 
packages lives at the top level in `jitsi-utils`.