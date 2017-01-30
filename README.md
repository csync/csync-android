# Contextual Sync
Contextual Sync (CSync) is an open source, real-time, continuous data synchronization service for building modern applications. The CSync data store is organized with key/values where keys have a hierarchical structure. Clients can obtain the current value for a key and any subsequent updates by listening on the key. Updates are delivered to all online clients in near-real time. Clients can also listen on a key pattern where some components contain wildcards. 

## Keys
CSync is structured as a tree of nodes referenced by period-delimited strings called keys.

To illustrate :

```
          companies
       /              \
    ibm                google
   /   \               /     \ 
stock   offices    stock   offices
```

The above tree consists of the following keys : `companies`, `companies.ibm`, `companies.google`, `companies.ibm.stock`, `companies.ibm.offices`, `companies.google.stock`, `companies.google.offices`. Any one of these keys can be listened to at a time and all changes to that singular node will be synced to the client device. 

### Key Limitations
Keys can have a maximum of 16 parts and a total length of 200 characters. Key components may contain only uppercase and lowercase alphabetic, numeric, "_", and "-".

Valid key: `this.is.a.valid.key.123456.7.8.9.10`

Invalid key: `this is an.invalidkey.🍕.4.5.6.7.8.9.10.11.12.13.14.15.16.17.18`

### Wildcards in Keys
Suppose a developer wishes to listen to a subset of the tree containing multiple nodes, CSync provides this ability through wildcards. Currently CSync supports `*` and `#` as wildcards. 

#### Asterisk Wildcard
An Asterisk (`*`) wildcard will match any value in the part of the key where the wildcard is. As an example, if a developer listens to `companies.*.stock` in the above tree, the client will sync with all stock nodes for all companies.

#### Hash Wildcard
If a developer wishes to listen to all child nodes in a subset of the tree, the `#` can appended to the end of a key and the client will sync with all child nodes of the specified key. For instance in the above tree if a user listens to `companies.ibm.#`, then the client will sync with all child nodes of `companies.ibm` which include `companies.ibm.stock` and `companies.ibm.offices`.

**Note:** Each listen is independent. For example, if a developer listens to both `companies.*.stock` and `companies.companyX.stock`, the data from `companies.companyX.stock` will be received by both of the listeners. 

## Guaranteed Relevance
Only the latest, most recent, values sync, so you’re never left with old data. CSync provides a consistent view of the values for keys in the CSync store. If no updates are made to a key for a long enough period of time, all subscribers to the key will see the same consistent value. CSync guarantees that the latest update will be reflected at all connected, subscribed clients, but not that all updates to a key will be delivered. Clients will not receive an older value than what they have already received for a given key.

## Local Storage
Work offline, read and write, and have data automatically sync the next time you’re connected. CSync maintains a local cache of data that is available to the client even when the client is offline or otherwise not connected to the CSync service. The client may perform listens, writes, and deletes on the local store while offline. When the client reestablishes connectivity to the CSync service, the local cache is efficiently synchronized with the latest data from the CSync store. The local cache is persistent across application restarts and device reboots.

