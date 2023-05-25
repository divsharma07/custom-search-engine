[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-24ddc0f5d75046c5622901739e7c5dd533143b0c8e959d652212380cedb1ea36.svg)](https://classroom.github.com/a/yjblvSPs)
[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-8d59dc4de5201274e310e4c54b9627a8934c3b88527886e3b421487c677d23eb.svg)](https://classroom.github.com/a/yjblvSPs)
# HW5


### Overview
This project has two parts: 
1) A UI for gathering manual relevance feedback. 
2) Relevance Metrics calculation that calculated the following:  R-precision, Average Precision, nDCG, precision@k and recall@k and F1@k (k=5,10, 20, 50, 100)

### How to run?
Run the Trec.java class, the arguments required are:
[qrelFile] [trecFile] -q

[qrelFile] - contains the manual relevance feedback.
[trecFile] - contains retrieved docs with scores.
[-q] - controls if you want to print stuff for all queries or just averaged.

Basically, to run it for the vertical search data, these would be the params:
individual_qrel.txt verticleSearchResults.txt -q
### UI Overview:
The manual feedback UI is just a flask app with a static HTML attached to it. It basically pulls 200 documents for each query from ESCloud and makes user grade each of these.

### Metrics Calculations Overview:
The values from the trecFile get sorted by score and then each query is evaluated. For each query, each of its documents, in descending order of score is then matched to its relevence score and all the metrics are calculated. These are then 
summed and averaged across queries.
It is made sure the code is modeled quite closely to the trec_eval.pl example and the output of the programs for similar inputs are the same.

