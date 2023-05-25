[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-24ddc0f5d75046c5622901739e7c5dd533143b0c8e959d652212380cedb1ea36.svg)](https://classroom.github.com/a/Vkjk5Qfy)
# HW7
### Project Overview
This project deals with classifying HAM or SPAM using Machine Learning models that are trained on mails from this trec corpus http://plg.uwaterloo.ca/~gvcormac/treccorpus07/.

### Data Preparation
The steps involved are:
1) Parsing the MIME format emails.
2) Preprocessing them to only pick selective words.
3) Adding this data to Elastic-Search for ease of fetching feature data later.
4) For last part of the assignment only words that lie withing a certain range of df/total_docs are picked, since other words might be irrelevant.

### Parts:
#### Part 1A
This part involves using custom spam words as features for the machine learning models that we are using to classify email data to SPAM or HAM. These custom ngrams are:
1) free
2) win
3) porn
4) lottery
5) erectile dysfunction
6) click here

#### Part 1B:
This parts involves using another predetermined list of [spam words](https://course.ccs.neu.edu/cs6200f20/assets/uploads/spam_words.html).

In Part1A and Part1B the following models are trained:
1) Naive Bayes
2) Decision Tree
3) M5P Regression

#### Part 2:
Part 2 involves using all the terms in the corpus as features. Some of these have been filtered using the method mentioned in the data preparation step. This ends up getting us about 10000 features. 
We use the sparse arff format for representing this data and then finally a LibLinear classifier is run on this data. 


#### Results
All the relevant results are in the Results folder in the root of the repository.