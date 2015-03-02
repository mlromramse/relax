




# Relax - The RESTful HTTP Server and Client

Well it isn't actually RESTful unless you use it for that but that was the goal of this project.
You can easily write request handlers that handle different request methods in the way you want.




## Main benefit, easy to use and light weight

This project use plain Java socket to implement the HTTP server, no bloat external dependencies.
So that might make this server less competent and more insecure.
True, but it was made to simply test your other web apps you write and that it is able to do.
At the moment of writing the stand alone, runnable, binary jar is around 100 kBytes.

It also contains a HTTP Client that can HEAD, GET, PUT, POST, DELETE, TRACE, OPTIONS, PATCH, CONNECT to any url.

New from version 1.1.0 is the ability to put load on a service with a simple json load description file.
Read more in the RelaxClient section below.




## The RelaxServer

The RelaxServer is built to be easy to build and run.
It is versatile and can handle all tasks that is thrown upon it.
Built in are a DefaultFileHandler that can serve files from a given folder and its subfolders.
It is the DefaultFileHandler that is used when the RelaxServer is started from its jar.



### The jar is runnable

When built with maven using the assembly plugin the jar is a runnable server in its own right.

##### Build like this:

    mvn clean package shade:shade

You will find the compiled artifact in the `target` directory.

##### Run it like this

    java -jar relax-n.n.n.jar

In this fashion it will serve your files in the current directory on localhost:8080 with 10 threads in the pool. 
There is also possible to change the server port, path to another directory and/or the number of threads in the fixed thread pool by adding one, two or all of the parameters:

* port=1234
* path=/absolute/or/relative/path/to/a/directory
* threads=20

##### Use the parameters like this:

    java -jar relax-n.n.n.jar port=1234 path=/absolute/or/relative/path/to/a/directory threads=20

Now you can browse to http://localhost:1234 in your favorite browser and see the files in the pointed out directory.

By putting you can add files:

	curl -XPUT -T a-file-to-put.gif http://localhost:1234/the-name-of-the-file-when-uploaded.gif

_Any file works, binary as well as text. 
Please note that using ports below 1024 will need root privileges on a unix environment._

##### Activate or deactivate logging

The runnable jar is using the SimpleLogger from log4j and that logs any INFO logs by default.
This can be adjusted by the standard Java VM argument for the SimpleLogger which is 
`-Dorg.slf4j.simpleLogger.defaultLogLevel=WANTED_LEVEL` where the WANTED_LEVEL can be any of these:
error, warn, info, debug or trace.

This will make the server quite quiet:

	java -Dorg.slf4j.simpleLogger.defaultLogLevel=error -jar relax-n.n.n.jar

If you want the info logs but directed into a file you do:

	java -jar relax-n.n.n.jar 2> filename.log



### Import RelaxServer into your project using maven

Build the project with maven like this:

	mvn clean install
	
Include it using dependency statement in your pom.ml like this:

	<dependency>
		<groupId>se.romram</groupId>
		<artifactId>relax</artifactId>
		<version>n.n.n</version>
    </dependency>



### Implement a handler of your own

By including the relax jar in your own project, see above, 
you can build a Hello World HTTP server as easy as this:

    RelaxServer server = new RelaxServer(8080, new RelaxHandler() {
        public boolean handle(RelaxRequest request, RelaxResponse response) {
            response.respond(200, "Hello World!");
            return true;
        }
    });
    server.start();

At this point you can browse http://localhost:8080 to see the response. 
Since no action is taken to distinguish between different paths the text 'Hello World!' will always be returned.



#### Adding functionality to the server

The RelaxServer class has a set of methods that all return the current object instance of the class which means that 
the methods can be placed in a chain:

    RelaxServer server = new RelaxServer(8080, new RelaxHandler() {
        public boolean handle(RelaxRequest request, RelaxResponse response) {
            response.respond(200, "<html><body><h1>Hello World!</h1></body></html>");
            return true;
        }
    });
    server
            .setExecutor(Executors.newFixedThreadPool(30))
            .addHeaders("Server: RelaxServer")
            .setContentType("text/html")
            .start();

