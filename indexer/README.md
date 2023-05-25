### How to run and parallelism overview?
The Controller.java file contains the main method that needs to be run to get an output. This produces text files that contain the documents, queries and their scores.
There are two arguments that the program requires:
1) The 0th argument accepts true or false and decides if the index is stemmed or non stemmed.
2) The 1st argument decides the number of threads that are used. This can be increased for better multiprocessing. A value of 1 is uses for the unstemmed index and a value as high as 20 is used for the stemmed one.

The code is optimised to run in batches such that we can change the size of each batch and the number of threads.
### Implementation Summary
The indexer interface serves as a contract for all the functionality of the custom indexer. This involves all the functionality including creating, merging, splitting, compressing, decompressing and finally also searching a word in the index.
Gzip library is used for compression. The single final file is created by merging all the files from the previous batch, and goes into a recursing merged directory. Then 10 files are created by splitting this final file.

While searching, a map takes care of finding the exact catalog file in which a term is present. This value is then read, and then we seek the exact offset in the corresponding index file. Hence this implies a logarithmic scan, since the term id's are sorted.
#### Document Parsing, Indexing and Query Parsing
Parsing remains the same as HW1, the only difference is that this time a custom regex is used in order to meet all the requirements. Other than that the document parsing is also done in batches so that memory does not run out.

#### Retrieval Models
OkapiTf, OkpaBM25 and UnigramLaplace have been implemented. The crux of the implementation remains the same. The method to fetch the required information has changed.

### Retrieval Model Results
| Relevance Model      | Average Precision with ES | Average Precision Stemmed |  Average Precision non-stemmed
| ----------- | ----------- | ----------- | ----------- | 
| OkapiTf   |  0.1704        | 0.2669 |   0.2502
| OkapiBM25 |  0.2954 | 0.2074 |   0.2130
| Unigram LM Laplace |    0.2368 | 0.2669 |  0.2502 
