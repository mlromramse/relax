




# Relax - The RESTful HTTP Server and Client

Well it isn't actually RESTful unless you use it for that but that was the goal of this project.
You can easily write request handlers that handle different request methods in the way you want.




## Main benefit, easy to use and light weight

This project use plain Java socket to implement the HTTP server, no bloat external dependencies.
So that might make this server less competent and more insecure.
True, but it was made to simply test your other web apps you write and that it is able to do.
At the moment of writing the stand alone, runnable, binary jar is below 100 kBytes.

It also contains a HTTP Client that can HEAD, GET, PUT, POST, DELETE, TRACE, OPTIONS, PATCH, CONNECT to any url.




## The RelaxServer

The RelaxServer is built to be easy to build and run.
It is versatile and can handle all tasks that is thrown upon it.
Built in are a DefaultFileHandler that can serve files from a given folder and its subfolders.
It is the DefaultFileHandler that is used when the RelaxServer is started from its jar.



### The jar is runnable

When built with maven using the assembly plugin the jar is a runnable server in its own right.

##### Build like this:

    mvn clean compile assembly:single

##### Run it like this

    java -jar relax-n-n-jar-with-dependencies.jar

In this fashion it will serve your files in the current directory on localhost:8080. 
There is also possible to change the port and/or path to another directory by adding one or both of the parameters:

* port=1234
* path=/absolute/or/relative/path/to/a/directory

##### Use the parameters like this:

    java -jar relax-n-n-jar-with-dependencies.jar port=1234 path=/absolute/or/relative/path/to/a/directory

Now you can browse to http://localhost:1234 in your favorite browser and see the files in the pointed out directory.

By putting you can add files:

	curl -XPUT -T a-file-to-put.gif http://localhost:1234/the-name-of-the-file-when-uploaded.gif

_Any file works, binary as well as text. 
Please note that using ports below 1024 will need root privileges on a unix environment._




### Implement a handler of your own

By including the relax jar in your own project you can build a Hello World HTTP server as easy as this:

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
            response.respond(200, "<h1>Hello World!</h1>");
            return true;
        }
    });
    server
            .setExecutor(Executors.newFixedThreadPool(30))
            .addHeaders("Server: RelaxServer")
            .setContentType("text/html")
            .start();

_We have added a different thread pool than the default which is fixed at 10 threads in the pool._

_One header has been added. Several headers can be added in the same method._

_The content type is also set to text/html this time instead of the default text/plain._




## The RelaxClient

The RelaxClient can only be used from inside a Java program.
It is very easy to use and has a lot of features that can be added.



### One liner Google example

The following code fetches the web page from Google.

    String response = new RelaxClient().get("http://google.com").toString();



### Multi line example

However the RelaxClient response contains more features than just the response in form of a String:

    RelaxClient client = new RelaxClient().get("http://google.com");
    if (client.getStatus().isOK()) {
        log.debug(client.getStatus().toString());
    }
    


### Additional features

Most of the methods that are available to the RelaxClient returns the instance object of the class,
it is therefore very easy to chain them together.

##### Here is an example:

    RelaxClient client = new RelaxClient()
            .throwExceptions()
            .useDefaultCookieManager()
            .get("http://google.com");
    if (client.getStatus().isOK()) {
        log.debug(client.getStatus().toString());
    }

_Here we added `throwExceptions()` which means that if something goes wrong a Runtime Exception Descendant
will be thrown. 
This is for those that want to recover from an error in that way. 
Otherwise it just logs the error._

_We also added `useDefaultCookieManager()` that manages cookies received._

