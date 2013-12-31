# tagcount

Provides a worker that receives tweet tag records from Amazon Kinesis. The number 
of occurrences of each tag in a sliding window is counted and stored in a database.

## Installation

Make sure [Leiningen 2+](http://leiningen.org/) is installed.

Build a stand-alone executable jar like this:

    $ lein uberjar

Also, a few properties need to be set, containing credentials for Amazon Kinesis
and the PostgreSQL RDS database. One way of doing this is to create a file
called `credentials`, which is "sourced" before the application is started:

	export AWS_ACCESS_KEY_ID=AKIAJQSAKJ4HJEXAMPLE
	export AWS_SECRET_KEY=somesecretawskey
	export AWS_REGION=us-east-1
	export DB_CLASSNAME=org.postgresql.Driver
	export DB_SUBPROTOCOL=postgresql
	export DB_SUBNAME=//twitter.whatever.us-east-1.rds.amazonaws.com:5432/trendingtweets
	export DB_USER=awsuser
	export DB_PASSWORD=somepwd

## Usage

First, source the credentials file:

	. credentials

The utility can be run either from Leiningen or as a stand-alone jar:

    $ lein run -- [args]

    $ java -jar tagcount-<VERSION>-standalone.jar [args]

