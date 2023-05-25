[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-24ddc0f5d75046c5622901739e7c5dd533143b0c8e959d652212380cedb1ea36.svg)](https://classroom.github.com/a/TxfK4bqz)
# HW6

### Overview
This project deals with the implementation of a machine learning algorithm to score the relevance of a query to a document. The features provided to train the model are the relevance scores from 6 different models namely, ES Search, OkapiTf, TfIdf, OkapiBM25, Unigram Laplace, Unigram Jelinek Mercer.
The project chooses to use Linear Regression because of the ease of choosing hyperparameters. Some better modells like Decision Trees, SVM and GradientBoosted Trees could also be used for better ranking. 

### Data Source
The data source for the algorithm is a list of ranked outputs of relevance models that have been run as a part of HW1. 
The data collection involves finding all the document and query pairs for which there is relevance judgement available and then some more. The scores from different models, for these document query pairs are concatenated. 
Finally, while training the while process is run 5 times, creating a 1:5 test train split and making sure all queries are allowed to be a part of the test set.


### Result
The final result is basically calculated by running trec_eval for the 5 runs and averaging the average precisions. 
The average precision across the 5 runs are:
#### Test Average Precision
Average Precision:: 0.4641

#### Training Average Precision
Average Precision: 0.4795