## Coding guidelines

We are currently using Square's Code Style.

Visit the [Square's Code Style][square] where they have an
install script and three steps to setup code-style. With it installed, anytime you write code
just click reformat code and it will follow the guidelines of Square's code style.

[square]: https://github.com/square/java-code-styles

## Documentation

All code changes should include comments describing the design, assumptions, dependencies,
and non-obvious aspects of the implementation.
Hopefully the existing code provides a good example of appropriate code comments.
If necessary, make the appropriate updates in the README.md and other documentation files.

We use [javadoc][javadoc] to build documentation from comments in the source code.
All external interfaces should be fully documented.

Use `javadoc src/main/java/com/ibm/csync/*.java` from the root directory of the repo to build the docs.
You can also find the current SDK documentation [here][docs] on our website.

[javadoc]: http://docs.oracle.com/javase/1.5.0/docs/tooldocs/windows/javadoc.html
[docs]: http://csync.mybluemix.net/docs/android-api/index.html

## Contributing your changes

1. If one does not exist already, open an issue that your contribution is going to resolve or fix.
    1. Make sure to give the issue a clear title and a very focused description.
2. On the issue page, set the appropriate Pipeline, Label(s), Milestone, and assign the issue to
yourself.
    1. We use Zenhub to organize our issues and plan our releases. Giving as much information as to
    what your changes are help us organize PRs and streamline the committing process.
3. Make a branch from the develop branch using the following naming convention:
    1. `YOUR_INITIALS/ISSUE#-DESCRIPTIVE-NAME`
    2. For example, `kb/94-create-contributingmd` was the branch that had the commit containing this
    tutorial.
4. Commit your changes!
5. When you have completed making all your changes, create a Pull Request (PR) from your git manager
or our Github repo.
6. In the comment for the PR write `Resolves #___` and fill the blank with the issue number you
created earlier.
    1. For example, the comment we wrote for the PR with this tutorial was `Resolves #94`
7. That's it, thanks for the contribution!

## Setting up your environment

It is highly recommended to use the [Android Studio][android studio]
IDE when running and testing the SDK.

[android studio]: https://developer.android.com/studio/index.html

## Running the tests

The CSync Android SDK includes both unit and integration tests. To run the tests, open the SDK in Android Studio and right-click the test folder in the `java` directory and select `Run 'Tests in 'csync''`. To run coverage tests, right-click the test folder in the `java` directory and select `Run 'Tests in 'csync'' with Coverage`.

There are a few environment variables you can use to target the integration tests to a particular
CSync server. Use `keystore.properties` to point the SDK tests to your csync-server by setting the
following variables:
 - **`CSYNC_HOST=<hostname|ip>`**: hostname or ip address of CSync server
 - **`CSYNC_PORT=nnn`**: port number for the CSync service

By default the tests will run using the guest authorization provider. You can optionally set the
following variables to use a different provider with your own token:
 - **`CSYNC_DEMO_PROVIDER=<provider>`**: authentication provider supported by the csync server
 - **`CSYNC_DEMO_TOKEN=<token>`**: token used to authenticate with the specified provider

## Dependency Table
| Name | URL |License Type | Version | Need/Reason | Release Date | Verification Code |
|------|-----|-------------|---------|-------------|--------------|-------------------|
| rxjava | https://github.com/ReactiveX/RxJava | Apache 2.0 | 1.1.0  | Observables and Function programming paradigms  |  11-Feb-2016 |   |
| jsonwebtoken | https://github.com/jwtk/jjwt  | Apache 2.0 | 0.6.0 | Creating webtoken for authentication with server.  | 14-Oct-2015	 |   |
| gson | https://github.com/google/gson | Apache 2.0  | 2.3.1  | JSON serialization  |  26-Feb-2016	 |   |
| java-websocket | https://github.com/TooTallNate/Java-WebSocket | MIT | 1.3.0 | Websocket connection to server | 23-Apr-2013	|  |
