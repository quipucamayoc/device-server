# Quipucamayoc Device Server

ClojureScript codebase that utilizes Noble to interact with BLE Wearables.

## Core project details

Visit the [Quipucamayoc](http://quipucamayoc.com/) website for further details and latest information.

## Requirements

Current primary build tool: Leiningen.

Clojure: 1.6 (1.7-alpha6 or higher recommended)<br>
ClojureScript: Latest

### Node/iojs dependencies

Can be installed with `lein`:

**If using `nvm`:**

```sh
nvm install iojs-v1.6.4
lein npm install
```

Replace `iojs-v1.6.4` with your desired node/iojs version.

**If using system node:**

``sh
lein npm install
``


## Documentation

Run `lein marg -d doc/ -f index.html` to generate the documentation.

## Running

If you have the BLE devices ready and turned on you can simply run:

```sh
lein cljsbuild once core
node connect.js
```

Can also be started in demo (device free) mode by:

```sh
lein cljsbuild once core
node connect.js sample
```

Note: if using `nvm` run `nvm install iojs-v1.6.4` before running.

## Dashboard

Has been moved to the [dashboard repository](https://github.com/quipucamayoc/dashboard).

## License

See `LICENSE` in root of repository.

## Copyright

©2015 [Boris Kourtoukov](http://boris.kourtoukov.com/) & [Ayllu Intiwatana Team](http://quipucamayoc.com/)