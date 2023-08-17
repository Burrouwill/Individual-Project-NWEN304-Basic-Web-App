Packages Used: 

a-take-2@0.0.0
cookie-parser@1.4.6
cors@2.8.5
debug@2.6.9
dotenv@16.3.1
express@4.16.4
http-errors@1.6.3
jade@1.11.0
mongodb@5.7.0
mongoose@7.4.2
morgan@1.9.1
nodemon@3.0.1


Installation & Running:

** From memory most of the above came with installing express, except for Mongo & Mongoose ** 

Prior To Running run these in terminal to ensure required packages are up to date:

  npm install express
  npm install mongoose
  npm install mongodb

Running the Server:

  Type: npm start inside the terminal when inside the "Server" folder (**\IndividualProject\Section A\Server)
  This will start the server on a local port & Connect to my MongoDB server.
  Once this is done, you can open the index.html file within the Client folder(**\IndividualProject\Section A\Client\public(js, htcm, css in here)) & test the CORE & COMPLETION functionality. 

  NOTE: There are two buttons that I implemented (SaveToMongo & LoadMongo) that work but were not required for the Challenge part. 

  There is a txt doccument in the same folder as this README for testing of COMPLETION MongoDB functionality using PowerShell Curl commands. 





Full Dependency Tree:

 PS C:\Users\wgrbu\Desktop\COMPSCI\T2\NWEN304\IndividualProject\Section A> npm list
