### How fast is PokerFace?
Everyone asks "How fast is PokerFace?", and frankly, "We don't know", just doesn't seem to satisfy them.  
So, after running some informal tests we can say "**It's really fast !**".  
Please keep in mind that the purpose of these benchmarks is not to make definitive claims.  The tests were performed using limited resources, just give us a feel as to what planet PokerFace is on.  Some results from the tests are [not yet full understood](#Questions), so please take all this with a grain of salt.

###Procedure
Testing was done on a Macbook with [ab](http://httpd.apache.org/docs/current/programs/ab.html) (Apache Bench) and [siege](http://www.joedog.org/siege-home/) running locally against local server instances of `PokerFace` and [nginx](http://nginx.org) each listening on the loopback interface to eliminate network traffic issues.  Two terminal windows were opened, one to run the server (either PokerFace or nginx) and one to run the load test tool (either ab or siege).

Each server was configured to serve static files from a local directory.  The files and configurations used for each server can be found in the [Samples/Benchmark/](http://) directory of this project.

After starting the server, it was hit with a brief load to warm it up.  This was a clear advantage to PokerFace, as it gave the Java Virtual Machine time to load resources and optimize the bytecode.  Tests were then run a number of times to see that results were consistent across runs.

### ab v2.3 
(ab -k -c 50 -t 0 -L Samples/Benchmark/urls.txt)

Please note: There is a publiclly available [multi-url patch for ab](http://code.google.com/p/apachebench-for-multi-url/) which allows you to to test multiple url's similar to the way `siege` works.

######nginx v1.7.9
```
Server Hostname:        127.0.0.1
Server Port:            8080
Concurrency Level:      50
Time taken for tests:   3.352 seconds
Complete requests:      50000
Total transferred:      6867886189 bytes
HTML transferred:       6854170533 bytes
Requests per second:    14914.80 [#/sec] (mean)
Time per request:       3.352 [ms] (mean)
Time per request:       0.067 [ms] (mean, across all concurrent requests)
Transfer rate:          2000647.33 [Kbytes/sec] received
Times (ms)    min  mean[+/-sd] median   max
Connect:        0    0   0.2      0       7
Processing:     0    3   1.8      3      17
Waiting:        0    3   1.5      2      15
Total:          0    3   1.9      3      18
```
######PokerFace
```
Server Hostname:        127.0.0.1
Server Port:            8080
Concurrency Level:      50
Time taken for tests:   3.181 seconds
Complete requests:      50000
Total transferred:      6750750044 bytes
HTML transferred:       6742654561 bytes
Requests per second:    15716.73 [#/sec] (mean)
Time per request:       3.181 [ms] (mean)
Time per request:       0.064 [ms] (mean, across all concurrent requests)
Transfer rate:          2072259.64 [Kbytes/sec] received
Times (ms)    min  mean[+/-sd] median   max
Connect:        0    0   0.0      0       2
Processing:     0    3   2.4      3      14
Waiting:        0    1   0.9      1      12
Total:          0    3   2.4      3      14
```

### siege v2.78 
(siege -c 100 -i -b -f Samples/Benchmark/urls.txt)
######nginx v1.7.9
```
Transactions:		        5870 hits
Elapsed time:		       10.15 secs
Data transferred:	     1038.33 MB
Response time:		        0.17 secs
Transaction rate:	      578.33 trans/sec
Throughput:		      102.30 MB/sec
Longest transaction:	        0.38
Shortest transaction:	        0.03
```
######PokerFace
```
Transactions:		       14271 hits
Elapsed time:		       10.31 secs
Data transferred:	     2541.44 MB
Response time:		        0.07 secs
Transaction rate:	     1384.19 trans/sec
Throughput:		      246.50 MB/sec
Longest transaction:	        0.22
Shortest transaction:	        0.00
```

###Questions<a name="Questions"></a>
* Why are the PokerFace performance numbers roughly the same as nginx's when tested using ab, but 2.5 times better when tested using siege?