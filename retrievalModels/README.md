### How to run?
The Controller.java file contains the main method that needs to be run to get an output. This produces text files that contain the documents, queries and their scores.

### Implementation Summary

#### Document Parsing, Indexing and Query Parsing
Parsing just involved going line by line to each document and reading the specific tags and making sure all doc tags of a single document get concatenated. To make indexing efficient, the lucene library is used to implement 
porter stemmer before indexing. The stop words also get removed in this process. The Preprocessor class does this for both the documents and then later for each of the queries.
#### Retrieval Models
There is an interface used called RetrieveModel, that serves as a contract for all the models.
Each of them scores the documents in their own specific way and then writes to a file that is named as {modelName}.txt.
There are some peculiarities here are there but for the most part all of the models used data that is stored into a list of Document objects, which stores all the data that is fetched from elastic search in the very beginning, including document frequencies, term frequencies and idf.
Elastic search setting are set such that term vectors are stored. TfIdf model uses data stored by the Okapi model as expected. The Unigram models use a high negative value to penalize the absence of a term since just adding 0 would skew the score to the wrong side because of the presence of low log values.

### Retrieval Model Results
| Relevance Model      | Average Precision | Precision at 10 docs | Precision at 30 docs |
| ----------- | ----------- | ----------- | -----------
|  ESDefault      |    0.3095    |  0.4680 |  0.3733
| OkapiTf   |  0.1704        | 0.3000 | 0.2453
| TfIdf     | 0.2311   |    0.3360 |  0.3000
| OkapiBM25 |  0.2954 | 0.4440 |  0.3653
| Unigram LM Laplace |    0.2368 | 0.4080 |  0.3013
| Unigram LM J-M |  0.2218 |  0.3720 | 0.2853

### Pseudo-Relevance Feedback
#### Custom Query Scoring
As per the usual relevance feedback mechanism this is what is done:
1) Used a relevance model (OkapiBM25) to fetch documents and then get top 30 of these for query expansion.
2) Now all the terms from these documents are extracted and then scored using this formula = n * idf 
(n is the number of docs amongst the top k, in which the term occurs. idf is inverse document frequency for that term, across the collection) 
3) These terms are then sorted and then top 3 of these are added to the query.
4) Relevance model is run again and an increase is seen in average precision.

#### Custom Query Scoring Result
| Relevance Model      | Average Precision Pre | Average Precision Post
| ----------- | ----------- | ----------- |
| OkapiBM25 | 0.2149 |  0.2641

#### Custom Query Scoring
As per the usual relevance feedback mechanism this is what is done:
1) Used a relevance model (OkapiTf) to fetch documents and then get top 30 of these for query expansion.
2) Then significant terms are fetched via elastic search, then the terms in the query are excluded from this set.
3) These terms are then sorted in terms of their Idf  and then top 3 of these are added to the query.
5) Relevance model is run again and an increase is seen in average precision.

#### Custom Query Scoring Result
| Relevance Model      | Average Precision Pre | Average Precision Post
| ----------- | ----------- | ----------- |
| OkapiTf | 0.2149 |  0.3320