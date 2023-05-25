# importing Flask and other modules
import os.path
from elasticsearch7 import Elasticsearch
from flask import Flask, request, render_template


def get_assessment_urls():
    urls = []
    urls_and_queryids_map = {}

    es = Elasticsearch("https://world-war-2.es.us-central1.gcp.elastic-cloud.com/",
                       http_auth=("elastic", "u0inB35B12cX5YtPrOP5EzjE"), timeout=30, max_retries=10,
                       retry_on_timeout=True)

    index_name = "worldwar2_crawled_data_index"
    body = {
        "query": {
            "match": {"content": "battle of stalingrad"}
        }
    }
    urls, urls_and_queryids_map = get_assessment_urls_for_topic(body, es, index_name, urls, urls_and_queryids_map, 1)

    body = {
        "query": {
            "match": {"content": "nazi germany rise"}
        }
    }
    urls, urls_and_queryids_map = get_assessment_urls_for_topic(body, es, index_name, urls, urls_and_queryids_map, 2)

    body = {
        "query": {
            "match": {"content": "United States battles won in WW2"}
        }
    }
    urls, urls_and_queryids_map = get_assessment_urls_for_topic(body, es, index_name, urls, urls_and_queryids_map, 3)

    body = {
        "query": {
            "match": {"content": "what caused world war 2"}
        }
    }
    urls, urls_and_queryids_map = get_assessment_urls_for_topic(body, es, index_name, urls, urls_and_queryids_map, 4)
    return urls, urls_and_queryids_map

def get_assessment_urls_for_topic(body, es, index_name, urls, urls_and_queryids_map, query_id):
    res = es.search(index=index_name, body=body, size=200)
    docs = res["hits"]["hits"]
    for doc in docs:
        urls.append(doc["_id"])
        urls_and_queryids_map[doc["_id"]] = query_id
    return urls, urls_and_queryids_map


# Flask constructor
app = Flask(__name__)



qrel_file = "individual_qrel.txt"
assessor_id = "Divyanshu_Sharma"
# Fetch a list of all 800 docs and save them in urls list. Below is a placeholder "urls" list
# Create a map of urls and its queryids(4 topics we have.1,2,3,4). Use this map to fetch query_id in API function.
urls, urls_and_queryids_map = get_assessment_urls()
query_id_and_topics = {1:"battle of stalingrad", 2: "nazi germany rise", 3: "United States battles won in WW2",
                       4:"what caused world war 2"}
print(len(urls))
print(len(urls_and_queryids_map))

# A decorator used to tell the application
# which URL is associated function
@app.route('/', methods=["GET", "POST"])
def grade_document():
    if request.method == "POST":
        current_url = request.form.get("current_url")
        relevancy = request.form.get("relevancy")
        if os.path.isfile(qrel_file):
            f = open(qrel_file, "a")
        else:
            f = open(qrel_file, "w")
        f.write("{0} {1} {2} {3}\n".format(urls_and_queryids_map[current_url], assessor_id, current_url, relevancy))
        urls.remove(current_url)
        f.close()
        return render_template("form.html", urls=urls, success=True, graded_doc=current_url, length_of_urls = len(urls),
                               topic=query_id_and_topics[urls_and_queryids_map[current_url]])
    return render_template("form.html", urls=urls, length_of_urls = len(urls))


if __name__ == '__main__':
    app.run()
