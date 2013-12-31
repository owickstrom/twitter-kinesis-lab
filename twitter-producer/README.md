# twitter-producer

Reads the Twitter streaming API, extracts the hashtags and the created_date,
and posts that data to a Kinesis stream, using the hashtag as the partition
key. This enables a Kinesis application to perform things like counting the
most popular hashtags during a certain time window.

## Installation

Make sure [Leiningen 2+](http://leiningen.org/) is installed.

Build a stand-alone executable jar like this:

    $ lein uberjar

Also, a few properties need to be set, containing credentials for the Twitter
Streaming API and Amazon Kinesis. One way of doing this is to create a file
called `credentials`, which is "sourced" before the application is started:

	export AWS_ACCESS_KEY_ID=AKIAJQSAKJ4HJEXAMPLE
	export AWS_SECRET_KEY=somesecretawskey
	export AWS_REGION=us-east-1
	export CONSUMER_KEY=somekey
	export CONSUMER_SECRET=somesecretagain
	export ACCESS_TOKEN=12345678-AbcDeFGh
	export ACCESS_TOKEN_SECRET=somemumbojumbo

## Usage

First, source the credentials file:

	. credentials

The utility can be run either from Leiningen or as a stand-alone jar:

    $ lein run -- [args]

    $ java -jar twitter-producer-<VERSION>-standalone.jar [args]

