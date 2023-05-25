import React from "react";

import ElasticSearchAPIConnector from "@elastic/search-ui-elasticsearch-connector";

import {
    ErrorBoundary,
    SearchProvider,
    SearchBox,
    Results,
    PagingInfo,
    ResultsPerPage,
    Paging,
    WithSearch,
} from "@elastic/react-search-ui";
import {
    Layout,
} from "@elastic/react-search-ui-views";
import "@elastic/react-search-ui-views/lib/styles/styles.css";

const connector = new ElasticSearchAPIConnector({
                                                    cloud: {
                                                        id: "worldwar2_cluster:dXMtZWFzdDQuZ2NwLmVsYXN0aWMtY2xvdWQuY29tOjQ0MyQ5MWJiYzk5ZWJmNGU0MWFlYjVhZmU0NWI0MTVmN2VhYiQ2MDBhYWQ4NDkxOTc0M2YyODE3ZThjMTAzOWM1NGYyNQ=="
                                                    },
                                                    index: "worldwar2_crawled_data_index",
                                                    apiKey: "",
                                                });

const config = {
    debug: true,
    alwaysSearchOnInitialLoad: true,
    apiConnector: connector,
    hasA11yNotifications: true,
    searchQuery: {
        filters: [],
        search_fields: {
            title: {
                weight: 3
            },
        },
        result_fields: {
            title: {
                snippet: {
                    size: 100,
                    fallback: true
                }
            },
            id: { raw: {} },
            author: {raw: {}}
        }
    },
    autocompleteQuery: {
        results: {
            search_fields: {
                parks_search_as_you_type: {}
            },
            resultsPerPage: 5,
            result_fields: {
                title: {
                    snippet: {
                        size: 100,
                        fallback: true
                    }
                },
                id: { raw: {} },
                author: {raw: {}}
            }
        },
        suggestions: {
            types: {
                documents: {
                    fields: ["parks_completion"]
                }
            },
            size: 4
        }
    }
};


export default function App() {
    return (
        <SearchProvider config={config}>
            <WithSearch
                mapContextToProps={({ wasSearched }) => ({
                    wasSearched
                })}
            >
                {({ wasSearched }) => {
                    return (
                        <div className="App">
                            <ErrorBoundary>
                                <Layout
                                    header={
                                        <SearchBox
                                            autocompleteMinimumCharacters={3}
                                            autocompleteResults={{
                                                linkTarget: "_blank",
                                                sectionTitle: "Results",
                                                titleField: "title",
                                                urlField: "id",
                                                shouldTrackClickThrough: true,
                                                clickThroughTags: ["test"]
                                            }}
                                            autocompleteSuggestions={true}
                                            debounceLength={0}
                                        />
                                    }

                                    bodyContent={
                                        <Results
                                            titleField="title"
                                            urlField="id"
                                            shouldTrackClickThrough={true}
                                        />
                                    }
                                    bodyHeader={
                                        <React.Fragment>
                                            {wasSearched && <PagingInfo />}
                                            {wasSearched && <ResultsPerPage />}
                                        </React.Fragment>
                                    }
                                    bodyFooter={<Paging />}
                                />
                            </ErrorBoundary>
                        </div>
                    );
                }}
            </WithSearch>
        </SearchProvider>
    );
}
