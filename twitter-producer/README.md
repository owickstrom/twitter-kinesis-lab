# twitter-producer

Reads the Twitter streaming API, extracts the hashtags and the created_date,
and posts that data to a Kinesis stream, using the hashtag as the partition
key. This enables a Kinesis application to perform things like counting the
most popular hashtags during a sliding time window.

## Installation

Make sure [Leiningen 2+](http://leiningen.org/) is installed.

## Usage

A few properties need to be set, containing an AWS access key with permissions
for Amazon Kinesis and OAuth credentials for the Twitter Streaming API:

* AWS access key, eg `AKIAJQSAKJ4HJEXAMPLE`
* AWS secret key, eg `somesecretawskey`
* AWS region, eg `eu-west-1`
* Twitter API consumer key, eg `somekey`
* Twitter API consumer secret, eg `somesecretagain`
* Twitter API access token, eg `12345678-AbcDeFGh`
* Twitter API access token secret, eg `somemumbojumbo`

We're using a utility called [environ][1] to be able to handle properties in the
same way, code-wise, whether we are in development, test, or production.

[1]: https://github.com/weavejester/environ

### Development

During development, the application is run from Leiningen like this:

    $ lein trampoline run

The `trampoline` thing simply kills the Leiningen JVM after the application has
started, thus "un-consuming" lots of memory.

Some parameters are required, so you will get a printout that looks like this:

    Some options are required

    This is the twitter-producer application, that gets tweets off the Twitter
    Streaming API, parses the Twitter hashtags, and posts each hashtag to the given
    Kinesis stream with a timestamp, sharding on the hashtag. A Kinesis client
    application downstream can then perform calculations on that data, for example
    sliding window counts.

    You must specify a Kinesis stream where to post the hashtags. That would perhaps
    be Hashtags-<teamname>. The stream will be created if not already existing.

    Usage: twitter-producer [-h] [-k N] -s STREAMNAME
      -s, --stream STREAMNAME     Kinesis stream name (required)
      -k, --shards N           1  Number of Kinesis shards; each 1000 writes/s and 5 reads/s
      -h, --help

In order to have the necessary properties available during development, place
them in a file called `profiles.clj`, which should look like:

    {:dev {:env {:aws-access-key-id "AKIAJQSAKJ4HJEXAMPLE"
                 :aws-secret-key "somesecretawskey"
                 :aws-region "eu-west-1"
                 :consumer-key "somekey"
                 :consumer-secret "somesecretagain"
                 :access-token "12345678-AbcDeFGh"
                 :access-token-secret "somemumbojumbo"}}}

NOTE: Make sure you don't accidentally add `profiles.clj` to source control.

The `:dev` profile will be used when running `lein run` or `lein repl`.
LightTable or other IDEs that have Leiningen integration can also be used.

If a `:test` environment is available, it will be used when running `lein test`
(should there be any tests that require any of these properties):

    {:dev {:env {:aws-access-key-id "AKIAJQSAKJ4HJEXAMPLE"
                 :aws-secret-key "somesecretawskey"
                 :aws-region "eu-west-1"
                 :consumer-key "somekey"
                 :consumer-secret "somesecretagain"
                 :access-token "12345678-AbcDeFGh"
                 :access-token-secret "somemumbojumbo"}}
     :test {:env {:aws-access-key-id "AKIAJQSAKJ4HJEXAMPLE"
                  :aws-secret-key "somesecretawskey"
                  :aws-region "eu-west-1"
                  :consumer-key "somekey"
                  :consumer-secret "somesecretagain"
                  :access-token "12345678-AbcDeFGh"
                  :access-token-secret "somemumbojumbo"}}}

### Production

Build a stand-alone executable jar like this:

    $ lein uberjar

In production, the settings need to be available through other means than the
`profiles.clj` file. One way is environment variables:

    $ AWS_ACCESS_KEY_ID=AKIAJQSAKJ4HJEXAMPLE ... java -jar twitter-producer-<VERSION>-standalone.jar

Another way is Java system properties:

    $ java -Daws.access.key.id=AKIAJQSAKJ4HJEXAMPLE ... -jar twitter-producer-<VERSION>-standalone.jar

Of course, the variables can also be placed in a file which is sourced before
running:

    $ cat credentials
    export AWS_ACCESS_KEY_ID=AKIAJQSAKJ4HJEXAMPLE
    export AWS_SECRET_KEY=somesecretawskey
    export AWS_REGION=us-east-1
    export CONSUMER_KEY=somekey
    export CONSUMER_SECRET=somesecretagain
    export ACCESS_TOKEN=12345678-AbcDeFGh
    export ACCESS_TOKEN_SECRET=somemumbojumbo

    $ . credentials

    $ java -jar twitter-producer-<VERSION>-standalone.jar
