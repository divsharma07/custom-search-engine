[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-8d59dc4de5201274e310e4c54b9627a8934c3b88527886e3b421487c677d23eb.svg)](https://classroom.github.com/a/8Sq9xjZA)
# HW4

### Overview
This project has two parts:
1) PageRank Implementation for Crawled URLs: This involves iterative computation of page rank done for the URLs that were crawled as a part of the previous assignment. Result file: mergedDocPageRank.txt
2) PageRank Implementation for wt2g Links: This involves iterative computation of page rank for the given set of pages, in the wt2g_inlinks.txt file. Result file: wt2gPageRank.txt
3) HITS Algorithm Implementation for wt2g: This is an implementation of the Authority and Hub scores for the links in wt2g_inlinks.txt file. Result files: authorityScores.txt, hubScores.txt.
### How to run?
Run the Controller.java class, without any arguments to run the program.
Note: The project uses a local redis instance for persistence. Here are the docker commands required to run persistent redis locally:

`docker network create -d bridge redisnet`

`docker run -d -p 6379:6379 --name redis --network redisnet redis --save 60 1 --loglevel warning`

### PageRank Overview:
The iterative page rank computation uses the following as the basis for implementation:

```
// P is the set of all pages; |P| = N
// S is the set of sink nodes, i.e., pages that have no out links
// M(p) is the set of pages that link to page p
// L(q) is the number of out-links from page q
// d is the PageRank damping/teleportation factor; use d = 0.85 as is typical

foreach page p in P
PR(p) = 1/N                          /* initial value */

while PageRank has not converged do
sinkPR = 0
foreach page p in S                  /* calculate total sink PR */
sinkPR += PR(p)
foreach page p in P
newPR(p) = (1-d)/N                 /* teleportation */
newPR(p) += d*sinkPR/N             /* spread remaining sink PR evenly */
foreach page q in M(p)             /* pages pointing to p */
newPR(p) += d*PR(q)/L(q)         /* add share of PageRank from in-links */
foreach page p
PR(p) = newPR(p)

return PR
```
Apart from this, the perplexity measure is taken as the metric to determine if the pagerank has converged. If perplexity measure remains the same up til its unit value, 4 successive times, PageRank is assumed to be stable and the algorithm ends.

### HITS overview:
HITS algorithm, uses a query and computes relavent Hubs and Authorities on the basis of the query. Unlike PageRank, this algorithm is query dependent.
This query is used to create the root set. From this root set, a base set is created, picking outLinks and inLinks from the root set.
The following algorithm is used:

A. Create a root set: Obtain the root set of about 1000 documents by ranking all pages using an IR function (e.g. BM25, ES Search). You will need to use your topic as your query

B. Repeat few two or three time this expansion to get a base set of about 10,000 pages:
For each page in the set, add all pages that the page points to
For each page in the set, obtain a set of pages that pointing to the page
if the size of the set is less than or equal to d, add all pages in the set to the root set
if the size of the set is greater than d, add an RANDOM (must be random) set of d pages from the set to the root set
Note: The constant d can be 200. The idea of it is trying to include more possibly strong hubs into the root set while constraining the size of the root size.
C. Compute HITS. For each web page, initialize its authority and hub scores to 1. Update hub and authority scores for each page in the base set until convergence

Authority Score Update: Set each web page's authority score in the root set to the sum of the hub score of each web page that points to it
Hub Score Update: Set each web pages's hub score in the base set to the sum of the authority score of each web page that it is pointing to
After every iteration, it is necessary to normalize the hub and authority scores. Please see the lecture note for detail.
Create one file for top 500 hub webpages, and one file for top 500 authority webpages. The format for both files should be:
[webpageurl][tab][hub/authority score]\n





