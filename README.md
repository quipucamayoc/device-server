# Quipucamayoc Device Server

ClojureScript codebase that utilizes Noble to interact with BLE Wearables.

# Core project details

Visit the [Quipucamayoc](http://quipucamayoc.com/) website for further details and latest information.

# Requirements

Current primary build tool: Leiningen.

Clojure: 1.6+
ClojureScript: Latest

# Documentation

Run `lein marg -d doc/ -f index.html` to generate the documentation.

# Running

Although this is tightly coupled to the rest of the project you can run a quick demo setup (no BLE devices needed) via:

```sh
lein cljsbuild once core
node run/connect.js
```

Useful just to see the dashboard operation.

# Copyright

©2015 [Boris Kourtoukov](http://boris.kourtoukov.com/) & [Ayllu Intiwatana Team](http://quipucamayoc.com/)

# License

See `LICENSE` in root of repository.