a-take-2@0.0.0 C:\Users\wgrbu\Desktop\COMPSCI\T2\NWEN304\IndividualProject\Section A
+-- cookie-parser@1.4.6
| +-- cookie@0.4.1
| `-- cookie-signature@1.0.6
+-- cors@2.8.5
| +-- object-assign@4.1.1
| `-- vary@1.1.2
+-- debug@2.6.9
| `-- ms@2.0.0
+-- dotenv@16.3.1
+-- express@4.16.4
| +-- accepts@1.3.8
| | +-- mime-types@2.1.35
| | | `-- mime-db@1.52.0
| | `-- negotiator@0.6.3
| +-- array-flatten@1.1.1
| +-- body-parser@1.18.3
| | +-- bytes@3.0.0
| | +-- content-type@1.0.5 deduped
| | +-- debug@2.6.9
| | | `-- ms@2.0.0 deduped
| | +-- depd@1.1.2 deduped
| | +-- http-errors@1.6.3
| | | +-- depd@1.1.2 deduped
| | | +-- inherits@2.0.3 deduped
| | | +-- setprototypeof@1.1.0 deduped
| | | `-- statuses@1.4.0 deduped
| | +-- iconv-lite@0.4.23
| | | `-- safer-buffer@2.1.2
| | +-- on-finished@2.3.0 deduped
| | +-- qs@6.5.2 deduped
| | +-- raw-body@2.3.3
| | | +-- bytes@3.0.0 deduped
| | | +-- http-errors@1.6.3
| | | | +-- depd@1.1.2 deduped
| | | | +-- inherits@2.0.3 deduped
| | | | +-- setprototypeof@1.1.0 deduped
| | | | `-- statuses@1.4.0 deduped
| | | +-- iconv-lite@0.4.23 deduped
| | | `-- unpipe@1.0.0 deduped
| | `-- type-is@1.6.18 deduped
| +-- content-disposition@0.5.2
| +-- content-type@1.0.5
| +-- cookie@0.3.1
| +-- cookie-signature@1.0.6 deduped
| +-- debug@2.6.9
| | `-- ms@2.0.0 deduped
| +-- depd@1.1.2
| +-- encodeurl@1.0.2
| +-- escape-html@1.0.3
| +-- etag@1.8.1
| +-- finalhandler@1.1.1
| | +-- debug@2.6.9
| | | `-- ms@2.0.0 deduped
| | +-- encodeurl@1.0.2 deduped
| | +-- escape-html@1.0.3 deduped
| | +-- on-finished@2.3.0 deduped
| | +-- parseurl@1.3.3 deduped
| | +-- statuses@1.4.0 deduped
| | `-- unpipe@1.0.0
| +-- fresh@0.5.2
| +-- merge-descriptors@1.0.1
| +-- methods@1.1.2
| +-- on-finished@2.3.0
| | `-- ee-first@1.1.1
| +-- parseurl@1.3.3
| +-- path-to-regexp@0.1.7
| +-- proxy-addr@2.0.7
| | +-- forwarded@0.2.0
| | `-- ipaddr.js@1.9.1
| +-- qs@6.5.2
| +-- range-parser@1.2.1
| +-- safe-buffer@5.1.2
| +-- send@0.16.2
| | +-- debug@2.6.9
| | | `-- ms@2.0.0 deduped
| | +-- depd@1.1.2 deduped
| | +-- destroy@1.0.4
| | +-- encodeurl@1.0.2 deduped
| | +-- escape-html@1.0.3 deduped
| | +-- etag@1.8.1 deduped
| | +-- fresh@0.5.2 deduped
| | +-- http-errors@1.6.3
| | | +-- depd@1.1.2 deduped
| | | +-- inherits@2.0.3 deduped
| | | +-- setprototypeof@1.1.0 deduped
| | | `-- statuses@1.4.0 deduped
| | +-- mime@1.4.1
| | +-- ms@2.0.0 deduped
| | +-- on-finished@2.3.0 deduped
| | +-- range-parser@1.2.1 deduped
| | `-- statuses@1.4.0 deduped
| +-- serve-static@1.13.2
| | +-- encodeurl@1.0.2 deduped
| | +-- escape-html@1.0.3 deduped
| | +-- parseurl@1.3.3 deduped
| | `-- send@0.16.2 deduped
| +-- setprototypeof@1.1.0
| +-- statuses@1.4.0
| +-- type-is@1.6.18
| | +-- media-typer@0.3.0
| | `-- mime-types@2.1.35 deduped
| +-- utils-merge@1.0.1
| `-- vary@1.1.2 deduped
+-- http-errors@1.6.3
| +-- depd@1.1.2 deduped
| +-- inherits@2.0.3
| +-- setprototypeof@1.1.0 deduped
| `-- statuses@1.4.0 deduped
+-- jade@1.11.0
| +-- character-parser@1.2.1
| +-- clean-css@3.4.28
| | +-- commander@2.8.1
| | | `-- graceful-readlink@1.0.1
| | `-- source-map@0.4.4
| |   `-- amdefine@1.0.1
| +-- commander@2.6.0
| +-- constantinople@3.0.2
| | `-- acorn@2.7.0
| +-- jstransformer@0.0.2
| | +-- is-promise@2.2.2
| | `-- promise@6.1.0
| |   `-- asap@1.0.0
| +-- mkdirp@0.5.6
| | `-- minimist@1.2.8
| +-- transformers@2.1.0
| | +-- css@1.0.8
| | | +-- css-parse@1.0.4
| | | `-- css-stringify@1.0.5
| | +-- promise@2.0.0
| | | `-- is-promise@1.0.1
| | `-- uglify-js@2.2.5
| |   +-- optimist@0.3.7
| |   | `-- wordwrap@0.0.3
| |   `-- source-map@0.1.43
| |     `-- amdefine@1.0.1 deduped
| +-- uglify-js@2.8.29
| | +-- source-map@0.5.7
| | +-- uglify-to-browserify@1.0.2
| | `-- yargs@3.10.0
| |   +-- camelcase@1.2.1
| |   +-- cliui@2.1.0
| |   | +-- center-align@0.1.3
| |   | | +-- align-text@0.1.4
| |   | | | +-- kind-of@3.2.2
| |   | | | | `-- is-buffer@1.1.6
| |   | | | +-- longest@1.0.1
| |   | | | `-- repeat-string@1.6.1
| |   | | `-- lazy-cache@1.0.4
| |   | +-- right-align@0.1.3
| |   | | `-- align-text@0.1.4 deduped
| |   | `-- wordwrap@0.0.2
| |   +-- decamelize@1.2.0
| |   `-- window-size@0.1.0
| +-- void-elements@2.0.1
| `-- with@4.0.3
|   +-- acorn@1.2.2
|   `-- acorn-globals@1.0.9
|     `-- acorn@2.7.0 deduped
+-- mongodb@5.7.0
| +-- bson@5.4.0
| +-- mongodb-connection-string-url@2.6.0
| | +-- @types/whatwg-url@8.2.2
| | | +-- @types/node@20.4.8
| | | `-- @types/webidl-conversions@7.0.0
| | `-- whatwg-url@11.0.0
| |   +-- tr46@3.0.0
| |   | `-- punycode@2.3.0
| |   `-- webidl-conversions@7.0.0
| +-- saslprep@1.0.3
| | `-- sparse-bitfield@3.0.3
| |   `-- memory-pager@1.5.0
| `-- socks@2.7.1
|   +-- ip@2.0.0
|   `-- smart-buffer@4.2.0
+-- mongoose@7.4.2
| +-- bson@5.4.0 deduped
| +-- kareem@2.5.1
| +-- mongodb@5.7.0 deduped
| +-- mpath@0.9.0
| +-- mquery@5.0.0
| | `-- debug@4.3.4
| |   `-- ms@2.1.2
| +-- ms@2.1.3
| `-- sift@16.0.1
+-- morgan@1.9.1
| +-- basic-auth@2.0.1
| | `-- safe-buffer@5.1.2 deduped
| +-- debug@2.6.9
| | `-- ms@2.0.0 deduped
| +-- depd@1.1.2 deduped
| +-- on-finished@2.3.0 deduped
| `-- on-headers@1.0.2
`-- nodemon@3.0.1
  +-- chokidar@3.5.3
  | +-- anymatch@3.1.3
  | | +-- normalize-path@3.0.0 deduped
  | | `-- picomatch@2.3.1
  | +-- braces@3.0.2
  | | `-- fill-range@7.0.1
  | |   `-- to-regex-range@5.0.1
  | |     `-- is-number@7.0.0
  | +-- UNMET OPTIONAL DEPENDENCY fsevents@2.3.2
  | +-- glob-parent@5.1.2
  | | `-- is-glob@4.0.3 deduped
  | +-- is-binary-path@2.1.0
  | | `-- binary-extensions@2.2.0
  | +-- is-glob@4.0.3
  | | `-- is-extglob@2.1.1
  | +-- normalize-path@3.0.0
  | `-- readdirp@3.6.0
  |   `-- picomatch@2.3.1 deduped
  +-- debug@3.2.7
  | `-- ms@2.1.3
  +-- ignore-by-default@1.0.1
  +-- minimatch@3.1.2
  | `-- brace-expansion@1.1.11
  |   +-- balanced-match@1.0.2
  |   `-- concat-map@0.0.1
  +-- pstree.remy@1.1.8
  +-- semver@7.5.4
  | `-- lru-cache@6.0.0
  |   `-- yallist@4.0.0
  +-- simple-update-notifier@2.0.0
  | `-- semver@7.5.4 deduped
  +-- supports-color@5.5.0
  | `-- has-flag@3.0.0
  +-- touch@3.1.0
  | `-- nopt@1.0.10
  |   `-- abbrev@1.1.1
  `-- undefsafe@2.0.5