## Authentication
Authenticate in an ever-growing number of ways from the provider of your choice. Currently the following methods are supported:
- [Google OAuth](https://developers.google.com/identity/protocols/OAuth2) `google`
- [Github Auth](https://developer.github.com/v3/oauth/) `github`
- Demo Login `demo`

### Demo Login
The Demo Login is an easy way of getting started with CSync. Just provide the `demo` authentication provider and the `demoToken` to authenticate as a demo user. This token allows for multiple user accounts as long as it is in the form `demoToken({someString})`. For example: `demoToken(abc)`, `demoToken` and `demoToken(123)` would all be treated as different user accounts.

## Access Controls
Use simple access controls to clearly state who can read and write, keeping your data safe. Each key in the CSync store has an associated access control list (ACL) that specifies which users can access the key. 

Three specific forms of access are defined:
- Create: Users with create permission may create child keys of this key.
- Read: Users with read permission may read the data for the key.
- Write: Users with write permission may write the data for the key.

The creator of a key in the CSync store has special permissions to that key. In particular, the creator always has Read, Write, and Create permissions, and they also have permission to delete the key and change its ACL.

CSync provides eight "static" ACLs that can be used to provide any combination of Read, Write, and Create access to just the key's creator or all users.
- Private
- PublicRead
- PublicWrite
- PublicCreate
- PublicReadWrite
- PublicReadCreate
- PublicWriteCreate
- PublicReadWriteCreate

The ACL for a key is set when the key is created by the first write performed to the key. If the write operation specified an ACL, then this ACL is attached to the key. If no ACL was specified in the write, then the key inherits the ACL from its closest ancestor in the key space—its parent if the parent exists, else its grandparent if that key exists, possibly all the way back to the root key. The ACL of the root key is `PublicCreate`, which permits any user to create a child key but does not allow public read or write access.

# Getting Started

## Installation

1. Clone this repo.
2. Open Android Studio and select `Open an existing Android Studio project`.
3. Choose the `csync-android/build.gradle` file to open (This step might take a little while).
4. After the csync-android project actually opens in Android Studio, close it then open your project in Android Studio.
5. `File -> Project Structure` in Android Studio.
6. Tap the `+` symbol in the top left of the window that appears to add a new module.
7. Choose `Import .JAR/.AAR Package`.
8. In the next window to appear, choose the `...` to select a file for `File name`
9. Choose `csync-android/build/outputs/csync-android-debug.aar`.
10. Make the `Subproject name` "CSync" and click Finish
11. Download [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
12. In `build.gradle` (for Module: app), in the `dependencies` section, add `compile project(':CSync');`
13. Add the following to the top of your `build.gradle` (Module: app):

        buildscript {
            repositories {
                mavenCentral()
            }

            dependencies {
                classpath 'me.tatarka:gradle-retrolambda:3.2.5'
            }
        }

        repositories {
            mavenCentral()
        }


14. Then in the same `build.gradle`, look for `apply plugin: 'com.android.application'` and add `apply plugin: 'me.tatarka.retrolambda'` right below it.
15. Again in your app `build.gradle`, add a `compileOptions` section inside the `android` section:

        android {
            ...
            compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
        }

16. Add the following to your dependencies section of your `build.gradle`:

        compile group:'com.google.code.gson', name:'gson', version:'2.3.1'
        compile 'io.reactivex:rxjava:1.0.14'
        compile group:'org.java-websocket', name:'Java-WebSocket', version:'1.3.0'
        compile 'io.reactivex:rxandroid:1.0.1'

# Usage

## Creating a CSync instance
```
CSApp csApp = new CSApp.Builder(<host>, <port>)
    .useSSL(false)
    .build();
```
CSApp uses in memory caching as a default option. To use sqlite instead, use the following changes when creating a CSApp instance:
```
CSApp csApp = new CSApp.Builder(<host>, <port>)
    .useSSL(false)
    .cache(context)
    .build();
```

## Connecting to CSync
```
csApp.authenticate(PROVIDER, TOKEN);
```

## Creating a `CSKey`
```
CSKey myKey = CSKey.fromString("your.pattern");
```

## Listening to values on a key
```
csApp.listen(myKey)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .map(response -> new Gson().fromJson(response.data, YourPojo.class))
    .subscribe(yourPojo -> {
        Log.d("TAG", yourPojo + "");
    });
```

## Writing a value to a CSync store
```
csApp.write(myKey, dataToWrite, <acl>);
```

where `<acl>` can be any of the ACLs mentioned [above](https://github.ibm.com/csync/csync-android#access-controls). 

## Deleting a key
```
csApp.delete(myKey);
```


# License
This library is licensed under Apache 2.0. Full license text is
available in [LICENSE](LICENSE).
