### How to run?
Run the Controller.java class, without any arguments to run the program.
Note: The project uses a local redis instance for persistence. Here are the docker commands required to run persistent redis locally:

`docker network create -d bridge redisnet`

`docker run -d -p 6379:6379 --name redis --network redisnet redis --save 60 1 --loglevel warning`

### How does the Frontier work?
The Frontier is basically a set of queues that make sure that makes sure of the freshness, relevance and politeness of the crawler. This project uses a Frontier design based on the Mercator architecture. It uses FrontierFrontQueues, that help prioritize the URLs to be crawled.
There are also FrontierBackQueues, each belonging to each domain, which help manage politeness. A politeness heap stores the BackQueue that can be polled the earliest, and then FrontierElement objects(the URLs) are picked 
from the BackQueue. A politeness timestamp is then given to each of these URLs such that for the same domain there is a 1-second wait. The crawler then waits till its the right time to poll a particular URL before proceeding to parse it.
The prioritization in the front queue is based on a relevance score `15*keyWordUrlMatches + 3* inlinks.size() - 5 * waveNumber`, and based on the thresholds defined in the configuration, one of the 5 FrontierFrontQueues is then picked to slot in each FrontierElement.
A method called randomBiased is used to poll elements randomly from the FrontierFrontQueues, such that it uses a quadratic function, making sure the higher priority queues get picked more often by the random method. This makes sure that the higher priority URLs get picked more often.
All this while, redis is involved in saving the Crawled URLs, and also maintaining a queue of newly fetched outlinks. In each iteration of a multithreaded run, 200 URLs are polled from the front of the redis queue and are prioritized and fed into the FrontierFrontQueues. From there they go to the relevant FrontierBackQueues, and then are picked eventually using the Politeness heap.
Then these URLs get send to the Crawler, which in a multi-threaded yet polite way, crawls a URL after making sure its metadata is good and that it can be crawled (using robot.txt parsing).

### Merging Documents
The merging of documents is done on an ESCloud instance. Basically, if a document does not exist we add it to the index. If it does exist, we combine the inlinks, outlinks and append the author names, before updating its value in the index.

### Vertical Search
The UI used for vertical search is based on this tool: [Calaca](https://github.com/romansanchez/Calaca). The modified version of this tool, which can reach the ElasticCloud instance can be accessed by running src/Calaca/_site/index.html.