_We have added a different thread pool than the default which is fixed at 10 
threads in the pool._

_One header has been added. 
Several headers can be added in the same method. 
Just separate them with a comma._

_The content type is also set to text/html this time instead of the default 
text/plain, since we return HTML tags this time._



### RelaxServer's built in handlers

To be usable as a stand alone web server the RelaxServer has two built in handlers.

##### DefaultFileHandler
The most usable is the DefaultFileHandler that is responsible for returning files from 
your filesystem as requested. 
If nothing is requested or if you select a directory a file listing is returned.
If the user agent tells the server that it can handle HTML that is returned with working links
otherwise the listing is returned as plain text.
_Hidden files are not returned._

Go to this url `http://localhost:8080` with both your favourite web browser and cUrl to see 
the difference. 

##### RelaxServerHandler
The other built in handler manages a few tasks that can come in handy.
First you can ask for `serverstats` which returns some statistics of the server.
It is returned as json and can look like this:

	{
		server: "RelaxServer",
		pid: "16773",
		port: 8080,
		activeThreads: 2,
		requestCount: "33065",
		os: {
			name: "Linux",
			arch: "amd64",
			version: "3.8.0-34-generic"
		},
		sysload: "3.34",
		cors: 4,
		process: {
			mem%: 1,
			peekTime: 505,
			sharedMem: 10000000,
			residentMem: 257000000,
			cpu%: 72
		},
		java: {
			name: "Java HotSpot(TM) 64-Bit Server VM",
			arch: "Oracle Corporation",
			version: "1.7.0_76"
		}
	}

_Most of these data items is collected from the underlying OS. The sysload value 
is an system overall value. 
Process values is only returned on a Linux platform and are in bytes and 
milliseconds where applicable. 
The cpu% item tells the cpu utilization of this process only as do the other 
values within the process element._



### Example of usage

This example is a simple http proxy.

	// Simple HTTP Proxy implementation
	public static void main(String[] args) throws IOException {
		final RelaxServer relaxServer = new RelaxServer(8080, new RelaxHandler() {
			@Override
			public boolean handle(RelaxRequest relaxRequest, RelaxResponse relaxResponse) {
				String url = relaxRequest.getRequestURL();

				RelaxClient relaxClient = new RelaxClient().get(url);

				String accept = relaxRequest.getHeaderMap().get("Accept");
				relaxResponse.setContentType(accept != null && accept.contains("text/css") ? "text/css" : "text/html");

				relaxResponse.respond(relaxClient.getStatus().getCode(), relaxClient.getBytes());

				return true;
			}
		});
		relaxServer.start();
	}





## The RelaxClient

The RelaxClient can be used from inside a Java program or via the load test 
feature.
It is very easy to use and has a lot of features that can be added.



### One liner Google example

The following code fetches the web page from Google.

    String response = new RelaxClient().get("http://google.com").toString();



### Multi line example

However the RelaxClient response contains more features than just the response 
in form of a String:

    RelaxClient client = new RelaxClient().get("http://google.com");
    if (client.getStatus().isOK()) {
        log.debug(client.getStatus().toString());
    }
    


### Additional features

Most of the methods that are available to the RelaxClient returns the instance 
object of the class, it is therefore very easy to chain them together.

##### Here is an example:

    RelaxClient client = new RelaxClient()
            .throwExceptions()
            .useDefaultCookieManager()
            .get("http://google.com");
    if (client.getStatus().isOK()) {
        log.debug(client.getStatus().toString());
    }

_Here we added `throwExceptions()` which means that if something goes wrong a 
Runtime Exception Descendant will be thrown. 
This is for those that want to recover from an error in that way. 
Otherwise it just logs the error._

_We also added `useDefaultCookieManager()` that manages cookies received._



### Load Test 

Since version 1.1.0 of the Relax project you can make simple load testing just 
by writing a small json file.

