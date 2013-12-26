# twitter-producer

Reads the Twitter streaming API, extracts the hashtags and the created_date,
and posts that data to a Kinesis stream, using the hashtag as the partition
key. This enables a Kinesis application to perform things like counting the
most popular hashtags during a certain time window.

## Installation

Make sure [Leiningen 2+](http://leiningen.org/) is installed.

Build a stand-alone executable jar like this:

    $ lein uberjar

## Usage

The utility can be run either from Leiningen or as a stand-alone jar:

    $ lein run -- [args]

    $ java -jar twitter-producer-<VERSION>-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

## License

Copyright © 2013 Ulrik Sandberg

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
