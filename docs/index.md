# simple-messaging-service

A messaging service backed by RabbitMQ which supports delayed sends.

Current status is roughed in MVP.

General concept mappings are topics = exchanges, subscriptions = queues.

Currently, all reads are acking - to be improved. Messages can be read 
one-at-a-time over REST, or streamed via websocket connection.