#### Bare Minimum Json - google-first-load.json

	{
		"tasks": [
			{
				"url": "http://www.google.com"
			}
		]
	}

_This is as small as the load test json can be. 
It will not put any heavy load on the service since no loops are defined._

##### You can use it like this:

	java -jar relax-n.n.n.jar execute=google-first-load.json

The response to this will be two printout rows in the console, among others, 
that looks like this:

	timeStamp,elapsed,label,responseCode,responseMessage,success,grpThreads,allThreads,Latency
    1425281402416,120,null,200,OK,true,1,1,120

_For you out there that are familiar with JMeter will notice the resemblance.
It is intentional since you can use these printouts to get nice reports from 
http://loadosophia.org and others._

##### Disclaimer

Relax is not intended to place heavy load on services and produce trustworthy 
reports.
This feature was added to be able to place background-noise-load on your 
services to easily notice how they behave when used by many.
 

#### Background-noise-load - load.json

This example shows all the features of the relax load json and is an realistic 
example of 100 users using our server at the same time, 
We have defined a base loop of -1 which means loop forever:

	{
		"virtualUsers": 100,
		"rampUp": 10,
		"loop": -1,
		"tasks": [
			{
				"name": "Start call",
				"loop": 1,
				"url": "http://localhost:8080",
				"validate": {
					"contains": ["pom.xml", "!filenameNotToBeFound"],
					"status": [200]
				},
				"delay": 2000
			},
			{
				"name": "Error page",
				"loop": 1,
				"url": "http://localhost:8080/404",
				"validate": {
					"status": [404]
				},
				"delay": 3000
			},
			{
				"name": "Server stats",
				"active": true,
				"loop": 1,
				"url": "http://localhost:8080/serverstats",
				"validate": {
				"contains": ["^\{", "cpu%.:", "peekTime.:.*\d{1,3}"],
					"status": [200]
				},
				"delay": 10000
			}
		]
	}

In this case every loop takes 15 seconds and has three requests. 
The first request has a delay of two seconds, the second request has a delay of
three seconds and the last has a delay of 10 seconds. 

The full loop takes around 15.5 seconds since the Server stats request itself 
takes 0.5 seconds. This corresponds to a per user tps (Transaction Per Second) 
of 3/15.5, approx 0.194.

Since we are using 100 virtual users we have a constant load of around 19.4 tps.
This is a cpu% load on the service of 15 % on a 4 core system.
Quite a nice background noise.

##### The load.json file explained

In the file above all features has been used but not necessarily to its extent.
Here is an explanation on all available parameters:

* virtualUsers: numeric value. Best practice is to not extend 200.
* rampUp: time in seconds until all users are up.
* loop: -1=forever or as many as you want.
* tasks: an array of task objects. No limit.
* task.name: The name of the task in the log.
* task.active: Flag to inactive task, e.g. during test phase.
* task.loop: task independent loop.
* task.url: the url to fetch.
* task.validate.contains: an array of reg.exp strings that should match and in 
  case the reg.exp starts with ! reg.exp that should NOT match.
* task.validate.status: an array of acceptable statuses. Any one should match.
* task.delay: delay in milliseconds to pause after the request.

##### Generate a file to upload to loadosophia.org

As mentioned above it is possible to take the output from relax and upload that 
to the JMeter report generator site loadosophia.org.

Since the error log and other output gets mixed you need to make use of file
descriptor redirect feature that you get from a unix type system.

	java -Dorg.slf4j.simpleLogger.defaultLogLevel=error \
	-jar target/relax-n.n.n.jar execute=src/test/resources/load.json \
	2> errors.log 1> result.csv

In the above call we set the error level to error although it is not strictly 
necessary. But it keeps the errors.log file small.

We execute the load.json file that is included in the project.

**The last row does the redirect:** 
* 2> errors.log - sends the error log to that file
* 1> result.csv - sends std out to the file result.csv

When done the result.csv file can be uploaded to loadosophia.org for processing.