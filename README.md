# Googol - Distributed Web Search Engine

## Overview
This project was made by college students for the course Distributed Systems  
Googol is a distributed web search engine that implements:
- Web crawling and indexing
- Distributed processing with RMI
- REST API integrations
- Real-time statistics with WebSockets

## How to Run
-For every change made in the backend of the code that is in the "src/main/java/motorbusca" folder you need to compile the code being in the "src/main/java" folder and use the "javac -encoding UTF-8 -cp ".;C:\the\dir\you\have\src\lib\jsoup-1.19.1.jar" motorbusca/*.java" comand in the terminal.  
After compiling all the code you can run the code using this commands in the order im giving you in the same folder as before

java motorbusca.IndexServer  
java motorbusca.StorageBarrelImpl 0  
java motorbusca.StorageBarrelImpl 1 localhost 8184  
java motorbusca.StorageBarrelImpl 2 localhost 8184  
java motorbusca.Downloader  
java ".;C:\the\dir\you\have\src\lib\jsoup-1.19.1.jar" motorbusca.Robot  
mvn spring-boot:run

You have to create the barrel 1 and 2 using the localhost and the port 8184 since we use them to have a partion of the data and do not store everything in one and in case of a failure of 1 of the 2 barrel all the data is saved.


## Features

- **Web Crawling**: Automatic URL discovery and content indexing
- **Distributed Architecture**: 
  - IndexServer (coordinator)
  - StorageBarrels (data partitions)
  - Downloaders (crawlers)
- **Search Capabilities**:
  - Term-based search with relevance ranking
  - Multi-word queries
  - Contextual analysis (OpenRouter AI)
- **Hacker News Integration**: Index top stories matching search terms

## Future work
- Improvemnet in the backend system architecture , since the one now is not correct. It shouldb be IndexServer --> WebCrawler --> Donwloader --> Storage Barrel 1/2.
- Make the ranking of the pages based on the amount of links that are retrived from the that URL.
- Ensure the barrel can get all the data that were once stored in the them in case of a error.
- Ensure the Downloader can be ran on mutiple threads and/or multiple computers at the same time.
- Making a way that stopwords don't enter the barrel, not by making a list of them but by analysis of the text retrieved from the URL
