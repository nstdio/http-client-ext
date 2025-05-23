# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased](https://github.com/nstdio/http-client-ext/compare/v2.4.0...HEAD)

## [v2.4.0](https://github.com/nstdio/http-client-ext/compare/v2.3.2...v2.4.0)

### ⭐  Features
 -  Support zstd compression via zstd-jni. ([c7e8775](https://github.com/nstdio/http-client-ext/commit/c7e877555386e1e21b71451e75392fb74cd90cb8))
### ♻️ Improvements
 -  Replace synchronized method with locks in SynchronizedCache. ([38d09af](https://github.com/nstdio/http-client-ext/commit/38d09af866cf79960956e5db3becaabd53585af4))
 -  Replace synchronized with ReentrantLock in PathSubscriber. ([0733b26](https://github.com/nstdio/http-client-ext/commit/0733b260c37bf6dd92f815e276e5da3a31dde142))
## [v2.3.2](https://github.com/nstdio/http-client-ext/compare/v2.3.1...v2.3.2)

### 🐞  Bug Fixes
 -  Decompression lenient mode fails on unknown directives. ([e816aa0](https://github.com/nstdio/http-client-ext/commit/e816aa019f77a78ed4cc7678b1860523195821e8))
### ♻️ Improvements
 -  Use empty headers when there is no value in builder. ([88d2bb8](https://github.com/nstdio/http-client-ext/commit/88d2bb8b2b7d1786d299ff5cf43cf87c273e5fd5))
 -  Use fixed thread pool to perform disk I/O. ([6349789](https://github.com/nstdio/http-client-ext/commit/6349789959e027dc960abbbad1005224fe2f231a))
 -  Improve task cancellation. ([b7b8223](https://github.com/nstdio/http-client-ext/commit/b7b8223be7eb694314fd5e221a8126fe7d7e33e5))
 -  Json publisher produces Content-Length header. ([ee94d87](https://github.com/nstdio/http-client-ext/commit/ee94d873b713a3ef13ca70d101a82c02f5b84273))
## [v2.3.1](https://github.com/nstdio/http-client-ext/compare/v2.3.0...v2.3.1)

### ♻️ Improvements
 -  Use read readable byte channel instead. ([c92c230](https://github.com/nstdio/http-client-ext/commit/c92c23029f5cc967e18511eb52946e644107e2a1))
 -  Increase write performance by using bulk writes when possible. ([59a08f0](https://github.com/nstdio/http-client-ext/commit/59a08f0e379e4d69558dbecda9c9b2c5a1a96be4))
 -  Do not duplicate byte buffer if it has no capacity. ([913b611](https://github.com/nstdio/http-client-ext/commit/913b61120c40bb62987fbcf456965cebae5dea2b))
 -  Improve performance of ByteBufferInputStream#skip. ([2b245da](https://github.com/nstdio/http-client-ext/commit/2b245daf339b09772c275ea5c65a21eed21eb3dc))
 -  Optimize cache invalidation. ([bfe58ef](https://github.com/nstdio/http-client-ext/commit/bfe58efe421a636469b7981479acce78cf5ff073))
## [v2.3.0](https://github.com/nstdio/http-client-ext/compare/v2.2.2...v2.3.0)

### ⭐  Features
 -  Publishing JSON body. ([79a9799](https://github.com/nstdio/http-client-ext/commit/79a97998f27500a461d47da20697cebe4ddb9b33))
### ♻️ Improvements
 -  Remove accidentally leaked kotlin stdlib dependency. ([ea66180](https://github.com/nstdio/http-client-ext/commit/ea66180b0634daf66133ec34939c07f5c9e870f4))
## [v2.2.2](https://github.com/nstdio/http-client-ext/compare/v2.2.1...v2.2.2)

### 🐞  Bug Fixes
 -  Do not duplicate headers when configured via builder. ([d4e4051](https://github.com/nstdio/http-client-ext/commit/d4e40513cdd928273928f18b65e91694e907a495))
## [v2.2.1](https://github.com/nstdio/http-client-ext/compare/v2.2.0...v2.2.1)

### 🐞  Bug Fixes
 -  Safely remove entry from the cache. ([cfbb458](https://github.com/nstdio/http-client-ext/commit/cfbb4586cf9f48fa6f0733c54e98e245b8a79b31))
 -  Amend cache holder map's loadFactor. ([8dad0df](https://github.com/nstdio/http-client-ext/commit/8dad0df4106f54a3ded249215eedc0896d9134b6))
## [v2.2.0](https://github.com/nstdio/http-client-ext/compare/v2.1.3...v2.2.0)

### ⭐  Features
 -  Add possibility to add default headers to request. ([0dd3ffc](https://github.com/nstdio/http-client-ext/commit/0dd3ffc96e1dd27b7290c204a0f7931174a3114d))
 -  Add Predicates to match header value and presence. ([adaa821](https://github.com/nstdio/http-client-ext/commit/adaa8213a04512ebf21689c3704ca98820688399))
 -  Flag to disable insecure HTTP requests via ExtendedHttpClient. ([bf90fb9](https://github.com/nstdio/http-client-ext/commit/bf90fb98c29e7841d0e59a459cdd1a2d7e58f0d4))
### ♻️ Improvements
 -  Improve insecure request exception message. ([5a8cb08](https://github.com/nstdio/http-client-ext/commit/5a8cb08625d3552ee8e37dd0616eaee65ebf8133))
 -  **doc**  Add Javadoc to `DecompressingBodyHandlerBuilder#lenient` method. ([cb96f95](https://github.com/nstdio/http-client-ext/commit/cb96f9551fb6ad494494ae67dd26e6975b1f358a))
 -  Reformat code. ([e9ac5c9](https://github.com/nstdio/http-client-ext/commit/e9ac5c939c0c28d946af9b188c83578f190e5dac))
## [v2.1.3](https://github.com/nstdio/http-client-ext/compare/v2.1.2...v2.1.3)

### ⭐  Features
 -  Cache interface now extends Closeable ([2e9076a](https://github.com/nstdio/http-client-ext/commit/2e9076a7ae337c55c5817d15ddab9c8e9e3e5bb5))
### ♻️ Improvements
 -  Improve cache write. ([b7f1289](https://github.com/nstdio/http-client-ext/commit/b7f128900372ee033aaef79b11ab09ed65f5c0ce))
## [v2.1.2](https://github.com/nstdio/http-client-ext/compare/v2.1.1...v2.1.2)

### 🐞  Bug Fixes
 -  NPE when no `Content-Encoding` header present. ([b3afc61](https://github.com/nstdio/http-client-ext/commit/b3afc610c593024c013185ba93a3a3e7988bd9ca))
### ♻️ Improvements
 -  **perf**  Tune decompression performance. ([1c3c581](https://github.com/nstdio/http-client-ext/commit/1c3c581f52db95cf4d88bc6dcea9e420b3015b5f))
## [v2.1.1](https://github.com/nstdio/http-client-ext/compare/v2.1.0...v2.1.1)

### ⭐  Features
 -  Replace JSON metadata serializers with binary version. ([9c3c576](https://github.com/nstdio/http-client-ext/commit/9c3c5762ea52f15396a4bb1982d202a178706653))
 -  Dynamic JsonMappingProvider registration. ([4975edd](https://github.com/nstdio/http-client-ext/commit/4975edd1e5cbf5ed08953e04aaf79166f1678c58))
 -  Add `must-understand` CacheControl header directive support. ([f0582bf](https://github.com/nstdio/http-client-ext/commit/f0582bf0104488a902d89cc4925291e7b74b4846))
### ♻️ Improvements
 -  Optimize encrypted file header length reading ([7cd8c1f](https://github.com/nstdio/http-client-ext/commit/7cd8c1fabd15b28f91c02dea4aaa9dd80c5d489e))
 -  **perf**  Store Cipher instance in ThreadLocal. ([1e1a5a1](https://github.com/nstdio/http-client-ext/commit/1e1a5a1443dd50f6148df1a936d3f43fadee96f7))
## [v2.1.0](https://github.com/nstdio/http-client-ext/compare/v2.0.0...v2.1.0)

### ⭐  Features
 -  Optional encryption for disk cache. ([3a1af2d](https://github.com/nstdio/http-client-ext/commit/3a1af2d7ffd832210cf588483699911b81817a15))
 -  Disk cache supports Gson as well. ([5f26718](https://github.com/nstdio/http-client-ext/commit/5f26718d22a88173efebee56e2855c13652744db))
### ♻️ Improvements
 -  **perf**  Optimize header value splitting. ([e1873c7](https://github.com/nstdio/http-client-ext/commit/e1873c7ab7694e15e69bec4186b0c7ce006debe5))
 -  **doc**  Add changelog. ([9ae5bcf](https://github.com/nstdio/http-client-ext/commit/9ae5bcf5ec4730d36e037401a8da168af76ed7b4))
 -  **doc**  Polish Javadoc. ([fbf12cc](https://github.com/nstdio/http-client-ext/commit/fbf12cc75f69e13de9f02bcdae339eb01487e165))
 -  **test**  Add test for asserting exception in JdkCompressionFactory. ([4498f0d](https://github.com/nstdio/http-client-ext/commit/4498f0d1fa43c10e1b84ca85e9a3221666a89861))
 -  **build**  Update Gradle 7.4.1 -> 7.4.2. ([260419d](https://github.com/nstdio/http-client-ext/commit/260419deff2a6402d1fc8d263412d0c0cc6953ac))
 -  **test**  Even more tests for HttpHeadersBuilder. ([ec66e86](https://github.com/nstdio/http-client-ext/commit/ec66e8653c2b2975370e95e0822ec6856932661c))
 -  **test**  More tests for HttpHeadersBuilder. ([125560b](https://github.com/nstdio/http-client-ext/commit/125560b3a7ed29a17514fcec0804a80337bbf176))
 -  **build**  Fix warning for unaligned target compatibility. ([8b743c3](https://github.com/nstdio/http-client-ext/commit/8b743c3eb587a890265b9d8e135981ef3e4582fc))
 -  **test**  Convert Java -> Kotlin. ([fa342ae](https://github.com/nstdio/http-client-ext/commit/fa342ae707f5e3cb4619aff6ca80654c7a216276))
 -  **doc**  Change Maven central badge ([740de64](https://github.com/nstdio/http-client-ext/commit/740de64fd62fd9e51b77c2adf98381ee660e820c))
## [v2.0.0](https://github.com/nstdio/http-client-ext/compare/v1.5.1...v2.0.0)

### ⭐  Features
 -  Add complex type support for JSON mappings. ([1617f77](https://github.com/nstdio/http-client-ext/commit/1617f77b7472af1ea325dd85c8135992fa2d4968))
 -  SPI for JSON mapping. ([70b8e0f](https://github.com/nstdio/http-client-ext/commit/70b8e0f7834dcc756ea753272cc974d7f26a2fd5))
 -  Proper JSON handlers and subscribers. ([25f1e2a](https://github.com/nstdio/http-client-ext/commit/25f1e2aba05ea81be42e77c4a3399daa6ce2a846))
### ♻️ Improvements
 -  **doc**  Update README.md ([63a7a35](https://github.com/nstdio/http-client-ext/commit/63a7a357a24f34b4c40695df9cbd0107cdb8e1b1))
 -  **ci**  Execute build on '2.x' branches ([8ab69c3](https://github.com/nstdio/http-client-ext/commit/8ab69c3093397b3d496775ac980002110c9952e7))
 -  Replace abstract provider class with interface. ([ce70b61](https://github.com/nstdio/http-client-ext/commit/ce70b61fb4a99828edf150087eddc1a813f1aee0))
 -  Reuse same JSON mapping instances. ([ec0ca6d](https://github.com/nstdio/http-client-ext/commit/ec0ca6d617729754ae70f45b3096c241e8cbc67d))
 -  Reformat code. ([6dda31e](https://github.com/nstdio/http-client-ext/commit/6dda31ec759632b2e0dc9d4ab4737b5ea346cd61))
## [v1.5.1](https://github.com/nstdio/http-client-ext/compare/v1.5.0...v1.5.1)

### 🐞  Bug Fixes
 -  **build**  Escape Kotlin string interpolation. ([1366d26](https://github.com/nstdio/http-client-ext/commit/1366d261b60a81a897013c07d94bf963ca390346))
## [v1.5.0](https://github.com/nstdio/http-client-ext/compare/v1.4.0...v1.5.0)

### ⭐  Features
 -  **cache**  Cache statistics. ([3ad0d64](https://github.com/nstdio/http-client-ext/commit/3ad0d646a6aed093ccd265d3e4828b3e6c49ea9a))
### 🐞  Bug Fixes
 -  **build**  Remove sourcesJar and javadocJar tasks from publishing. ([3d6d30e](https://github.com/nstdio/http-client-ext/commit/3d6d30e8eec75448976ea319e5714750ae906bde))
 -  Use byte arrays instead of InputStream for JSON subscribers. ([6eeeda6](https://github.com/nstdio/http-client-ext/commit/6eeeda66afae35f87fd9b7f955ecbe9ed0af780c))
### ♻️ Improvements
 -  Micro optimizations. ([0dc675b](https://github.com/nstdio/http-client-ext/commit/0dc675bef45c82acc43a03f667ca27dd2fb9565b))
 -  Increase test coverage. ([a3bfa92](https://github.com/nstdio/http-client-ext/commit/a3bfa927314b96780a8dce85014a78ea9643c171))
 -  Fix spiTest configuration. ([5d5dbbf](https://github.com/nstdio/http-client-ext/commit/5d5dbbf769007e1db22efbff0ddd70b04e458cc5))
 -  Change badges. ([b75532a](https://github.com/nstdio/http-client-ext/commit/b75532a8d57abe130c7756e66c3fb53d45fe65d6))
 -  **ci**  Use codecov for test coverage. ([2786e53](https://github.com/nstdio/http-client-ext/commit/2786e533c8eaac79e0b319398c4fd7f0188df391))
 -  Modernize build. ([ef6162f](https://github.com/nstdio/http-client-ext/commit/ef6162f98deedf324016ec2ca5d88bc8a1e20b05))
 -  Store interceptors as fields. ([95771c7](https://github.com/nstdio/http-client-ext/commit/95771c7b625b1048b97eac34b48a544635154b59))
 -  Shorten module configuration. ([fedf8b8](https://github.com/nstdio/http-client-ext/commit/fedf8b8d6e81e2270cc74a5e1c5a045e966816ae))
## [v1.4.0](https://github.com/nstdio/http-client-ext/compare/v1.3.0...v1.4.0)

### ⭐  Features
 -  **comp**  SPI for compression. ([3eb9061](https://github.com/nstdio/http-client-ext/commit/3eb9061030cff8f741077ccf48fc6b3dc8fdfcb0))
### ♻️ Improvements
 -  Fix test coverage. ([669443e](https://github.com/nstdio/http-client-ext/commit/669443e9ed3aa939c9aaeb315b9ff44350d27d4a))
 -  Refactor DecompressingSubscriber. ([da229eb](https://github.com/nstdio/http-client-ext/commit/da229ebbec1cbd74548439eedc1524d6cbb61676))
 -  Address Sonar issues. ([b0194db](https://github.com/nstdio/http-client-ext/commit/b0194dbafdab4ac905fdcda0cecbd5a150e05915))
 -  Optimize readNBytes for ByteBufferInputStream. ([d00a56f](https://github.com/nstdio/http-client-ext/commit/d00a56f18afb0fd1f68b6b994dca94e1a4711fb9))
 -  Polish javadoc. ([7cb13e9](https://github.com/nstdio/http-client-ext/commit/7cb13e9e7583a6032f94bbe9834757edcde17244))
## [v1.3.0](https://github.com/nstdio/http-client-ext/compare/v1.2.2...v1.3.0)

### ⭐  Features
 -  **cache**  Add support for stale-if-error Cache-Control extension. ([a42139f](https://github.com/nstdio/http-client-ext/commit/a42139f5d08b37232dd97cf3aa612076689e7ced))
### ♻️ Improvements
 -  Uncomment signing in build.gradle. ([f538899](https://github.com/nstdio/http-client-ext/commit/f53889995bc8e9a6b429cd1f95ff4863189e9866))
 -  Extract mark field to local variable. ([eeaf224](https://github.com/nstdio/http-client-ext/commit/eeaf224cd8543bcfb2e01def015942ce6d88630a))
 -  Enhance documentation for JSON handling. ([cbfc2b4](https://github.com/nstdio/http-client-ext/commit/cbfc2b4ae580d5b113b0d29eb9509fc0e18d6ce4))
 -  Extract common IO parts. ([843aac5](https://github.com/nstdio/http-client-ext/commit/843aac5f4932344992ca2a916fc2508fc818cdca))
## [v1.2.2](https://github.com/nstdio/http-client-ext/compare/v1.2.1...v1.2.2)

### ♻️ Improvements
 -  **deps**  Bump mockito-core from 4.3.1 to 4.4.0 ([a5dd1e2](https://github.com/nstdio/http-client-ext/commit/a5dd1e27b676680a5965bdf66df8df5b1eafedb5))
## [v1.2.1](https://github.com/nstdio/http-client-ext/compare/v1.2.0...v1.2.1)

## [v1.2.0](https://github.com/nstdio/http-client-ext/compare/v1.1.1...v1.2.0)

## [v1.1.1](https://github.com/nstdio/http-client-ext/compare/v1.0.3...v1.1.1)

### ⭐  Features
 -  Options to control failing on malformed, unknown directives. ([ba7604a](https://github.com/nstdio/http-client-ext/commit/ba7604ae981445dff84d11879f6744f140e85ea0))
### ♻️ Improvements
 -  **deps**  Bump json-path-assert from 2.6.0 to 2.7.0 ([c0c4c6f](https://github.com/nstdio/http-client-ext/commit/c0c4c6fcbf9213fb5c3747e11ba8f09a987c0271))
 -  **deps**  Bump assertj-core from 3.21.0 to 3.22.0 ([191a00f](https://github.com/nstdio/http-client-ext/commit/191a00f3f89003fa6001c6a6eb2ee23564b6b16f))
 -  **deps**  Bump junitVersion from 5.8.1 to 5.8.2 ([1e99729](https://github.com/nstdio/http-client-ext/commit/1e9972928fbfce90049c84bc497538ba877d158e))
 -  Reformat code indent. ([58af693](https://github.com/nstdio/http-client-ext/commit/58af693750da49bc89374ca1475a3ef2bce68db3))
 -  **deps**  Bump junitVersion from 5.8.0-RC1 to 5.8.1 ([12fb5de](https://github.com/nstdio/http-client-ext/commit/12fb5de65c7a2f092e02a8dd4bf72f714b6e1fda))
 -  **deps**  Bump assertj-core from 3.20.2 to 3.21.0 ([09b98ad](https://github.com/nstdio/http-client-ext/commit/09b98ad8e4375085cc14764e7d8ef72551d99c2c))
 -  **ci**  Update setup-java action version. ([3568fc2](https://github.com/nstdio/http-client-ext/commit/3568fc271d0dbe10f153dd60100251ca2d26538c))
 -  **ci**  Add dependabot.yml. ([a159eb0](https://github.com/nstdio/http-client-ext/commit/a159eb0a33c3bb82d600660745a74f43a6cbeeb7))
 -  **ci**  Publish Gradle build scans. ([cc11af9](https://github.com/nstdio/http-client-ext/commit/cc11af936434504bbbbc82ed5d6880df0b68795c))
 -  **ci**  Turn Gradle daemon off. ([7503489](https://github.com/nstdio/http-client-ext/commit/750348964e79c275b8ff9bdd083d4e4e32f60227))
 -  **ci**  Use setup-java's Gradle cache. ([cac390a](https://github.com/nstdio/http-client-ext/commit/cac390ab07fe510399229458165166fb8de8db1a))
## [v1.0.3](https://github.com/nstdio/http-client-ext/compare/v1.0.2...v1.0.3)

### ♻️ Improvements
 -  **ci**  Remove caching from release.yml. ([a7c385b](https://github.com/nstdio/http-client-ext/commit/a7c385b0d852a30bb56ab1d153e1c343117704f4))
## [v1.0.2](https://github.com/nstdio/http-client-ext/compare/v1.0.1...v1.0.2)

### 🐞  Bug Fixes
 -  Fix static import in tests. ([a5208fa](https://github.com/nstdio/http-client-ext/commit/a5208fafb501c5da4d248d824e56d873c6cebe8f))
### ♻️ Improvements
 -  Relocate all classes. ([0a89cb0](https://github.com/nstdio/http-client-ext/commit/0a89cb0a9a4b8a69dd575258b44865b30f2832f2))
 -  Add license. ([0f6fa1b](https://github.com/nstdio/http-client-ext/commit/0f6fa1b0c4b1a88c829e7fd66b67b4021c761453))
## [v1.0.1](https://github.com/nstdio/http-client-ext/releases/tag/v1.0.1)

### 🐞  Bug Fixes
 -  Configure git committer identity to perform release commits. ([e9d3a20](https://github.com/nstdio/http-client-ext/commit/e9d3a208bb6b0efe1bc32a39027e238cd86d4181))
 -  Use InflaterInputStream when compression is deflate. ([b7598fe](https://github.com/nstdio/http-client-ext/commit/b7598fedcc160263954cadae7ab9683a46e138ee))
### ♻️ Improvements
 -  Prepare for release. ([9ddfdf3](https://github.com/nstdio/http-client-ext/commit/9ddfdf321c9830da8537597bb761e7b7468f4c4a))
 -  Rename package com.github.nstdio -> io.github.nstdio. ([f1718bc](https://github.com/nstdio/http-client-ext/commit/f1718bc6c5036230f57d9a1f2ed13f2b33307aa7))
 -  Change project groupId com.github.nstdio -> io.github.nstdio. ([6e32a4f](https://github.com/nstdio/http-client-ext/commit/6e32a4f9a32ca325313df34b04ee87852d95ca2f))
 -  Polish build.gradle. ([2f1dc7d](https://github.com/nstdio/http-client-ext/commit/2f1dc7dbe9aedba7171a38f197c0e901f319365b))
 -  **ci**  Add workflow to validate Gradle Wrapper. ([d7f6dac](https://github.com/nstdio/http-client-ext/commit/d7f6dac962c44af1cf823d5f79329ef5dfc94a56))
 -  **doc**  Add Sonarcloud metrics badges. ([28d8ab7](https://github.com/nstdio/http-client-ext/commit/28d8ab7d41044e85bc99bee9c725853ff3b874a2))
 -  **ci**  Generate JaCoCo XML report on CI. ([b507cbc](https://github.com/nstdio/http-client-ext/commit/b507cbc028589d25314bdd1a4a661a6df9165079))
 -  **ci**  Integrate JaCoCo. ([8f687b2](https://github.com/nstdio/http-client-ext/commit/8f687b26e90628d0993546224942927dc54bd6f7))
 -  **ci**  Integrate Sonarcloud. ([14e37c8](https://github.com/nstdio/http-client-ext/commit/14e37c8b3a4a466a11db690592b8cac0493ccb47))
