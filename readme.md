




# Relax - The RESTful HTTP Server and Client

Well it isn't actually RESTful unless you use it for that but that was the goal of this project.




## Main benefit, easy to use and light weight

This project use plain Java socket to implement the HTTP server, no bloat external dependencies.
So that might make this server less competent and more insecure.
True, but it was made to simply test your other web apps you write and that it is able to do.




## The jar is runnable

When built with maven using the assembly plugin the jar is a runnable server in its own right.

##### Build like this:

    mvn clean compile assembly:single

##### Run it like this

    java -jar relax-n-n-jar-with-dependencies.jar

In this fashion it will serve your files in the current directory on localhost:8080. 
There is also possible to change the port and/or path to another directory by adding one or both of the parameters:

* port=1234
* path=/absolute/or/relative/path/to/a/directory

    java -jar relax-n-n-jar-with-dependencies.jar port=1234 path=/absolute/or/relative/path/to/a/directory

Now you can browse to http://localhost:1234 in your favorite browser and see the files in the pointed out directory.

By putting you can add files:

	curl -XPUT -T a-file-to-put.gif http://localhost:1234/the-name-of-the-file-when-uploaded.gif

_Any file works, binary as well as text._

